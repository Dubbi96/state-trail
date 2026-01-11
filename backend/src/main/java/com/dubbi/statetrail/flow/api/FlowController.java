package com.dubbi.statetrail.flow.api;

import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.flow.api.dto.FlowDtos.FlowDTO;
import com.dubbi.statetrail.flow.api.dto.FlowDtos.FlowStepDTO;
import com.dubbi.statetrail.flow.domain.FlowEntity;
import com.dubbi.statetrail.flow.domain.FlowRepository;
import com.dubbi.statetrail.flow.domain.FlowSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlowController {
    private final CrawlRunRepository crawlRunRepository;
    private final FlowRepository flowRepository;

    public FlowController(CrawlRunRepository crawlRunRepository, FlowRepository flowRepository) {
        this.crawlRunRepository = crawlRunRepository;
        this.flowRepository = flowRepository;
    }

    @GetMapping("/api/crawl-runs/{runId}/flows")
    public ResponseEntity<ListResponse<FlowDTO>> listByRun(@PathVariable UUID runId) {
        if (crawlRunRepository.findById(runId).isEmpty()) return ResponseEntity.notFound().build();
        var flows = flowRepository.findByRunId(runId).stream().map(FlowController::toDto).toList();
        return ResponseEntity.ok(ListResponse.of(flows));
    }

    @PostMapping("/api/crawl-runs/{runId}/flows/auto-smoke")
    public ResponseEntity<Map<String, Object>> generateAutoSmoke(@PathVariable UUID runId) {
        var runOpt = crawlRunRepository.findById(runId);
        if (runOpt.isEmpty()) return ResponseEntity.notFound().build();

        var run = runOpt.get();
        // 아직 graph miner가 없으므로, “추천 플로우 3개” 더미를 생성합니다.
        for (int i = 1; i <= 3; i++) {
            var flow = new FlowEntity(
                    UUID.randomUUID(),
                    run.getProject(),
                    run.getAuthProfile(),
                    run,
                    "Auto Smoke #" + i,
                    FlowSource.AUTO_SMOKE,
                    List.of(),
                    Map.of("suite", "smoke")
            );
            flowRepository.save(flow);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/flows/{flowId}/generate-test")
    public ResponseEntity<Map<String, Object>> generateTest(@PathVariable UUID flowId) {
        var flowOpt = flowRepository.findById(flowId);
        if (flowOpt.isEmpty()) return ResponseEntity.notFound().build();
        var flow = flowOpt.get();

        var code = """
                import { test, expect } from '@playwright/test';
                
                test('%s', async ({ page }) => {
                  // TODO: worker/템플릿 기반 결정론적 생성으로 대체
                  await page.goto('%s');
                  await expect(page).toHaveURL(/.*/);
                });
                """.formatted(flow.getName(), flow.getProject().getBaseUrl());

        return ResponseEntity.ok(Map.of("code", code));
    }

    private static FlowDTO toDto(FlowEntity e) {
        // steps는 아직 edgeId 기반으로 저장하지 않으므로 빈 배열로 반환
        return new FlowDTO(e.getId(), e.getName(), List.<FlowStepDTO>of());
    }
}


