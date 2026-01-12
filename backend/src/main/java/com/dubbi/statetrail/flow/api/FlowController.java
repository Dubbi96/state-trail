package com.dubbi.statetrail.flow.api;

import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.flow.api.dto.FlowDtos.FlowDTO;
import com.dubbi.statetrail.flow.api.dto.FlowDtos.FlowStepDTO;
import com.dubbi.statetrail.flow.domain.FlowEntity;
import com.dubbi.statetrail.flow.domain.FlowRepository;
import com.dubbi.statetrail.flow.domain.FlowSource;
import com.dubbi.statetrail.flow.service.FlowMiner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlowController {
    private final CrawlRunRepository crawlRunRepository;
    private final FlowRepository flowRepository;
    private final FlowMiner flowMiner;

    public FlowController(
            CrawlRunRepository crawlRunRepository, 
            FlowRepository flowRepository,
            FlowMiner flowMiner
    ) {
        this.crawlRunRepository = crawlRunRepository;
        this.flowRepository = flowRepository;
        this.flowMiner = flowMiner;
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
        var flows = flowMiner.extractSmokeFlows(run, 10); // 최대 10개 smoke 플로우
        
        for (var flow : flows) {
            flowRepository.save(flow);
        }
        
        return ResponseEntity.ok(Map.of("ok", true, "count", flows.size()));
    }

    @GetMapping("/api/flows/{flowId}")
    public ResponseEntity<FlowDTO> get(@PathVariable UUID flowId) {
        return flowRepository.findById(flowId)
                .map(FlowController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/flows/{flowId}")
    public ResponseEntity<FlowDTO> update(
            @PathVariable UUID flowId,
            @RequestBody UpdateFlowRequest request
    ) {
        return flowRepository.findById(flowId)
                .map(flow -> {
                    if (request.name() != null) {
                        flow.setName(request.name());
                    }
                    if (request.steps() != null) {
                        List<Map<String, Object>> stepMaps = request.steps().stream()
                                .map(step -> Map.<String, Object>of("edgeId", step.edgeId().toString()))
                                .toList();
                        flow.setSteps(stepMaps);
                    }
                    flowRepository.save(flow);
                    return flow;
                })
                .map(FlowController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/flows/{flowId}")
    public ResponseEntity<Void> delete(@PathVariable UUID flowId) {
        if (flowRepository.existsById(flowId)) {
            flowRepository.deleteById(flowId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
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

    record UpdateFlowRequest(String name, List<FlowStepDTO> steps) {}

    private static FlowDTO toDto(FlowEntity e) {
        // steps에서 edgeId 추출
        List<FlowStepDTO> stepDtos = e.getSteps().stream()
                .map(step -> {
                    Object edgeIdObj = step.get("edgeId");
                    if (edgeIdObj instanceof String edgeIdStr) {
                        try {
                            return new FlowStepDTO(UUID.fromString(edgeIdStr));
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    }
                    return null;
                })
                .filter(step -> step != null)
                .toList();
        
        return new FlowDTO(e.getId(), e.getName(), stepDtos);
    }
}
