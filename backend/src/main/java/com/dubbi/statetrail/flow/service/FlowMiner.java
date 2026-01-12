package com.dubbi.statetrail.flow.service;

import com.dubbi.statetrail.crawl.domain.CrawlLinkEntity;
import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageEntity;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.crawl.domain.CrawlRunEntity;
import com.dubbi.statetrail.flow.domain.FlowEntity;
import com.dubbi.statetrail.flow.domain.FlowSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 그래프에서 플로우를 자동 추출하는 서비스
 */
@Service
public class FlowMiner {
    private final CrawlPageRepository crawlPageRepository;
    private final CrawlLinkRepository crawlLinkRepository;

    public FlowMiner(CrawlPageRepository crawlPageRepository, CrawlLinkRepository crawlLinkRepository) {
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
    }

    /**
     * 최단경로 기반 smoke 플로우 추출
     * startUrl에서 다른 주요 노드들까지의 최단경로를 찾아서 플로우로 생성
     */
    public List<FlowEntity> extractSmokeFlows(CrawlRunEntity run, int maxFlows) {
        var pages = crawlPageRepository.findByRunId(run.getId());
        var links = crawlLinkRepository.findByRunId(run.getId());
        
        if (pages.isEmpty() || links.isEmpty()) {
            return List.of();
        }
        
        // 그래프 구조 생성: pageId -> List<edgeId>
        Map<UUID, List<CrawlLinkEntity>> graph = buildGraph(links);
        
        // startUrl에 해당하는 노드 찾기
        var startPageOpt = pages.stream()
                .filter(p -> p.getUrl().equals(run.getStartUrl()))
                .findFirst();
        if (startPageOpt.isEmpty()) {
            return List.of();
        }
        var startPage = startPageOpt.get();
        
        // BFS로 최단경로 찾기 (edgeId 경로 저장)
        Map<UUID, List<UUID>> shortestPaths = findShortestPaths(graph, startPage.getId());
        
        // 주요 노드들 선택 (depth가 깊은 노드들 우선, 또는 특정 조건 만족하는 노드들)
        List<CrawlPageEntity> targetPages = selectTargetPages(pages, startPage, maxFlows);
        
        // 각 타겟까지의 경로로 플로우 생성
        List<FlowEntity> flows = new ArrayList<>();
        for (int i = 0; i < Math.min(targetPages.size(), maxFlows); i++) {
            var target = targetPages.get(i);
            List<UUID> path = shortestPaths.get(target.getId());
            
            if (path != null && !path.isEmpty()) {
                // 경로를 steps로 변환 (edgeId 리스트)
                List<Map<String, Object>> steps = path.stream()
                        .map(edgeId -> Map.<String, Object>of("edgeId", edgeId.toString()))
                        .toList();
                
                var flow = new FlowEntity(
                        UUID.randomUUID(),
                        run.getProject(),
                        run.getAuthProfile(),
                        run,
                        String.format("Smoke: %s → %s", 
                                getPageLabel(startPage), 
                                getPageLabel(target)),
                        FlowSource.AUTO_SMOKE,
                        steps,
                        Map.of("suite", "smoke", "from", startPage.getId().toString(), "to", target.getId().toString())
                );
                flows.add(flow);
            }
        }
        
        return flows;
    }

