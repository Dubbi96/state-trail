package com.dubbi.statetrail.crawl.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.dubbi.statetrail.common.util.Hashing;
import com.dubbi.statetrail.common.util.UrlPattern;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.dubbi.statetrail.crawl.web.UiSignatureExtractor;
import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dubbi.statetrail.crawl.domain.CrawlLinkEntity;
import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageEntity;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.crawl.web.ActionType;
import com.dubbi.statetrail.crawl.web.AllowlistRules;
import com.dubbi.statetrail.crawl.web.CrawlBudget;
import com.dubbi.statetrail.crawl.web.CrawlStrategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebCrawlerService {
    private final CrawlRunRepository crawlRunRepository;
    private final CrawlPageRepository crawlPageRepository;
    private final CrawlLinkRepository crawlLinkRepository;
    private final CrawlRunEventHub eventHub;
    private final ObjectStorageService objectStorageService;
    private final AuthProfileRepository authProfileRepository;
    private final ObjectMapper objectMapper;

    public WebCrawlerService(
            CrawlRunRepository crawlRunRepository,
            CrawlPageRepository crawlPageRepository,
            CrawlLinkRepository crawlLinkRepository,
            CrawlRunEventHub eventHub,
            ObjectStorageService objectStorageService,
            AuthProfileRepository authProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.crawlRunRepository = crawlRunRepository;
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
        this.eventHub = eventHub;
        this.objectStorageService = objectStorageService;
        this.authProfileRepository = authProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void start(UUID runId) {
        var runOpt = crawlRunRepository.findByIdWithRelations(runId);
        if (runOpt.isEmpty()) return;

        var run = runOpt.get();
        run.markRunning();
        crawlRunRepository.save(run);
        eventHub.publish(runId, "STATUS", Map.of("status", "RUNNING", "startedAt", Instant.now().toString()));

        int edges = 0;
        int errors = 0;

        try {
            CrawlBudget budget = CrawlBudget.from(run.getBudget());
            CrawlStrategy strategy = CrawlStrategy.fromNullable(run.getStrategy());
            boolean browserMode = strategy.isBrowser();
            CrawlStrategy ordering = strategy.base();

            var allowlist = AllowlistRules.from(run.getProject().getAllowlistRules());
            System.out.printf("[Crawl] Allowlist config: domains=%s, pathPrefixes=%s, deny=%s%n", 
                allowlist.domains(), allowlist.pathPrefixes(), allowlist.deny());
            Instant deadline = Instant.now().plus(budget.maxDuration());

            // in-memory state
            Set<String> visited = new HashSet<>();
            Set<String> enqueued = new HashSet<>();
            Set<String> edgeSeen = new HashSet<>();
            Map<String, Integer> depthByUrl = new HashMap<>();
            Map<String, CrawlPageEntity> pageByUrl = new HashMap<>();

            // frontier
            ArrayDeque<String> bfs = new ArrayDeque<>();
            Map<String, Integer> mcsScore = new HashMap<>();
            long[] seq = new long[]{0L};
            PriorityQueue<FrontierItem> mcs = new PriorityQueue<>(
                    (a, b) -> {
                        int c = Integer.compare(b.score(), a.score());
                        if (c != 0) return c;
                        return Long.compare(a.seq(), b.seq());
                    }
            );

            URI startUri;
            try {
                startUri = URI.create(run.getStartUrl());
            } catch (Exception e) {
                run.markFailed("invalid startUrl", run.getStats());
                crawlRunRepository.save(run);
                eventHub.publish(runId, "STATUS", Map.of("status", "FAILED", "error", "invalid startUrl"));
                return;
            }

            if (!allowlist.allows(startUri)) {
                run.markFailed("startUrl denied by allowlist", run.getStats());
                crawlRunRepository.save(run);
                eventHub.publish(runId, "STATUS", Map.of("status", "FAILED", "error", "startUrl denied by allowlist"));
                return;
            }

            // Note: If allowlist is empty (no domains/paths specified), it allows all URLs.
            // This enables crawling external sites like example.com without restrictions.

            // seed
            var startPage = getOrCreatePage(runId, run.getStartUrl(), 0);
            pageByUrl.put(run.getStartUrl(), startPage);
            depthByUrl.put(run.getStartUrl(), 0);
            eventHub.publish(runId, "NODE_CREATED", Map.of(
                    "id", startPage.getId(),
                    "url", run.getStartUrl(),
                    "depth", 0,
                    "nodeKey", startPage.getNodeKey()
            ));
            offer(ordering, run.getStartUrl(), bfs, enqueued, mcsScore, mcs, seq);

            Playwright playwright = null;
            Browser browser = null;
            BrowserContext context = null;
            Page page = null;
            Path tempStorageStatePath = null;
            try {
                if (browserMode) {
                    playwright = Playwright.create();
                    // headless=false로 설정하여 실제 브라우저 창을 띄워 상태 변화를 더 정확히 감지
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setSlowMo(100)); // 100ms 지연으로 디버깅 용이
                    
                    // Auth 컨텍스트 주입
                    if (run.getAuthProfile() != null) {
                        var authProfile = authProfileRepository.findById(run.getAuthProfile().getId()).orElse(null);
                        if (authProfile != null) {
                            if (authProfile.getType() == com.dubbi.statetrail.auth.domain.AuthProfileType.STORAGE_STATE 
                                    && authProfile.getStorageStateObjectKey() != null) {
                                try {
                                    // MinIO에서 storage state 로드
                                    var storageStateStream = objectStorageService.loadStorageState(authProfile.getStorageStateObjectKey());
                                    var storageStateJson = new String(storageStateStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                                    
                                    // 임시 파일로 저장 (Playwright는 파일 경로를 요구)
                                    tempStorageStatePath = Files.createTempFile("playwright-storage-state-" + runId + "-", ".json");
                                    Files.write(tempStorageStatePath, storageStateJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    
                                    System.out.printf("[Crawl] Loaded storage state for auth profile: %s (temp file: %s)%n", 
                                        authProfile.getName(), tempStorageStatePath);
                                } catch (Exception e) {
                                    System.err.printf("[Crawl] Failed to load storage state: %s%n", e.getMessage());
                                    e.printStackTrace();
                                }
                            } else if (authProfile.getType() == com.dubbi.statetrail.auth.domain.AuthProfileType.SCRIPT_LOGIN
                                    && authProfile.getLoginScript() != null) {
                                System.out.printf("[Crawl] Auth profile '%s' has login script, will execute after navigation%n", authProfile.getName());
                                // 로그인 스크립트는 startUrl로 이동한 후 실행 (아래에서 처리)
                            }
                        }
                    }
                    
                    // Storage state가 있으면 파일 경로로 주입, 없으면 기본 컨텍스트 생성
                    if (tempStorageStatePath != null) {
                        context = browser.newContext(new Browser.NewContextOptions()
                                .setStorageStatePath(tempStorageStatePath));
                    } else {
                        context = browser.newContext();
                    }
                    
                    // 브라우저 컨텍스트 옵션 설정 (더 나은 상태 감지를 위해)
                    BrowserContext.NewContextOptions contextOptions = new BrowserContext.NewContextOptions()
                            .setViewportSize(1280, 720)
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    
                    if (context == null) {
                        context = browser.newContext(contextOptions);
                    }
                    
                    page = context.newPage();
                    
                    // 디버깅을 위한 로깅
                    System.out.printf("[Crawl] Browser launched in non-headless mode (visible window)%n");
                    
                    // SCRIPT_LOGIN 타입인 경우 로그인 스크립트 실행
                    if (run.getAuthProfile() != null && page != null) {
                        var authProfile = authProfileRepository.findById(run.getAuthProfile().getId()).orElse(null);
                        if (authProfile != null 
                                && authProfile.getType() == com.dubbi.statetrail.auth.domain.AuthProfileType.SCRIPT_LOGIN
                                && authProfile.getLoginScript() != null) {
                            try {
                                // startUrl로 이동
                                page.navigate(run.getStartUrl(), new Page.NavigateOptions().setTimeout(15_000));
                                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                                
                                // 로그인 스크립트 실행 (JavaScript로 평가)
                                // 주의: 실제 로그인 스크립트는 Playwright API 호출로 변환되어야 함
                                // 현재는 간단히 JavaScript로 실행 (향후 더 정교한 파싱 필요)
                                page.evaluate(authProfile.getLoginScript());
                                page.waitForTimeout(1000); // 로그인 완료 대기
                                
                                System.out.printf("[Crawl] Executed login script for auth profile: %s%n", authProfile.getName());
                            } catch (Exception e) {
                                System.err.printf("[Crawl] Failed to execute login script: %s%n", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }

            while (Instant.now().isBefore(deadline)) {
                if (pageByUrl.size() >= budget.maxNodes()) break;
                if (edges >= budget.maxEdges()) break;

                String url = poll(ordering, bfs, mcsScore, mcs, visited);
                if (url == null) break;

                if (visited.contains(url)) continue;
                int depth = depthByUrl.getOrDefault(url, 0);
                if (depth > budget.maxDepth()) {
                    visited.add(url);
                    continue;
                }

                visited.add(url);

                CrawlPageEntity current = pageByUrl.computeIfAbsent(url, u -> getOrCreatePage(runId, u, depth));

                try {
                    PageFetchResult result = browserMode
                            ? fetchWithBrowser(page, url)
                            : fetchWithJsoup(url);

                    current.markFetched(result.status, result.contentType, result.title, result.htmlSnapshot);
                    
                    // 브라우저 모드인 경우 UI 시그니처, 스크린샷, 네트워크 로그 저장
                    if (browserMode && page != null) {
                        if (result.uiSignature() != null && !result.uiSignature().isEmpty()) {
                            current.setUiSignature(result.uiSignature());
                        }
                        
                        // 스크린샷 캡처 및 저장
                        try {
                            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
                            String screenshotKey = objectStorageService.saveScreenshot(runId, current.getId(), screenshot);
                            current.setScreenshotObjectKey(screenshotKey);
                            System.out.printf("[Crawl] Saved screenshot: %s%n", screenshotKey);
                        } catch (Exception e) {
                            System.err.printf("[Crawl] Failed to save screenshot: %s%n", e.getMessage());
                        }
                        
                        // 네트워크 로그를 HAR 형식으로 변환하여 저장
                        if (result.networkRequests() != null && !result.networkRequests().isEmpty()) {
                            try {
                                Map<String, Object> har = createHarFromRequests(result.networkRequests());
                                String harJson = objectMapper.writeValueAsString(har);
                                String networkLogKey = objectStorageService.saveNetworkLog(runId, current.getId(), harJson);
                                current.setNetworkLogObjectKey(networkLogKey);
                                System.out.printf("[Crawl] Saved network log: %s%n", networkLogKey);
                            } catch (Exception e) {
                                System.err.printf("[Crawl] Failed to save network log: %s%n", e.getMessage());
                            }
                        }
                    }
                    
                    crawlPageRepository.save(current);

                    // expand
                    int linksFound = result.links.size();
                    int linksAllowed = 0;
                    int linksEnqueued = 0;
                    for (LinkOut link : result.links) {
                        if (pageByUrl.size() >= budget.maxNodes()) break;
                        if (edges >= budget.maxEdges()) break;
                        if (Instant.now().isAfter(deadline)) break;

                        String toUrl = normalize(url, link.href());
                        if (toUrl == null) {
                            System.out.printf("[Crawl] Failed to normalize link: %s (from %s)%n", link.href(), url);
                            continue;
                        }
                        URI toUri;
                        try {
                            toUri = URI.create(toUrl);
                        } catch (Exception e) {
                            System.out.printf("[Crawl] Failed to create URI from: %s (error: %s)%n", toUrl, e.getMessage());
                            continue;
                        }
                        if (!allowlist.allows(toUri)) {
                            System.out.printf("[Crawl] Link denied by allowlist: %s%n", toUrl);
                            continue;
                        }
                        linksAllowed++;

                        int toDepth = depth + 1;
                        if (toDepth > budget.maxDepth()) {
                            System.out.printf("[Crawl] Link exceeds max depth (%d > %d): %s%n", toDepth, budget.maxDepth(), toUrl);
                            continue;
                        }

                        CrawlPageEntity toPage = pageByUrl.get(toUrl);
                        if (toPage == null) {
                            toPage = getOrCreatePage(runId, toUrl, toDepth);
                            pageByUrl.put(toUrl, toPage);
                            depthByUrl.put(toUrl, toDepth);
                            Map<String, Object> nodeEvent = new HashMap<>();
                            nodeEvent.put("id", toPage.getId());
                            nodeEvent.put("url", toUrl);
                            nodeEvent.put("depth", toDepth);
                            nodeEvent.put("nodeKey", toPage.getNodeKey());
                            if (toPage.getTitle() != null) {
                                nodeEvent.put("title", toPage.getTitle());
                            }
                            eventHub.publish(runId, "NODE_CREATED", nodeEvent);
                        }

                        String edgeKey = current.getId() + "->" + toPage.getId();
                        if (edgeSeen.add(edgeKey)) {
                            try {
                                var linkEntity = new CrawlLinkEntity(UUID.randomUUID(), run, current, toPage, link.anchorText());
                                linkEntity.setActionType(ActionType.NAVIGATE);
                                crawlLinkRepository.save(linkEntity);
                                edges++;
                                
                                Map<String, Object> edgeEvent = new HashMap<>();
                                edgeEvent.put("id", linkEntity.getId());
                                edgeEvent.put("from", current.getId());
                                edgeEvent.put("to", toPage.getId());
                                edgeEvent.put("actionType", ActionType.NAVIGATE.name());
                                if (link.anchorText() != null) {
                                    edgeEvent.put("anchorText", link.anchorText());
                                }
                                eventHub.publish(runId, "EDGE_CREATED", edgeEvent);
                            } catch (Exception ignored) {
                                // ignore duplicates due to race/constraints
                            }
                        }

                        if (visited.contains(toUrl)) {
                            System.out.printf("[Crawl] Link already visited, skipping: %s%n", toUrl);
                        } else if (enqueued.contains(toUrl)) {
                            System.out.printf("[Crawl] Link already enqueued, skipping: %s%n", toUrl);
                        } else {
                            linksEnqueued++;
                            System.out.printf("[Crawl] Enqueuing link: %s (depth=%d)%n", toUrl, toDepth);
                            if (ordering == CrawlStrategy.MCS) {
                                mcsScore.put(toUrl, mcsScore.getOrDefault(toUrl, 0) + 1);
                            }
                            offer(ordering, toUrl, bfs, enqueued, mcsScore, mcs, seq);
                        }
                    }
                    
                    // Log link extraction stats for debugging
                    if (linksFound > 0) {
                        System.out.printf("[Crawl] %s: found %d links, %d allowed, %d enqueued (depth=%d)%n", 
                            url, linksFound, linksAllowed, linksEnqueued, depth);
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.printf("[Crawl] Error fetching %s: %s%n", url, e.getMessage());
                    e.printStackTrace();
                }

                // stats heartbeat
                if (visited.size() % 5 == 0) {
                    var stats = Map.<String, Object>of("nodes", pageByUrl.size(), "edges", edges, "errors", errors, "visited", visited.size());
                    run.updateStats(stats);
                    crawlRunRepository.save(run);
                    eventHub.publish(runId, "STATS", stats);
                }
            }
            } finally {
                if (page != null) page.close();
                if (context != null) context.close();
                if (browser != null) browser.close();
                if (playwright != null) playwright.close();
                
                // 임시 storage state 파일 정리
                if (tempStorageStatePath != null) {
                    try {
                        Files.deleteIfExists(tempStorageStatePath);
                    } catch (IOException e) {
                        System.err.printf("[Crawl] Failed to delete temp storage state file: %s%n", e.getMessage());
                    }
                }
            }

            var finalStats = Map.<String, Object>of(
                    "nodes", pageByUrl.size(),
                    "edges", edges,
                    "errors", errors,
                    "visited", visited.size(),
                    "finishedReason", Instant.now().isAfter(deadline) ? "TIME" : "BUDGET_OR_FRONTIER"
            );
            run.markSucceeded(finalStats);
            crawlRunRepository.save(run);
            eventHub.publish(runId, "STATUS", Map.of("status", "SUCCEEDED", "finishedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()));
            eventHub.publish(runId, "STATS", finalStats);
        } catch (Exception fatal) {
            var stats = Map.<String, Object>of("edges", edges, "errors", errors);
            run.markFailed("crawler crashed: " + fatal.getClass().getSimpleName() + ": " + fatal.getMessage(), stats);
            crawlRunRepository.save(run);
            eventHub.publish(runId, "STATUS", Map.of("status", "FAILED", "error", run.getErrorMessage()));
        }
    }

    private record LinkOut(String href, String anchorText) {}
    
    /**
     * 액션 후보: 클릭 가능한 요소를 나타냄
     */
    private record ActionCandidate(
        String type,           // "click", "input", "submit", "navigate"
        String text,           // 버튼/링크 텍스트
        String selector,       // CSS selector
        String href,           // 링크인 경우 href (null 가능)
        int priority           // 우선순위: 1=아코디언/메뉴, 2=페이지네이션, 3=일반 버튼
    ) {}
    
    /**
     * 상태 변화 감지 결과
     */
    private record StateChangeResult(
        boolean changed,              // 상태가 변경되었는지
        String newUrl,                // 새 URL (변경된 경우)
        String newDomHash,            // 새 DOM 해시
        Map<String, Object> newUiSignature,  // 새 UI 시그니처
        List<LinkOut> discoveredLinks // 새로 발견된 링크들
    ) {}
    private record FrontierItem(String url, int score, long seq) {}
    private record PageFetchResult(
            Integer status, 
            String contentType, 
            String title, 
            String htmlSnapshot, 
            Set<LinkOut> links,
            Map<String, Object> uiSignature,
            List<Map<String, Object>> networkRequests
    ) {}

    private PageFetchResult fetchWithJsoup(String url) throws Exception {
        Connection.Response res = Jsoup.connect(url)
                .userAgent("StateTrailBot/0.1")
                .timeout(10_000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .execute();

        String contentType = res.contentType();
        Integer status = res.statusCode();
        String body = res.body();

        String title = null;
        Set<LinkOut> links = Set.of();
        if (contentType != null && contentType.toLowerCase().contains("text/html")) {
            Document doc = res.parse();
            title = doc.title();
            links = extractLinks(doc);
        }

        String snapshot = body == null ? null : (body.length() > 200_000 ? body.substring(0, 200_000) : body);
        return new PageFetchResult(status, contentType, title, snapshot, links, Map.of(), List.of());
    }

    private PageFetchResult fetchWithBrowser(Page page, String url) {
        // 네트워크 요청 추적 시작
        List<Map<String, Object>> networkRequests = new ArrayList<>();
        
        // Request 리스너 등록
        page.onRequest(request -> {
            Map<String, Object> req = new HashMap<>();
            req.put("url", request.url());
            req.put("method", request.method());
            req.put("resourceType", request.resourceType() != null ? request.resourceType() : "other");
            req.put("headers", request.headers() != null ? request.headers() : Map.of());
            networkRequests.add(req);
        });
        
        Response res = page.navigate(url, new Page.NavigateOptions().setTimeout(15_000));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        // SPA hydration / client fetch time - React 앱이 완전히 렌더링될 때까지 대기
        page.waitForTimeout(3000);
        // 네트워크가 안정될 때까지 추가 대기 (API 호출 완료 대기)
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5_000));
        } catch (Exception e) {
            // 타임아웃되어도 계속 진행
        }
        // React Router가 완전히 초기화될 때까지 추가 대기
        page.waitForTimeout(1000);

        String contentType = null;
        Integer status = null;
        if (res != null) {
            status = res.status();
            contentType = res.headers().getOrDefault("content-type", null);
        }

        String title = null;
        String html = null;
        try {
            title = page.title();
        } catch (Exception ignored) {}
        try {
            html = page.content();
        } catch (Exception ignored) {}

        // UI 시그니처 추출
        Map<String, Object> uiSignature = UiSignatureExtractor.extractFromPage(page);

        // 상태/행동 탐색 방식: 액션 후보 추출 및 실행
        Set<LinkOut> links = extractActionsAndDiscoverLinks(page, uiSignature);
        String snapshot = html == null ? null : (html.length() > 200_000 ? html.substring(0, 200_000) : html);
        return new PageFetchResult(status, contentType, title, snapshot, links, uiSignature, networkRequests);
    }
    
    /**
     * 상태/행동 탐색 방식으로 액션 후보를 추출하고 실행하여 링크 발견
     */
    private static Set<LinkOut> extractActionsAndDiscoverLinks(Page page, Map<String, Object> uiSignature) {
        Set<LinkOut> links = new HashSet<>();
        
        // 1. 먼저 일반적인 <a href> 링크 추출
        Set<LinkOut> staticLinks = extractStaticLinks(page);
        links.addAll(staticLinks);
        
        // 2. 정적 링크가 없거나 적으면, 액션 기반 탐색 수행
        if (staticLinks.isEmpty()) {
            System.out.println("[Crawl] Browser: No static links found, switching to action-based exploration");
            
            // 액션 후보 추출
            List<ActionCandidate> actions = extractActionsFromUiSignature(uiSignature);
            System.out.printf("[Crawl] Browser: Extracted %d action candidates%n", actions.size());
            
            // 현재 상태 저장
            String currentUrl = page.url();
            String currentDomHash = (String) uiSignature.getOrDefault("domHash", "");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> currentCTAs = (List<Map<String, Object>>) uiSignature.getOrDefault("ctas", List.of());
            int currentCTACount = currentCTAs.size();
            
            // 우선순위별로 액션 실행 (아코디언/메뉴 우선)
            List<ActionCandidate> navigationActions = actions.stream()
                .filter(a -> a.priority() == 1)
                .toList();
            
            System.out.printf("[Crawl] Browser: Found %d priority-1 (navigation) actions out of %d total actions%n", 
                    navigationActions.size(), actions.size());
            
            // Priority 1이 없으면 모든 액션 실행 (fallback)
            List<ActionCandidate> actionsToExecute = navigationActions.isEmpty() ? actions : navigationActions;
            if (navigationActions.isEmpty()) {
                System.out.println("[Crawl] Browser: No priority-1 actions found, executing all actions");
            }
            
            // 1단계: 네비게이션 액션 (아코디언/메뉴) 먼저 실행하여 상태 확장
            for (ActionCandidate action : actionsToExecute) {
                if (links.size() >= 20) break; // 최대 20개까지만
                
                System.out.printf("[Crawl] Browser: Executing action '%s' (type=%s, priority=%d)%n", 
                        action.text(), action.type(), action.priority());
                
                StateChangeResult result = tryActionAndDetectStateChange(page, action, currentUrl, currentDomHash, uiSignature);
                
                if (result.changed()) {
                    System.out.printf("[Crawl] Browser: State changed after action '%s': URL=%s -> %s, domHash=%s -> %s%n", 
                            action.text(), currentUrl, result.newUrl(), currentDomHash, result.newDomHash());
                    
                    // 새로 발견된 링크 추가
                    links.addAll(result.discoveredLinks());
                    System.out.printf("[Crawl] Browser: Discovered %d links from action '%s'%n", 
                            result.discoveredLinks().size(), action.text());
                    
                    // 상태가 변경되었으므로, 새로운 UI Signature에서 추가 액션 추출 가능
                    if (result.newUiSignature() != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> newCTAs = (List<Map<String, Object>>) result.newUiSignature().getOrDefault("ctas", List.of());
                        if (newCTAs.size() > currentCTACount) {
                            System.out.printf("[Crawl] Browser: New CTAs discovered after action '%s': %d -> %d%n", 
                                    action.text(), currentCTACount, newCTAs.size());
                            
                            // 새로 나타난 CTA에서 링크 추출 시도
                            for (Map<String, Object> newCTA : newCTAs) {
                                String newHref = (String) newCTA.get("href");
                                if (newHref != null && !newHref.isBlank()) {
                                    try {
                                        String newText = (String) newCTA.getOrDefault("text", "");
                                        links.add(new LinkOut(newHref, newText));
                                        System.out.printf("[Crawl] Browser: Found link in new CTA: %s (text: '%s')%n", newHref, newText);
                                    } catch (Exception e) {
                                        // 무시
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.printf("[Crawl] Browser: No state change detected after action '%s'%n", action.text());
                }
            }
            
            // 2단계: 확장된 상태에서 정적 링크 다시 추출
            if (!links.isEmpty()) {
                Set<LinkOut> expandedLinks = extractStaticLinks(page);
                links.addAll(expandedLinks);
                System.out.printf("[Crawl] Browser: Discovered %d total links after action-based exploration%n", links.size());
            }
        }
        
        return links;
    }
    
    /**
     * UI Signature에서 액션 후보 추출 (우선순위 포함)
     */
    private static List<ActionCandidate> extractActionsFromUiSignature(Map<String, Object> uiSignature) {
        List<ActionCandidate> actions = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ctas = (List<Map<String, Object>>) uiSignature.getOrDefault("ctas", List.of());
        
        for (Map<String, Object> cta : ctas) {
            String type = (String) cta.getOrDefault("type", "button");
            String text = (String) cta.getOrDefault("text", "");
            String selector = (String) cta.getOrDefault("selector", "");
            String href = (String) cta.get("href");
            
            if (text.isBlank() && selector.isBlank()) continue;
            
            // 우선순위 결정
            int priority = 3; // 기본: 일반 버튼
            String lowerText = text.toLowerCase();
            String lowerSelector = selector.toLowerCase();
            
            // 아코디언/메뉴 우선순위 1
            if (lowerSelector.contains("accordion") || 
                lowerSelector.contains("menuitem") ||
                lowerText.contains("관리") ||
                lowerText.contains("menu") ||
                lowerText.contains("nav")) {
                priority = 1;
            }
            // 페이지네이션 우선순위 2
            else if (lowerSelector.contains("pagination") ||
                     lowerText.matches("^\\d+$") || // 숫자만 (페이지 번호)
                     lowerText.equals("next") ||
                     lowerText.equals("prev") ||
                     lowerText.equals("이전") ||
                     lowerText.equals("다음")) {
                priority = 2;
            }
            
            String actionType = href != null ? "navigate" : "click";
            ActionCandidate candidate = new ActionCandidate(actionType, text, selector, href, priority);
            actions.add(candidate);
            System.out.printf("[Crawl] Browser: Added action candidate: text='%s', type=%s, priority=%d, selector='%s'%n", 
                    text, actionType, priority, selector);
        }
        
        // 우선순위별 정렬
        actions.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
        
        return actions;
    }
    
    /**
     * 액션 실행 후 상태 변화 감지
     */
    private static StateChangeResult tryActionAndDetectStateChange(
            Page page, 
            ActionCandidate action, 
            String beforeUrl, 
            String beforeDomHash,
            Map<String, Object> beforeUiSignature) {
        
        try {
            // 액션 실행
            boolean clicked = false;
            if ("navigate".equals(action.type()) && action.href() != null) {
                // 링크인 경우 직접 URL로 이동
                try {
                    System.out.printf("[Crawl] Browser: Navigating to %s%n", action.href());
                    page.navigate(action.href(), new Page.NavigateOptions().setTimeout(5000));
                    clicked = true;
                } catch (Exception e) {
                    System.out.printf("[Crawl] Browser: Failed to navigate to %s: %s%n", action.href(), e.getMessage());
                }
            } else {
                // 버튼 클릭 - Playwright Locator 사용 (더 안정적)
                System.out.printf("[Crawl] Browser: Attempting to click button with text '%s'%n", action.text());
                try {
                    // 텍스트로 버튼 찾기 (접근성 기반)
                    Locator buttonLocator = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, 
                        new Page.GetByRoleOptions().setName(action.text()).setExact(true));
                    
                    // 버튼이 보이는지 확인
                    if (buttonLocator.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                        System.out.printf("[Crawl] Browser: Button '%s' is visible, clicking...%n", action.text());
                        buttonLocator.click(new Locator.ClickOptions().setTimeout(5000));
                        clicked = true;
                        System.out.printf("[Crawl] Browser: Successfully clicked button '%s' using Locator%n", action.text());
                    } else {
                        System.out.printf("[Crawl] Browser: Button '%s' not visible, trying fallback method%n", action.text());
                        // Fallback: JavaScript로 클릭
                        String textEscaped = action.text().replace("\\", "\\\\").replace("\"", "\\\"");
                        Object result = page.evaluate(String.format("""
                            () => {
                                const text = "%s";
                                const buttons = Array.from(document.querySelectorAll('button, [role="button"]'));
                                for (const btn of buttons) {
                                    const btnText = (btn.innerText || btn.textContent || '').trim();
                                    if (btnText === text) {
                                        // 강제로 클릭 이벤트 발생
                                        btn.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                        btn.focus();
                                        btn.click();
                                        // 추가로 이벤트 트리거
                                        const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
                                        btn.dispatchEvent(clickEvent);
                                        return true;
                                    }
                                }
                                return false;
                            }
                        """, textEscaped));
                        clicked = result instanceof Boolean && (Boolean) result;
                        if (clicked) {
                            System.out.printf("[Crawl] Browser: Successfully clicked button '%s' using JavaScript fallback%n", action.text());
                        } else {
                            System.out.printf("[Crawl] Browser: Failed to find/click button '%s'%n", action.text());
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("[Crawl] Browser: Error clicking button '%s': %s, trying JavaScript fallback%n", 
                            action.text(), e.getMessage());
                    // Fallback: JavaScript로 클릭
                    try {
                        String textEscaped = action.text().replace("\\", "\\\\").replace("\"", "\\\"");
                        Object result = page.evaluate(String.format("""
                            () => {
                                const text = "%s";
                                const buttons = Array.from(document.querySelectorAll('button, [role="button"]'));
                                for (const btn of buttons) {
                                    const btnText = (btn.innerText || btn.textContent || '').trim();
                                    if (btnText === text) {
                                        btn.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                        btn.focus();
                                        btn.click();
                                        const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
                                        btn.dispatchEvent(clickEvent);
                                        return true;
                                    }
                                }
                                return false;
                            }
                        """, textEscaped));
                        clicked = result instanceof Boolean && (Boolean) result;
                        if (clicked) {
                            System.out.printf("[Crawl] Browser: Successfully clicked button '%s' using JavaScript fallback%n", action.text());
                        }
                    } catch (Exception e2) {
                        System.out.printf("[Crawl] Browser: JavaScript fallback also failed: %s%n", e2.getMessage());
                    }
                }
            }
            
            if (!clicked) {
                return new StateChangeResult(false, beforeUrl, beforeDomHash, null, List.of());
            }
            
            // 아코디언이 실제로 펼쳐질 때까지 대기 (더 정교한 감지)
            System.out.printf("[Crawl] Browser: Waiting for accordion '%s' to expand...%n", action.text());
            boolean accordionIsExpanded = false;
            int newLinksCount = 0;
            try {
                // 아코디언이 펼쳐지고 실제로 내용이 나타날 때까지 최대 5초 대기
                String textEscaped = action.text().replace("\\", "\\\\").replace("\"", "\\\"");
                Object expandedResult = page.waitForFunction(String.format("""
                    () => {
                        const text = "%s";
                        const buttons = document.querySelectorAll('button, [role="button"]');
                        for (const btn of buttons) {
                            const btnText = (btn.innerText || btn.textContent || '').trim();
                            if (btnText === text) {
                                // 1. aria-expanded 확인
                                const ariaExpanded = btn.getAttribute('aria-expanded') === 'true' ||
                                                    btn.closest('[aria-expanded="true"]') !== null;
                                
                                // 2. MUI 클래스 확인
                                const muiExpanded = btn.closest('.Mui-expanded') !== null ||
                                                   btn.closest('.MuiAccordion-expanded') !== null;
                                
                                // 3. 아코디언 패널 찾기
                                let accordionPanel = btn.closest('.MuiAccordion-root');
                                if (!accordionPanel) {
                                    accordionPanel = btn.parentElement;
                                    while (accordionPanel && !accordionPanel.classList.contains('MuiAccordion-root')) {
                                        accordionPanel = accordionPanel.parentElement;
                                    }
                                }
                                
                                // 4. 패널이 실제로 보이는지 확인 (display, height, visibility)
                                let panelVisible = false;
                                if (accordionPanel) {
                                    const panelDetails = accordionPanel.querySelector('.MuiAccordionDetails-root, .MuiCollapse-root, [class*="AccordionDetails"], [class*="Collapse"]');
                                    if (panelDetails) {
                                        const style = window.getComputedStyle(panelDetails);
                                        const height = panelDetails.offsetHeight || panelDetails.clientHeight;
                                        panelVisible = style.display !== 'none' && 
                                                     style.visibility !== 'hidden' && 
                                                     height > 0;
                                    }
                                }
                                
                                // 5. 펼쳐진 패널 내부에 링크가 있는지 확인
                                let hasLinks = false;
                                if (accordionPanel) {
                                    const links = accordionPanel.querySelectorAll('a[href], [class*="ListItemButton"] a, [class*="ListItem-root"] a, [role="link"]');
                                    hasLinks = links.length > 0;
                                }
                                
                                // 6. 펼쳐진 패널 내부에 새로운 버튼/메뉴 항목이 있는지 확인
                                let hasNewItems = false;
                                if (accordionPanel) {
                                    const items = accordionPanel.querySelectorAll('[class*="ListItem"], [class*="MenuItem"], [role="menuitem"]');
                                    hasNewItems = items.length > 0;
                                }
                                
                                // 아코디언이 펼쳐졌고 실제로 내용이 보이는 경우
                                const expanded = ariaExpanded || muiExpanded;
                                const hasContent = panelVisible || hasLinks || hasNewItems;
                                
                                return expanded && hasContent;
                            }
                        }
                        return false;
                    }
                """, textEscaped), new Page.WaitForFunctionOptions().setTimeout(5000));
                accordionIsExpanded = expandedResult instanceof Boolean && (Boolean) expandedResult;
                
                // 펼쳐진 패널 내부의 링크 개수 확인
                if (accordionIsExpanded) {
                    String textEscaped2 = action.text().replace("\\", "\\\\").replace("\"", "\\\"");
                    Object linksCount = page.evaluate(String.format("""
                        () => {
                            const text = "%s";
                            const buttons = document.querySelectorAll('button, [role="button"]');
                            for (const btn of buttons) {
                                const btnText = (btn.innerText || btn.textContent || '').trim();
                                if (btnText === text) {
                                    let accordionPanel = btn.closest('.MuiAccordion-root');
                                    if (!accordionPanel) {
                                        accordionPanel = btn.parentElement;
                                        while (accordionPanel && !accordionPanel.classList.contains('MuiAccordion-root')) {
                                            accordionPanel = accordionPanel.parentElement;
                                        }
                                    }
                                    if (accordionPanel) {
                                        const links = accordionPanel.querySelectorAll('a[href], [class*="ListItemButton"], [class*="ListItem-root"], [role="link"], [role="menuitem"]');
                                        return links.length;
                                    }
                                }
                            }
                            return 0;
                        }
                    """, textEscaped2));
                    if (linksCount instanceof Number) {
                        newLinksCount = ((Number) linksCount).intValue();
                    }
                }
                
                System.out.printf("[Crawl] Browser: Accordion expanded after click: %s (waited for expansion, found %d new items)%n", 
                        accordionIsExpanded, newLinksCount);
            } catch (Exception e) {
                // 타임아웃되었거나 아코디언이 아닐 수 있음
                System.out.printf("[Crawl] Browser: Timeout waiting for accordion expansion or not an accordion: %s%n", e.getMessage());
                // 다시 한 번 확인 (간단한 버전)
                String textEscaped = action.text().replace("\\", "\\\\").replace("\"", "\\\"");
                Object accordionExpanded = page.evaluate(String.format("""
                    () => {
                        const text = "%s";
                        const buttons = document.querySelectorAll('button, [role="button"]');
                        for (const btn of buttons) {
                            const btnText = (btn.innerText || btn.textContent || '').trim();
                            if (btnText === text) {
                                const expanded = btn.getAttribute('aria-expanded') === 'true' ||
                                               btn.closest('[aria-expanded="true"]') !== null ||
                                               btn.closest('.Mui-expanded') !== null ||
                                               btn.closest('.MuiAccordion-expanded') !== null;
                                
                                // 패널 내부 링크 확인
                                let accordionPanel = btn.closest('.MuiAccordion-root');
                                if (!accordionPanel) {
                                    accordionPanel = btn.parentElement;
                                    while (accordionPanel && !accordionPanel.classList.contains('MuiAccordion-root')) {
                                        accordionPanel = accordionPanel.parentElement;
                                    }
                                }
                                const hasLinks = accordionPanel && accordionPanel.querySelectorAll('a[href], [class*="ListItem"]').length > 0;
                                
                                return expanded || hasLinks;
                            }
                        }
                        return false;
                    }
                """, textEscaped));
                accordionIsExpanded = accordionExpanded instanceof Boolean && (Boolean) accordionExpanded;
                System.out.printf("[Crawl] Browser: Accordion expanded check (final): %s%n", accordionIsExpanded);
            }
            
            // DOM 안정화 대기
            page.waitForTimeout(500);
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(2000));
            } catch (Exception e) {
                // 타임아웃되어도 계속
            }
            page.waitForTimeout(500);
            
            // 상태 변화 확인
            String afterUrl = page.url();
            Map<String, Object> newUiSignature = UiSignatureExtractor.extractFromPage(page);
            String afterDomHash = (String) newUiSignature.getOrDefault("domHash", "");
            
            // 더 정교한 DOM 변화 감지: CTA 개수 변화도 확인
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> beforeCTAs = beforeUiSignature != null ? 
                (List<Map<String, Object>>) beforeUiSignature.getOrDefault("ctas", List.of()) : List.of();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> afterCTAs = (List<Map<String, Object>>) 
                newUiSignature.getOrDefault("ctas", List.of());
            
            boolean urlChanged = !afterUrl.equals(beforeUrl);
            boolean domHashChanged = !afterDomHash.equals(beforeDomHash);
            boolean ctaCountChanged = afterCTAs.size() != beforeCTAs.size();
            
            // 아코디언이 펼쳐졌거나, DOM 해시가 변경되었거나, CTA 개수가 변경되었거나, 새로운 링크가 발견되었으면 상태 변화로 간주
            boolean domChanged = accordionIsExpanded || domHashChanged || ctaCountChanged || (newLinksCount > 0);
            
            System.out.printf("[Crawl] Browser: State check - URL changed: %s, DOM hash changed: %s, CTA count changed: %s, Accordion expanded: %s, New links found: %d%n",
                    urlChanged, domHashChanged, ctaCountChanged, accordionIsExpanded, newLinksCount);
            
            // 상태 변화 감지: URL 변경 또는 DOM 변경(아코디언 펼침 포함)
            // 중요: 아코디언이 펼쳐졌거나 새로운 링크가 발견되었으면 무조건 상태 변화로 간주
            if (urlChanged || domChanged || accordionIsExpanded || newLinksCount > 0) {
                System.out.printf("[Crawl] Browser: State change detected - URL changed: %s, DOM changed: %s, accordion expanded: %s, new links: %d%n", 
                        urlChanged, domChanged, accordionIsExpanded, newLinksCount);
                
                // 새로 발견된 링크 추출 (펼쳐진 아코디언 내부 링크 포함)
                List<LinkOut> discoveredLinks = extractStaticLinks(page).stream().toList();
                System.out.printf("[Crawl] Browser: Found %d static links in changed state%n", discoveredLinks.size());
                
                // 아코디언이 펼쳐진 경우, 펼쳐진 영역에서 추가 링크 찾기 (항상 실행)
                if (accordionIsExpanded || newLinksCount > 0) {
                    List<LinkOut> accordionLinks = extractLinksFromExpandedAccordion(page, action.text());
                    discoveredLinks = new ArrayList<>(discoveredLinks);
                    discoveredLinks.addAll(accordionLinks);
                    System.out.printf("[Crawl] Browser: Found %d additional links from expanded accordion%n", accordionLinks.size());
                    
                    // 아코디언이 펼쳐졌지만 링크를 못 찾은 경우, 더 공격적으로 찾기
                    if (accordionLinks.isEmpty() && newLinksCount > 0) {
                        System.out.printf("[Crawl] Browser: Accordion expanded but no links found, trying aggressive extraction...%n");
                        List<LinkOut> aggressiveLinks = extractLinksAggressively(page, action.text());
                        discoveredLinks.addAll(aggressiveLinks);
                        System.out.printf("[Crawl] Browser: Found %d links with aggressive extraction%n", aggressiveLinks.size());
                    }
                }
                
                return new StateChangeResult(true, afterUrl, afterDomHash, newUiSignature, discoveredLinks);
            } else {
                System.out.printf("[Crawl] Browser: No state change detected (URL: %s, DOM: %s, accordion: %s, new links: %d)%n", 
                        urlChanged, domChanged, accordionIsExpanded, newLinksCount);
            }
            
            return new StateChangeResult(false, beforeUrl, beforeDomHash, null, List.of());
            
        } catch (Exception e) {
            System.out.printf("[Crawl] Browser: Error executing action '%s': %s%n", action.text(), e.getMessage());
            return new StateChangeResult(false, beforeUrl, beforeDomHash, null, List.of());
        }
    }
    
    /**
     * 펼쳐진 아코디언에서 링크 추출
     */
    private static List<LinkOut> extractLinksFromExpandedAccordion(Page page, String accordionText) {
        List<LinkOut> links = new ArrayList<>();
        
        try {
            String textEscaped = accordionText.replace("\\", "\\\\").replace("\"", "\\\"");
            Object result = page.evaluate(String.format("""
                () => {
                    const accordionText = "%s";
                    const links = [];
                    const baseUrl = window.location.origin;
                    const currentUrl = window.location.href.split('#')[0];
                    
                    // 아코디언 버튼 찾기
                    let accordionButton = null;
                    const buttons = document.querySelectorAll('button, [role="button"]');
                    for (const btn of buttons) {
                        const btnText = (btn.innerText || btn.textContent || '').trim();
                        if (btnText === accordionText) {
                            accordionButton = btn;
                            break;
                        }
                    }
                    
                    if (!accordionButton) return links;
                    
                    // 아코디언 패널 찾기 (MUI Accordion 패턴)
                    let accordionPanel = accordionButton.closest('.MuiAccordion-root');
                    if (!accordionPanel) {
                        // 부모 요소에서 찾기
                        accordionPanel = accordionButton.parentElement;
                        while (accordionPanel && !accordionPanel.classList.contains('MuiAccordion-root')) {
                            accordionPanel = accordionPanel.parentElement;
                        }
                    }
                    
                    if (accordionPanel) {
                        // 펼쳐진 패널 내부의 모든 링크 찾기
                        accordionPanel.querySelectorAll('a[href], [class*="MuiListItemButton"] a, [class*="MuiListItem-root"] a').forEach(a => {
                            try {
                                const href = a.href || a.getAttribute('href');
                                if (href && typeof href === 'string' && 
                                    !href.startsWith('javascript:') && 
                                    !href.startsWith('#') && 
                                    href !== '') {
                                    let url;
                                    try {
                                        url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                        url = url.split('#')[0];
                                    } catch (e) {
                                        return;
                                    }
                                    
                                    if (url !== currentUrl && (url.startsWith('http://') || url.startsWith('https://'))) {
                                        const linkText = (a.innerText || a.textContent || '').trim().slice(0, 100);
                                        links.push({
                                            url: url,
                                            text: linkText || url
                                        });
                                    }
                                }
                            } catch (e) {}
                        });
                        
                        // ListItemButton 내부의 링크도 찾기 (React Router Link)
                        accordionPanel.querySelectorAll('[class*="MuiListItemButton"], [class*="MuiListItem-root"]').forEach(item => {
                            try {
                                let target = item;
                                while (target && target !== accordionPanel) {
                                    if (target.tagName === 'A') {
                                        const href = target.href || target.getAttribute('href');
                                        if (href && typeof href === 'string' && 
                                            !href.startsWith('javascript:') && 
                                            !href.startsWith('#') && 
                                            href !== '') {
                                            try {
                                                let url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                                url = url.split('#')[0];
                                                if (url !== currentUrl && (url.startsWith('http://') || url.startsWith('https://'))) {
                                                    const linkText = (target.innerText || target.textContent || item.innerText || item.textContent || '').trim().slice(0, 100);
                                                    links.push({
                                                        url: url,
                                                        text: linkText || url
                                                    });
                                                }
                                            } catch (e) {}
                                        }
                                        break;
                                    }
                                    target = target.parentElement;
                                }
                            } catch (e) {}
                        });
                    }
                    
                    return links;
                }
            """, textEscaped));
            
            if (result instanceof List<?>) {
                for (Object linkObj : (List<?>) result) {
                    if (linkObj instanceof Map<?, ?>) {
                        Map<?, ?> linkMap = (Map<?, ?>) linkObj;
                        Object urlObj = linkMap.get("url");
                        Object textObj = linkMap.get("text");
                        if (urlObj instanceof String) {
                            String url = (String) urlObj;
                            String linkText = textObj instanceof String ? (String) textObj : url;
                            links.add(new LinkOut(url, linkText));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("[Crawl] Browser: Error extracting links from expanded accordion '%s': %s%n", 
                    accordionText, e.getMessage());
        }
        
        return links;
    }
    
    /**
     * 공격적인 링크 추출 (아코디언이 펼쳐졌지만 링크를 못 찾은 경우)
     */
    private static List<LinkOut> extractLinksAggressively(Page page, String accordionText) {
        List<LinkOut> links = new ArrayList<>();
        
        try {
            String textEscaped = accordionText.replace("\\", "\\\\").replace("\"", "\\\"");
            Object result = page.evaluate(String.format("""
                () => {
                    const accordionText = "%s";
                    const links = [];
                    const baseUrl = window.location.origin;
                    const currentUrl = window.location.href.split('#')[0];
                    
                    // 아코디언 버튼 찾기
                    let accordionButton = null;
                    const buttons = document.querySelectorAll('button, [role="button"]');
                    for (const btn of buttons) {
                        const btnText = (btn.innerText || btn.textContent || '').trim();
                        if (btnText === accordionText) {
                            accordionButton = btn;
                            break;
                        }
                    }
                    
                    if (!accordionButton) return links;
                    
                    // 아코디언 패널 찾기
                    let accordionPanel = accordionButton.closest('.MuiAccordion-root');
                    if (!accordionPanel) {
                        accordionPanel = accordionButton.parentElement;
                        while (accordionPanel && !accordionPanel.classList.contains('MuiAccordion-root')) {
                            accordionPanel = accordionPanel.parentElement;
                        }
                    }
                    
                    if (!accordionPanel) return links;
                    
                    // 모든 가능한 클릭 가능한 요소 찾기
                    const selectors = [
                        'a[href]',
                        '[class*="ListItemButton"]',
                        '[class*="ListItem-root"]',
                        '[class*="MenuItem"]',
                        '[role="link"]',
                        '[role="menuitem"]',
                        '[onclick]',
                        '[data-href]',
                        '[data-to]',
                        '[data-path]',
                        '[data-route]'
                    ];
                    
                    selectors.forEach(selector => {
                        accordionPanel.querySelectorAll(selector).forEach(el => {
                            try {
                                let url = null;
                                let linkText = '';
                                
                                // href 속성
                                if (el.tagName === 'A' && el.href) {
                                    url = el.href;
                                    linkText = (el.innerText || el.textContent || '').trim();
                                }
                                // data 속성
                                else if (el.getAttribute('data-href')) {
                                    url = el.getAttribute('data-href');
                                    linkText = (el.innerText || el.textContent || '').trim();
                                }
                                else if (el.getAttribute('data-to')) {
                                    url = el.getAttribute('data-to');
                                    linkText = (el.innerText || el.textContent || '').trim();
                                }
                                else if (el.getAttribute('data-path')) {
                                    url = new URL(el.getAttribute('data-path'), baseUrl).href;
                                    linkText = (el.innerText || el.textContent || '').trim();
                                }
                                // onClick 핸들러가 있는 경우 (React Router 등)
                                else if (el.onclick || el.getAttribute('onclick')) {
                                    // onClick 핸들러는 분석하기 어려우므로, 부모 요소에서 href 찾기
                                    let parent = el.parentElement;
                                    while (parent && parent !== accordionPanel) {
                                        if (parent.tagName === 'A' && parent.href) {
                                            url = parent.href;
                                            linkText = (el.innerText || el.textContent || '').trim();
                                            break;
                                        }
                                        parent = parent.parentElement;
                                    }
                                }
                                
                                if (url && typeof url === 'string' && 
                                    !url.startsWith('javascript:') && 
                                    !url.startsWith('#') && 
                                    url !== '' &&
                                    url !== currentUrl &&
                                    (url.startsWith('http://') || url.startsWith('https://'))) {
                                    url = url.split('#')[0];
                                    links.push({
                                        url: url,
                                        text: linkText || url
                                    });
                                }
                            } catch (e) {}
                        });
                    });
                    
                    // 중복 제거
                    const uniqueLinks = [];
                    const seenUrls = new Set();
                    links.forEach(link => {
                        if (!seenUrls.has(link.url)) {
                            seenUrls.add(link.url);
                            uniqueLinks.push(link);
                        }
                    });
                    
                    return uniqueLinks;
                }
            """, textEscaped));
            
            if (result instanceof List<?>) {
                for (Object linkObj : (List<?>) result) {
                    if (linkObj instanceof Map<?, ?>) {
                        Map<?, ?> linkMap = (Map<?, ?>) linkObj;
                        Object urlObj = linkMap.get("url");
                        Object textObj = linkMap.get("text");
                        if (urlObj instanceof String) {
                            String url = (String) urlObj;
                            String linkText = textObj instanceof String ? (String) textObj : url;
                            links.add(new LinkOut(url, linkText));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("[Crawl] Browser: Error in aggressive link extraction for '%s': %s%n", 
                    accordionText, e.getMessage());
        }
        
        return links;
    }
    
    /**
     * 정적 링크 추출 (<a href> 태그)
     */
    private static Set<LinkOut> extractStaticLinks(Page page) {
        Set<LinkOut> links = new HashSet<>();
        
        try {
            Object raw = page.evaluate("""
                () => {
                    const links = [];
                    const baseUrl = window.location.origin;
                    const currentUrl = window.location.href.split('#')[0];
                    
                    document.querySelectorAll('a[href]').forEach(a => {
                        try {
                            const href = a.href || a.getAttribute('href');
                            if (href && typeof href === 'string' && 
                                !href.startsWith('javascript:') && 
                                !href.startsWith('#') && 
                                href !== '' &&
                                href !== currentUrl) {
                                let url;
                                try {
                                    url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                    url = url.split('#')[0];
                                } catch (e) {
                                    return;
                                }
                                
                                if (url.startsWith('http://') || url.startsWith('https://')) {
                                    const text = (a.innerText || a.textContent || '').trim().slice(0, 200);
                                    links.push({
                                        href: url,
                                        text: text || url
                                    });
                                }
                            }
                        } catch (e) {}
                    });
                    
                    return links;
                }
            """);
            
            if (raw instanceof List<?>) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof Map<?, ?>) {
                        Map<?, ?> m = (Map<?, ?>) item;
                        Object hrefObj = m.get("href");
                        Object textObj = m.get("text");
                        if (hrefObj instanceof String) {
                            String href = (String) hrefObj;
                            String text = textObj instanceof String ? (String) textObj : href;
                            links.add(new LinkOut(href, text));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("[Crawl] Browser: Error extracting static links: %s%n", e.getMessage());
        }
        
        return links;
    }

    // Deprecated: extractActionsAndDiscoverLinks로 대체됨
    @Deprecated
    private static Set<LinkOut> extractLinksFromBrowser(Page page, Map<String, Object> uiSignature) {
        Set<LinkOut> links = new HashSet<>();
        
        // 먼저 일반적인 <a href> 링크 추출
        Object raw = page.evaluate("""
            () => {
                const links = new Set();
                const baseUrl = window.location.origin;
                const currentUrl = window.location.href;
                
                // 디버깅 정보 (백엔드 로그로 전달)
                const debugInfo = {
                    currentUrl: currentUrl,
                    totalATags: document.querySelectorAll('a').length,
                    totalAHrefTags: document.querySelectorAll('a[href]').length,
                    bodyHtml: document.body ? document.body.innerHTML.length : 0
                };
                
                // 1. 일반 <a href> 링크 (절대 URL로 변환)
                document.querySelectorAll('a[href]').forEach(a => {
                    try {
                        const hrefAttr = a.getAttribute('href');
                        const href = a.href || hrefAttr; // a.href는 브라우저가 자동으로 절대 URL로 변환
                        
                        if (href && typeof href === 'string' && 
                            !href.startsWith('javascript:') && 
                            !href.startsWith('#') && 
                            href !== '' &&
                            href !== window.location.href) {
                            
                            let url;
                            try {
                                // 브라우저의 a.href는 이미 절대 URL
                                url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                            } catch (e) {
                                console.warn('[LinkExtraction] Failed to parse href:', href, e);
                                return;
                            }
                            
                            // 자기 자신 페이지는 제외
                            if (url === currentUrl || url === currentUrl.split('#')[0]) {
                                return;
                            }
                            
                            const text = (a.innerText || a.textContent || '').trim().slice(0, 200);
                            const linkData = {
                                href: url,
                                text: text || url
                            };
                            links.add(JSON.stringify(linkData));
                        }
                    } catch (e) {
                        console.warn('[LinkExtraction] Error processing <a> tag:', e);
                    }
                });
                
                // 2. React Router Link 컴포넌트의 실제 <a> 태그 찾기
                // React Router v6+는 Link를 <a> 태그로 렌더링하므로 이미 1번에서 처리됨
                // 하지만 명시적으로 확인하기 위해 추가 검사
                
                // 3. data-* 속성에서 URL 추출
                document.querySelectorAll('[data-to], [to], [data-href], [data-path], [data-route], [data-url]').forEach(el => {
                    try {
                        const path = el.getAttribute('data-to') || el.getAttribute('to') || 
                                    el.getAttribute('data-href') || el.getAttribute('data-path') || 
                                    el.getAttribute('data-route') || el.getAttribute('data-url');
                        if (path && typeof path === 'string' && path !== '' && !path.startsWith('#')) {
                            let url;
                            try {
                                url = path.startsWith('http') ? path : new URL(path, baseUrl).href;
                            } catch (e) {
                                return;
                            }
                            if (url !== currentUrl) {
                                const text = (el.innerText || el.textContent || '').trim().slice(0, 200);
                                links.add(JSON.stringify({
                                    href: url,
                                    text: text || url
                                }));
                            }
                        }
                    } catch (e) {}
                });
                
                // 4. 버튼이나 클릭 가능한 요소에서 href 속성 찾기 (이미 <a>가 아닌 경우)
                document.querySelectorAll('button[href], [role="button"][href], [role="link"][href]').forEach(el => {
                    try {
                        const href = el.getAttribute('href');
                        if (href && typeof href === 'string' && 
                            !href.startsWith('javascript:') && 
                            !href.startsWith('#') && 
                            href !== '') {
                            let url;
                            try {
                                url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                            } catch (e) {
                                return;
                            }
                            if (url !== currentUrl) {
                                const text = (el.innerText || el.textContent || '').trim().slice(0, 200);
                                links.add(JSON.stringify({
                                    href: url,
                                    text: text || url
                                }));
                            }
                        }
                    } catch (e) {}
                });
                
                // 4. React Router Link 컴포넌트에서 실제 to 속성 추출
                // React Router v6+는 실제로 <a> 태그를 렌더링하지만, data 속성이나 특별한 클래스를 사용할 수 있음
                // 또는 실제 React Fiber 트리에서 Link 컴포넌트의 props를 찾기
                try {
                    // React DevTools가 설치되어 있으면 window.__REACT_DEVTOOLS_GLOBAL_HOOK__를 사용할 수 있지만,
                    // 일반적으로는 실제 렌더링된 DOM만 확인
                    
                    // React Router가 렌더링한 <a> 태그는 보통 href 속성을 가지고 있음
                    // 하지만 SPA이므로 실제 네비게이션은 클라이언트 사이드에서 처리
                    // 따라서 실제로 <a href>를 찾는 것이 가장 안전함
                } catch (e) {}
                
                // 5. MUI 및 Material-UI 컴포넌트에서 실제 <a> 태그 찾기
                // MUI ListItemButton은 보통 React Router Link로 감싸져 있어서 <a> 태그로 렌더링됨
                // 이미 1번에서 처리되지만, 명시적으로 확인
                document.querySelectorAll('[class*="MuiListItemButton"] a[href], [class*="MuiListItem-root"] a[href], [class*="MuiAccordionSummary"] a[href]').forEach(a => {
                    try {
                        const href = a.href || a.getAttribute('href');
                        if (href && typeof href === 'string' && 
                            !href.startsWith('javascript:') && 
                            !href.startsWith('#') && 
                            href !== '' &&
                            href !== currentUrl) {
                            let url;
                            try {
                                url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                            } catch (e) {
                                return;
                            }
                            const text = (a.innerText || a.textContent || '').trim().slice(0, 200);
                            links.add(JSON.stringify({
                                href: url,
                                text: text || url
                            }));
                        }
                    } catch (e) {}
                });
                
                // 6. 부모 체인에서 <a> 태그 찾기 (MUI 컴포넌트가 <a>로 감싸져 있는 경우)
                document.querySelectorAll('[class*="MuiListItemButton"], [class*="MuiListItem-root"], [class*="MuiButtonBase-root"]').forEach(el => {
                    try {
                        // 자신 또는 부모에서 <a> 태그 찾기
                        let target = el;
                        while (target && target !== document.body) {
                            if (target.tagName === 'A') {
                                const href = target.href || target.getAttribute('href');
                                if (href && typeof href === 'string' && 
                                    !href.startsWith('javascript:') && 
                                    !href.startsWith('#') && 
                                    href !== '' &&
                                    href !== currentUrl) {
                                    let url;
                                    try {
                                        url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                    } catch (e) {
                                        break;
                                    }
                                    const text = (target.innerText || target.textContent || el.innerText || el.textContent || '').trim().slice(0, 200);
                                    links.add(JSON.stringify({
                                        href: url,
                                        text: text || url
                                    }));
                                }
                                break;
                            }
                            target = target.parentElement;
                        }
                    } catch (e) {}
                });
                
                // 8. 모든 클릭 가능한 요소에서 data-* 속성으로 URL 추출 (실제 속성만)
                document.querySelectorAll('[data-path], [data-route], [data-page], [data-href], [data-url]').forEach(el => {
                    try {
                        const path = el.getAttribute('data-path') || el.getAttribute('data-route') || 
                                    el.getAttribute('data-page') || el.getAttribute('data-href') || 
                                    el.getAttribute('data-url');
                        if (path && path.startsWith('/') && path !== '/') {
                            const url = new URL(path, baseUrl).href;
                            links.add(JSON.stringify({
                                href: url,
                                text: (el.innerText || el.textContent || '').trim().slice(0, 200)
                            }));
                        }
                    } catch (e) {}
                });
                
                // 디버깅 정보를 반환에 포함
                const result = {
                    links: Array.from(links).map(s => JSON.parse(s)),
                    debug: debugInfo
                };
                return result;
            }
        """);
        
        // 결과가 Map인 경우 (디버깅 정보 포함) links 추출, 아니면 List로 처리
        List<?> list;
        Map<?, ?> debugInfo = null;
        if (raw instanceof Map<?, ?> map) {
            Object linksObj = map.get("links");
            Object debugObj = map.get("debug");
            if (linksObj instanceof List<?>) {
                list = (List<?>) linksObj;
                if (debugObj instanceof Map<?, ?>) {
                    debugInfo = (Map<?, ?>) debugObj;
                    System.out.printf("[Crawl] Browser: Link extraction debug - currentUrl: %s, totalATags: %s, totalAHrefTags: %s, bodyHtmlLength: %s%n",
                            debugInfo.get("currentUrl"),
                            debugInfo.get("totalATags"),
                            debugInfo.get("totalAHrefTags"),
                            debugInfo.get("bodyHtml"));
                }
            } else {
                System.out.println("[Crawl] Browser: Evaluation returned unexpected format");
                return Set.of();
            }
        } else if (raw instanceof List<?>) {
            list = (List<?>) raw;
        } else {
            System.out.println("[Crawl] Browser: No links found or evaluation failed");
            return Set.of();
        }
        
        Set<LinkOut> out = new HashSet<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Object hrefObj = m.get("href");
            if (hrefObj == null) continue;
            String href = String.valueOf(hrefObj);
            if (href.isBlank()) continue;
            
            // URL 정규화: fragment 제거
            try {
                java.net.URI uri = java.net.URI.create(href);
                if (uri.getFragment() != null) {
                    href = new java.net.URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), 
                            uri.getPath(), uri.getQuery(), null).toString();
                }
            } catch (Exception e) {
                // URL 파싱 실패 시 건너뛰기
                continue;
            }
            
            // http/https만 허용
            if (!(href.startsWith("http://") || href.startsWith("https://"))) {
                continue;
            }
            
            // 자기 자신 페이지는 제외 (순환 방지)
            String currentUrl = page.url();
            try {
                java.net.URI current = java.net.URI.create(currentUrl);
                java.net.URI link = java.net.URI.create(href);
                String currentPath = current.getPath() != null ? current.getPath() : "/";
                String linkPath = link.getPath() != null ? link.getPath() : "/";
                if (current.getHost() != null && current.getHost().equals(link.getHost()) && 
                    currentPath.equals(linkPath) && 
                    (current.getQuery() == null ? "" : current.getQuery()).equals(link.getQuery() == null ? "" : link.getQuery())) {
                    continue;
                }
            } catch (Exception e) {
                // 비교 실패해도 계속 진행
            }
            
            Object textObj = m.get("text");
            String text = textObj == null ? "" : String.valueOf(textObj);
            out.add(new LinkOut(href, text));
        }
        
        System.out.printf("[Crawl] Browser: Extracted %d links from page (after deduplication)%n", out.size());
        
        // extractActionsAndDiscoverLinks에서 이미 처리됨
        
        return out;
    }
    
    // Deprecated: extractActionsAndDiscoverLinks로 대체됨
    @Deprecated
    private static Set<LinkOut> extractLinksByClicking(Page page, List<Map<String, Object>> ctas) {
        Set<LinkOut> links = new HashSet<>();
        String originalUrl = page.url();
        
        // 클릭 전 현재 DOM의 링크 수집 (기준점)
        Set<String> existingLinks = new HashSet<>();
        try {
            Object existing = page.evaluate("""
                () => {
                    const links = new Set();
                    const baseUrl = window.location.origin;
                    document.querySelectorAll('a[href]').forEach(a => {
                        try {
                            const href = a.href || a.getAttribute('href');
                            if (href && typeof href === 'string' && 
                                !href.startsWith('javascript:') && 
                                !href.startsWith('#') && 
                                href !== '') {
                                const url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                links.add(url.split('#')[0]); // fragment 제거
                            }
                        } catch (e) {}
                    });
                    return Array.from(links);
                }
            """);
            if (existing instanceof List<?>) {
                for (Object url : (List<?>) existing) {
                    if (url instanceof String) {
                        existingLinks.add((String) url);
                    }
                }
            }
        } catch (Exception e) {
            // 무시
        }
        
        // 최대 10개까지만 클릭 (시간 제한)
        int maxClicks = Math.min(ctas.size(), 10);
        
        // 1단계: 모든 아코디언 버튼 클릭 (URL 변경 없는 경우, 상태 유지)
        for (int i = 0; i < maxClicks; i++) {
            Map<String, Object> cta = ctas.get(i);
            String selector = (String) cta.get("selector");
            String text = (String) cta.get("text");
            
            if (selector == null || selector.isBlank() || text == null || text.isBlank()) {
                continue;
            }
            
            try {
                // 텍스트로 정확한 요소 찾기 및 클릭
                Object clicked = page.evaluate("""
                    (text) => {
                        const buttons = document.querySelectorAll('button, [role="button"]');
                        for (const btn of buttons) {
                            const btnText = (btn.innerText || btn.textContent || '').trim();
                            if (btnText === text) {
                                btn.click();
                                return true;
                            }
                        }
                        return false;
                    }
                """, text);
                
                if (clicked instanceof Boolean && (Boolean) clicked) {
                    System.out.printf("[Crawl] Browser: Clicked button '%s' by text match%n", text);
                } else {
                    // selector로 시도
                    String selectorEscaped = selector.replace("\\", "\\\\").replace("\"", "\\\"");
                    String textEscaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
                    Object clickedBySelector = page.evaluate(String.format("""
                        () => {
                            const selector = "%s";
                            const text = "%s";
                            const elements = document.querySelectorAll(selector);
                            for (const el of elements) {
                                const elText = (el.innerText || el.textContent || '').trim();
                                if (elText === text) {
                                    el.click();
                                    return true;
                                }
                            }
                            return false;
                        }
                    """, selectorEscaped, textEscaped));
                    
                    if (clickedBySelector instanceof Boolean && (Boolean) clickedBySelector) {
                        System.out.printf("[Crawl] Browser: Clicked button '%s' by selector and text match%n", text);
                    } else {
                        System.out.printf("[Crawl] Browser: Failed to click button '%s', skipping%n", text);
                        continue;
                    }
                }
                
                // DOM 변경 완료 대기 (짧게)
                page.waitForTimeout(300);
            } catch (Exception e) {
                System.out.printf("[Crawl] Browser: Error clicking button '%s': %s%n", text, e.getMessage());
                // 계속 진행
            }
        }
        
        // 모든 버튼 클릭 완료 후 최종 대기
        page.waitForTimeout(1000);
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(2000));
        } catch (Exception e) {
            // 타임아웃되어도 계속
        }
        
        // 2단계: 아코디언이 모두 펼쳐진 상태에서 링크 추출
        try {
            Object allLinks = page.evaluate("""
                () => {
                    const links = [];
                    const baseUrl = window.location.origin;
                    const currentUrl = window.location.href.split('#')[0];
                    const foundUrls = new Set();
                    
                    // 모든 <a href> 링크 찾기
                    document.querySelectorAll('a[href]').forEach(a => {
                        try {
                            const href = a.href || a.getAttribute('href');
                            if (href && typeof href === 'string' && 
                                !href.startsWith('javascript:') && 
                                !href.startsWith('#') && 
                                href !== '') {
                                let url;
                                try {
                                    url = href.startsWith('http') ? href : new URL(href, baseUrl).href;
                                    url = url.split('#')[0]; // fragment 제거
                                } catch (e) {
                                    return;
                                }
                                
                                if (url !== currentUrl && 
                                    (url.startsWith('http://') || url.startsWith('https://')) &&
                                    !foundUrls.has(url)) {
                                    foundUrls.add(url);
                                    const linkText = (a.innerText || a.textContent || '').trim().slice(0, 100);
                                    links.push({
                                        url: url,
                                        text: linkText || url
                                    });
                                }
                            }
                        } catch (e) {}
                    });
                    
                    return links;
                }
            """);
            
            if (allLinks instanceof List<?>) {
                for (Object linkObj : (List<?>) allLinks) {
                    if (linkObj instanceof Map<?, ?>) {
                        Map<?, ?> linkMap = (Map<?, ?>) linkObj;
                        Object urlObj = linkMap.get("url");
                        Object textObj = linkMap.get("text");
                        if (urlObj instanceof String) {
                            String url = (String) urlObj;
                            String linkText = textObj instanceof String ? (String) textObj : url;
                            if (!existingLinks.contains(url) && !url.equals(originalUrl.split("#")[0])) {
                                links.add(new LinkOut(url, linkText));
                                System.out.printf("[Crawl] Browser: Discovered link from expanded accordion: %s (text: '%s')%n", url, linkText);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("[Crawl] Browser: Error extracting links from expanded accordions: %s%n", e.getMessage());
            e.printStackTrace();
        }
        
        return links;
    }

    private static Set<LinkOut> extractLinks(Document doc) {
        Set<LinkOut> out = new HashSet<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (href == null || href.isBlank()) continue;
            // filter obvious non-http(s)
            if (!(href.startsWith("http://") || href.startsWith("https://"))) continue;
            String text = a.text();
            if (text != null && text.length() > 200) text = text.substring(0, 200);
            out.add(new LinkOut(href, text));
        }
        System.out.printf("[Crawl] Jsoup: Extracted %d links from page%n", out.size());
        return out;
    }

    private static String normalize(String baseUrl, String href) {
        try {
            URI base = URI.create(baseUrl);
            URI resolved = base.resolve(href);
            // strip fragment
            URI noFrag = new URI(resolved.getScheme(), resolved.getUserInfo(), resolved.getHost(), resolved.getPort(), resolved.getPath(), resolved.getQuery(), null);
            return noFrag.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 네트워크 요청 리스트를 HAR 형식으로 변환
     */
    private Map<String, Object> createHarFromRequests(List<Map<String, Object>> requests) {
        Map<String, Object> har = new HashMap<>();
        Map<String, Object> log = new HashMap<>();
        
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<String, Object> req : requests) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("request", Map.of(
                    "method", req.getOrDefault("method", "GET"),
                    "url", req.getOrDefault("url", ""),
                    "headers", req.getOrDefault("headers", Map.of())
            ));
            entry.put("response", Map.of(
                    "status", 200, // 실제 응답 정보는 나중에 추가 가능
                    "headers", Map.of()
            ));
            entries.add(entry);
        }
        
        log.put("version", "1.2");
        log.put("entries", entries);
        har.put("log", log);
        return har;
    }

    private CrawlPageEntity getOrCreatePage(UUID runId, String url, int depth) {
        // defensive: use repository to avoid unique constraint violations
        return crawlPageRepository.findByRunIdAndUrl(runId, url).orElseGet(() -> {
            String nodeKey = Hashing.sha256Hex(url);
            String urlPattern = UrlPattern.normalizeToPattern(url);
            CrawlPageEntity created = new CrawlPageEntity(UUID.randomUUID(), crawlRunRepository.getReferenceById(runId), nodeKey, url, depth);
            created.setUrlPattern(urlPattern);
            try {
                return crawlPageRepository.save(created);
            } catch (Exception e) {
                return crawlPageRepository.findByRunIdAndUrl(runId, url).orElse(created);
            }
        });
    }

    private static void offer(
            CrawlStrategy strategy,
            String url,
            ArrayDeque<String> bfs,
            Set<String> enqueued,
            Map<String, Integer> mcsScore,
            PriorityQueue<FrontierItem> mcs,
            long[] seq
    ) {
        if (strategy == CrawlStrategy.BFS) {
            if (enqueued.add(url)) bfs.addLast(url);
            return;
        }
        // MCS: only add if not already enqueued
        if (enqueued.add(url)) {
            int score = mcsScore.getOrDefault(url, 0);
            mcs.add(new FrontierItem(url, score, seq[0]++));
        }
    }

    private static String poll(
            CrawlStrategy strategy,
            ArrayDeque<String> bfs,
            Map<String, Integer> mcsScore,
            PriorityQueue<FrontierItem> mcs,
            Set<String> visited
    ) {
        if (strategy == CrawlStrategy.BFS) {
            String url = bfs.pollFirst();
            return url;
        }

        // MCS: poll from priority queue
        while (true) {
            FrontierItem item = mcs.poll();
            if (item == null) return null;
            if (visited.contains(item.url())) continue;
            int current = mcsScore.getOrDefault(item.url(), 0);
            if (current != item.score()) continue; // stale
            return item.url();
        }
    }
}


