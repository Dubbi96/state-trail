package com.dubbi.statetrail.graph.api;

import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.graph.api.dto.GraphInspectorDtos.EdgeDetailDTO;
import com.dubbi.statetrail.graph.api.dto.GraphInspectorDtos.NodeDetailDTO;
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

    public GraphInspectorController(CrawlPageRepository crawlPageRepository, CrawlLinkRepository crawlLinkRepository) {
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<NodeDetailDTO> getNode(@PathVariable UUID runId, @PathVariable UUID nodeId) {
        return crawlPageRepository.findById(nodeId)
                .filter(p -> p.getCrawlRun().getId().equals(runId))
                .map(p -> {
                    String html = p.getHtmlSnapshot();
                    Integer size = html == null ? null : html.length();
                    String snippet = html == null ? null : (html.length() > 8000 ? html.substring(0, 8000) : html);
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
                            snippet
                    );
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/edges/{edgeId}")
    public ResponseEntity<EdgeDetailDTO> getEdge(@PathVariable UUID runId, @PathVariable UUID edgeId) {
        return crawlLinkRepository.findById(edgeId)
                .filter(e -> e.getCrawlRun().getId().equals(runId))
                .map(e -> new EdgeDetailDTO(
                        e.getId(),
                        e.getFromPage().getId(),
                        e.getToPage().getId(),
                        "LINK",
                        e.getAnchorText()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}


