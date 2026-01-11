package com.dubbi.statetrail.crawl.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.dubbi.statetrail.common.util.Hashing;
import com.dubbi.statetrail.crawl.domain.CrawlLinkEntity;
import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageEntity;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.crawl.web.AllowlistRules;
import com.dubbi.statetrail.crawl.web.CrawlBudget;
import com.dubbi.statetrail.crawl.web.CrawlStrategy;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    public WebCrawlerService(
            CrawlRunRepository crawlRunRepository,
            CrawlPageRepository crawlPageRepository,
            CrawlLinkRepository crawlLinkRepository,
            CrawlRunEventHub eventHub
    ) {
        this.crawlRunRepository = crawlRunRepository;
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
        this.eventHub = eventHub;
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
            eventHub.publish(runId, "NODE", Map.of("id", startPage.getId(), "url", run.getStartUrl(), "depth", 0));
            offer(ordering, run.getStartUrl(), bfs, enqueued, mcsScore, mcs, seq);

            Playwright playwright = null;
            Browser browser = null;
            BrowserContext context = null;
            Page page = null;
            try {
                if (browserMode) {
                    playwright = Playwright.create();
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                    context = browser.newContext();
                    page = context.newPage();
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
                            eventHub.publish(runId, "NODE", Map.of("id", toPage.getId(), "url", toUrl, "depth", toDepth));
                        }

                        String edgeKey = current.getId() + "->" + toPage.getId();
                        if (edgeSeen.add(edgeKey)) {
                            try {
                                crawlLinkRepository.save(new CrawlLinkEntity(UUID.randomUUID(), run, current, toPage, link.anchorText()));
                                edges++;
                                eventHub.publish(runId, "EDGE", Map.of("from", current.getId(), "to", toPage.getId()));
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
    private record PageFetchResult(Integer status, String contentType, String title, String htmlSnapshot, Set<LinkOut> links) {}

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
        return new PageFetchResult(status, contentType, title, snapshot, links);
    }

    private PageFetchResult fetchWithBrowser(Page page, String url) {
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

        Set<LinkOut> links = extractLinksFromBrowser(page);
        String snapshot = html == null ? null : (html.length() > 200_000 ? html.substring(0, 200_000) : html);
        return new PageFetchResult(status, contentType, title, snapshot, links);
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

    private CrawlPageEntity getOrCreatePage(UUID runId, String url, int depth) {
        // defensive: use repository to avoid unique constraint violations
        return crawlPageRepository.findByRunIdAndUrl(runId, url).orElseGet(() -> {
            String nodeKey = Hashing.sha256Hex(url);
            CrawlPageEntity created = new CrawlPageEntity(UUID.randomUUID(), crawlRunRepository.getReferenceById(runId), nodeKey, url, depth);
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


