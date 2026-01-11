package com.dubbi.statetrail.crawl.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CrawlRunEventHub {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByRunId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID runId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByRunId.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(runId, emitter));
        emitter.onTimeout(() -> remove(runId, emitter));
        emitter.onError((e) -> remove(runId, emitter));

        // initial ping
        publish(runId, "PING", Map.of("ts", Instant.now().toString()));
        return emitter;
    }

    public void publish(UUID runId, String type, Object payload) {
        List<SseEmitter> emitters = emittersByRunId.get(runId);
        if (emitters == null) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type).data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                remove(runId, emitter);
            }
        }
    }

    private void remove(UUID runId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByRunId.get(runId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) emittersByRunId.remove(runId);
    }
}


