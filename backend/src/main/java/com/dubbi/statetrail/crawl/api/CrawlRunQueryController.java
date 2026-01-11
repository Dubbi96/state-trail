package com.dubbi.statetrail.crawl.api;

import com.dubbi.statetrail.crawl.api.dto.CrawlRunDtos.CrawlRunDTO;
import com.dubbi.statetrail.crawl.domain.CrawlRunEntity;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl-runs")
public class CrawlRunQueryController {
    private final CrawlRunRepository crawlRunRepository;

    public CrawlRunQueryController(CrawlRunRepository crawlRunRepository) {
        this.crawlRunRepository = crawlRunRepository;
    }

    @GetMapping("/{runId}")
    public ResponseEntity<CrawlRunDTO> get(@PathVariable UUID runId) {
        return crawlRunRepository.findById(runId)
                .map(CrawlRunQueryController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static CrawlRunDTO toDto(CrawlRunEntity e) {
        return new CrawlRunDTO(
                e.getId(),
                e.getProject().getId(),
                e.getAuthProfile().getId(),
                e.getStatus(),
                e.getStartUrl(),
                e.getBudget(),
                e.getStrategy(),
                e.getStats(),
                e.getErrorMessage()
        );
    }
}