    /**
     * 엣지 커버리지 플로우 추출
     * 모든 엣지를 최소한 한 번씩 커버하는 플로우 집합 생성 (Chinese Postman Problem의 변형)
     */
    public List<FlowEntity> extractEdgeCoverageFlows(CrawlRunEntity run) {
        var pages = crawlPageRepository.findByRunId(run.getId());
        var links = crawlLinkRepository.findByRunId(run.getId());
        
        if (pages.isEmpty() || links.isEmpty()) {
            return List.of();
        }
        
        // 그래프 구조 생성
        Map<UUID, List<CrawlLinkEntity>> graph = buildGraph(links);
        
        // startUrl 노드 찾기
        var startPageOpt = pages.stream()
                .filter(p -> p.getUrl().equals(run.getStartUrl()))
                .findFirst();
        if (startPageOpt.isEmpty()) {
            return List.of();
        }
        var startPage = startPageOpt.get();
        
        // 간단한 휴리스틱: 각 엣지를 순회하면서 경로 생성
        Set<UUID> coveredEdges = new HashSet<>();
        List<FlowEntity> flows = new ArrayList<>();
        List<UUID> currentPath = new ArrayList<>();
        UUID currentNode = startPage.getId();
        
        // DFS 기반으로 엣지 커버리지 추적
        while (coveredEdges.size() < links.size() && flows.size() < 50) { // 최대 50개 플로우
            List<CrawlLinkEntity> outgoing = graph.getOrDefault(currentNode, List.of());
            
            // 아직 커버되지 않은 엣지 찾기
            var uncoveredEdge = outgoing.stream()
                    .filter(e -> !coveredEdges.contains(e.getId()))
                    .findFirst();
            
            if (uncoveredEdge.isPresent()) {
                var edge = uncoveredEdge.get();
                currentPath.add(edge.getId());
                coveredEdges.add(edge.getId());
                currentNode = edge.getToPage().getId();
            } else {
                // 더 이상 갈 곳이 없으면 플로우 생성
                if (!currentPath.isEmpty()) {
                    List<Map<String, Object>> steps = currentPath.stream()
                            .map(edgeId -> Map.<String, Object>of("edgeId", edgeId.toString()))
                            .toList();
                    
                    var flow = new FlowEntity(
                            UUID.randomUUID(),
                            run.getProject(),
                            run.getAuthProfile(),
                            run,
                            String.format("Edge Coverage #%d", flows.size() + 1),
                            FlowSource.AUTO_EDGE_COVERAGE,
                            steps,
                            Map.of("suite", "coverage", "edgeCount", currentPath.size())
                    );
                    flows.add(flow);
                    currentPath.clear();
                    currentNode = startPage.getId();
                } else {
                    break;
                }
            }
        }
        
        // 마지막 플로우 처리
        if (!currentPath.isEmpty()) {
            List<Map<String, Object>> steps = currentPath.stream()
                    .map(edgeId -> Map.<String, Object>of("edgeId", edgeId.toString()))
                    .toList();
            
            var flow = new FlowEntity(
                    UUID.randomUUID(),
                    run.getProject(),
                    run.getAuthProfile(),
                    run,
                    String.format("Edge Coverage #%d", flows.size() + 1),
                    FlowSource.AUTO_EDGE_COVERAGE,
                    steps,
                    Map.of("suite", "coverage", "edgeCount", currentPath.size())
            );
            flows.add(flow);
        }
        
        return flows;
    }

    /**
     * 그래프 구조 생성: pageId -> outgoing edges
     */
    private Map<UUID, List<CrawlLinkEntity>> buildGraph(List<CrawlLinkEntity> links) {
        Map<UUID, List<CrawlLinkEntity>> graph = new HashMap<>();
        for (var link : links) {
            graph.computeIfAbsent(link.getFromPage().getId(), k -> new ArrayList<>()).add(link);
        }
        return graph;
    }

    /**
     * BFS로 최단경로 찾기 (edgeId 경로 반환)
     */
    private Map<UUID, List<UUID>> findShortestPaths(Map<UUID, List<CrawlLinkEntity>> graph, UUID start) {
        Map<UUID, List<UUID>> paths = new HashMap<>();
        Map<UUID, UUID> parentEdge = new HashMap<>(); // node -> edge that reached it
        Queue<UUID> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();
        
        queue.offer(start);
        visited.add(start);
        paths.put(start, List.of()); // start node has empty path
        
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            
            for (var edge : graph.getOrDefault(current, List.of())) {
                UUID next = edge.getToPage().getId();
                
                if (!visited.contains(next)) {
                    visited.add(next);
                    parentEdge.put(next, edge.getId());
                    queue.offer(next);
                    
                    // 경로 재구성
                    List<UUID> path = new ArrayList<>(paths.get(current));
                    path.add(edge.getId());
                    paths.put(next, path);
                }
            }
        }
        
        return paths;
    }

    /**
     * 타겟 노드 선택 (depth가 깊은 노드들 우선)
     */
    private List<CrawlPageEntity> selectTargetPages(
            List<CrawlPageEntity> pages, 
            CrawlPageEntity startPage, 
            int maxCount
    ) {
        return pages.stream()
                .filter(p -> !p.getId().equals(startPage.getId())) // start 제외
                .sorted((a, b) -> Integer.compare(b.getDepth(), a.getDepth())) // depth 큰 순
                .limit(maxCount)
                .toList();
    }

    private String getPageLabel(CrawlPageEntity page) {
        if (page.getTitle() != null && !page.getTitle().isBlank()) {
            return page.getTitle();
        }
        try {
            var url = new java.net.URL(page.getUrl());
            var path = url.getPath();
            if (path.isEmpty() || path.equals("/")) {
                return url.getHost();
            }
            var segments = path.split("/");
            return segments[segments.length - 1];
        } catch (Exception e) {
            return page.getUrl();
        }
    }
}

