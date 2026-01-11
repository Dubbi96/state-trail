package com.dubbi.statetrail.crawl.api;

import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.crawl.service.CrawlRunEventHub;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/crawl-runs/{runId}/events")
public class CrawlRunEventsController {
    private final CrawlRunRepository crawlRunRepository;
    private final CrawlRunEventHub eventHub;

    public CrawlRunEventsController(CrawlRunRepository crawlRunRepository, CrawlRunEventHub eventHub) {
        this.crawlRunRepository = crawlRunRepository;
        this.eventHub = eventHub;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@PathVariable UUID runId) {
        if (crawlRunRepository.findById(runId).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(eventHub.subscribe(runId));
    }
}


