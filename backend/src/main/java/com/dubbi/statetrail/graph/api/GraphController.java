package com.dubbi.statetrail.graph.api;

import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.GraphDTO;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.NodeDTO;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.EdgeDTO;
import com.dubbi.statetrail.graph.util.UiSignatureSummary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl-runs/{runId}/graph")
public class GraphController {
    private final CrawlRunRepository crawlRunRepository;
    private final CrawlPageRepository crawlPageRepository;
    private final CrawlLinkRepository crawlLinkRepository;
    private final ObjectStorageService objectStorageService;

    public GraphController(
            CrawlRunRepository crawlRunRepository, 
            CrawlPageRepository crawlPageRepository, 
            CrawlLinkRepository crawlLinkRepository,
            ObjectStorageService objectStorageService
    ) {
        this.crawlRunRepository = crawlRunRepository;
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
        this.objectStorageService = objectStorageService;
    }

    @GetMapping
    public ResponseEntity<GraphDTO> get(@PathVariable UUID runId) {
        if (crawlRunRepository.findById(runId).isEmpty()) return ResponseEntity.notFound().build();
        
        var nodes = crawlPageRepository.findByRunId(runId).stream()
                .map(p -> {
                    // 스크린샷 썸네일 presigned URL 생성
                    String screenshotThumbUrl = null;
                    if (p.getScreenshotObjectKey() != null) {
                        try {
                            screenshotThumbUrl = objectStorageService.getPresignedUrl(p.getScreenshotObjectKey());
                        } catch (Exception e) {
                            // ignore presigned URL generation errors
                        }
                    }
                    
                    // UI 시그니처 요약
                    Map<String, Object> uiSignatureSummary = UiSignatureSummary.summarize(p.getUiSignature());
                    
                    // 리스크 태그
                    Map<String, Object> riskTags = UiSignatureSummary.extractRiskTags(p.getUiSignature());
                    
                    // 메타데이터
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("httpStatus", p.getHttpStatus());
                    metadata.put("contentType", p.getContentType());
                    if (p.getNetworkLogObjectKey() != null) {
                        metadata.put("hasNetworkLog", true);
                    }
                    
                    return new NodeDTO(
                            p.getId(),
                            p.getNodeKey(),
                            p.getUrl(),
                            p.getUrlPattern() != null ? p.getUrlPattern() : p.getUrl(),
                            p.getTitle(),
                            screenshotThumbUrl,
                            p.getDepth(),
                            uiSignatureSummary,
                            riskTags,
                            metadata
                    );
                })
                .toList();
        
        var edges = crawlLinkRepository.findByRunId(runId).stream()
                .map(e -> {
                    // ActionType이 null이면 기본값 "NAVIGATE"
                    String actionType = e.getActionType() != null ? e.getActionType().name() : "NAVIGATE";
                    
                    // Edge 태그 (anchorText 포함)
                    Map<String, Object> tags = new HashMap<>();
                    if (e.getAnchorText() != null) {
                        tags.put("anchorText", e.getAnchorText());
                    }
                    
                    return new EdgeDTO(
                            e.getId(),
                            e.getFromPage().getId(),
                            e.getToPage().getId(),
                            actionType,
                            e.getLocator(),
                            e.getRiskTags() != null ? e.getRiskTags() : Map.of(),
                            tags
                    );
                })
                .toList();
        
        return ResponseEntity.ok(new GraphDTO(nodes, edges));
    }
}


