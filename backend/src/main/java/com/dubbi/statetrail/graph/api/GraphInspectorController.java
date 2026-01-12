package com.dubbi.statetrail.graph.api;

import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.crawl.web.ActionType;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.dubbi.statetrail.graph.api.dto.GraphInspectorDtos.EdgeDetailDTO;
import com.dubbi.statetrail.graph.api.dto.GraphInspectorDtos.NodeDetailDTO;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl-runs/{runId}")
public class GraphInspectorController {
    private final CrawlPageRepository crawlPageRepository;
    private final CrawlLinkRepository crawlLinkRepository;
    private final ObjectStorageService objectStorageService;

    public GraphInspectorController(
            CrawlPageRepository crawlPageRepository, 
            CrawlLinkRepository crawlLinkRepository,
            ObjectStorageService objectStorageService
    ) {
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
        this.objectStorageService = objectStorageService;
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<NodeDetailDTO> getNode(@PathVariable UUID runId, @PathVariable UUID nodeId) {
        return crawlPageRepository.findById(nodeId)
                .filter(p -> p.getCrawlRun().getId().equals(runId))
                .map(p -> {
                    String html = p.getHtmlSnapshot();
                    Integer size = html == null ? null : html.length();
                    String snippet = html == null ? null : (html.length() > 8000 ? html.substring(0, 8000) : html);
                    
                    // 스크린샷 원본 presigned URL
                    String screenshotUrl = null;
                    if (p.getScreenshotObjectKey() != null) {
                        try {
                            screenshotUrl = objectStorageService.getPresignedUrl(p.getScreenshotObjectKey());
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    
                    // 네트워크 로그 presigned URL
                    String networkLogUrl = null;
                    if (p.getNetworkLogObjectKey() != null) {
                        try {
                            networkLogUrl = objectStorageService.getPresignedUrl(p.getNetworkLogObjectKey());
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    
                    return new NodeDetailDTO(
                            p.getId(),
                            p.getNodeKey(),
                            p.getUrl(),
                            p.getTitle(),
                            p.getHttpStatus(),
                            p.getContentType(),
                            p.getDepth(),
                            p.getDiscoveredAt(),
                            p.getFetchedAt(),
                            size,
                            snippet,
                            screenshotUrl,
                            networkLogUrl,
                            p.getUiSignature() != null ? p.getUiSignature() : Map.of()
                    );
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.<NodeDetailDTO>notFound().build());
    }

    @GetMapping("/edges/{edgeId}")
    public ResponseEntity<EdgeDetailDTO> getEdge(@PathVariable UUID runId, @PathVariable UUID edgeId) {
        return crawlLinkRepository.findById(edgeId)
                .filter(e -> e.getCrawlRun().getId().equals(runId))
                .map(e -> {
                    String actionType = e.getActionType() != null ? e.getActionType().name() : "NAVIGATE";
                    return new EdgeDetailDTO(
                            e.getId(),
                            e.getFromPage().getId(),
                            e.getToPage().getId(),
                            actionType,
                            e.getLocator(),
                            e.getAnchorText(),
                            e.getPayload() != null ? e.getPayload() : Map.of(),
                            e.getRiskTags() != null ? e.getRiskTags() : Map.of(),
                            e.getHttpEvidence() != null ? e.getHttpEvidence() : Map.of()
                    );
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.<EdgeDetailDTO>notFound().build());
    }
}


