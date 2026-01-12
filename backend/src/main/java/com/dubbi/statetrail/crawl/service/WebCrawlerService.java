package com.dubbi.statetrail.crawl.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
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
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                    
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
                    page = context.newPage();
                    
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
        // hydration / client fetch time
        page.waitForTimeout(700);

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

        Set<LinkOut> links = extractLinksFromBrowser(page);
        String snapshot = html == null ? null : (html.length() > 200_000 ? html.substring(0, 200_000) : html);
        return new PageFetchResult(status, contentType, title, snapshot, links, uiSignature, networkRequests);
    }

    private static Set<LinkOut> extractLinksFromBrowser(Page page) {
        // Extract all links including relative URLs - browser will resolve them to absolute URLs
        Object raw = page.evaluate("() => Array.from(document.querySelectorAll('a[href]')).map(a => ({ href: a.href, text: (a.innerText || a.textContent || '').trim().slice(0, 200) }))");
        if (!(raw instanceof List<?> list)) {
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
            // Browser's a.href is always absolute, so we can accept all http/https URLs
            if (!(href.startsWith("http://") || href.startsWith("https://"))) {
                continue;
            }
            Object textObj = m.get("text");
            String text = textObj == null ? "" : String.valueOf(textObj);
            out.add(new LinkOut(href, text));
        }
        System.out.printf("[Crawl] Browser: Extracted %d links from page%n", out.size());
        return out;
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


