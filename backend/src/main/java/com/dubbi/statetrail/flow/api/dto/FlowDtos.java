package com.dubbi.statetrail.flow.api.dto;

import java.util.List;
import java.util.UUID;

public final class FlowDtos {
    private FlowDtos() {}

    public record FlowStepDTO(UUID edgeId) {}

    public record FlowDTO(
            UUID id,
            String name,
            List<FlowStepDTO> steps
    ) {}
}


