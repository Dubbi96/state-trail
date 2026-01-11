package com.dubbi.statetrail.graph.api;

import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.crawl.domain.CrawlLinkRepository;
import com.dubbi.statetrail.crawl.domain.CrawlPageRepository;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.GraphDTO;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.NodeDTO;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.EdgeDTO;
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

    public GraphController(CrawlRunRepository crawlRunRepository, CrawlPageRepository crawlPageRepository, CrawlLinkRepository crawlLinkRepository) {
        this.crawlRunRepository = crawlRunRepository;
        this.crawlPageRepository = crawlPageRepository;
        this.crawlLinkRepository = crawlLinkRepository;
    }

    @GetMapping
    public ResponseEntity<GraphDTO> get(@PathVariable UUID runId) {
        if (crawlRunRepository.findById(runId).isEmpty()) return ResponseEntity.notFound().build();
        var nodes = crawlPageRepository.findByRunId(runId).stream()
                .map(p -> new NodeDTO(
                        p.getId(),
                        p.getNodeKey(),
                        p.getUrl(),
                        p.getUrlPattern() != null ? p.getUrlPattern() : p.getUrl(),
                        p.getTitle(),
                        null,
                        p.getDepth()
                ))
                .toList();
        var edges = crawlLinkRepository.findByRunId(runId).stream()
                .map(e -> new EdgeDTO(
                        e.getId(),
                        e.getFromPage().getId(),
                        e.getToPage().getId(),
                        "LINK",
                        Map.of("anchorText", e.getAnchorText())
                ))
                .toList();
        return ResponseEntity.ok(new GraphDTO(nodes, edges));
    }
}


