package com.dubbi.statetrail.graph.api;

import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.graph.api.dto.GraphDtos.GraphDTO;
import java.util.List;
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

    public GraphController(CrawlRunRepository crawlRunRepository) {
        this.crawlRunRepository = crawlRunRepository;
    }

    @GetMapping
    public ResponseEntity<GraphDTO> get(@PathVariable UUID runId) {
        if (crawlRunRepository.findById(runId).isEmpty()) return ResponseEntity.notFound().build();
        // Worker가 아직 없는 MVP 스캐폴딩 단계에서는 빈 그래프를 반환합니다.
        return ResponseEntity.ok(new GraphDTO(List.of(), List.of()));
    }
}


