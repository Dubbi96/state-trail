package com.dubbi.statetrail.graph.api.dto;

import java.time.Instant;
import java.util.UUID;

public final class GraphInspectorDtos {
    private GraphInspectorDtos() {}

    public record NodeDetailDTO(
            UUID id,
            String nodeKey,
            String url,
            String title,
            Integer httpStatus,
            String contentType,
            int depth,
            Instant discoveredAt,
            Instant fetchedAt,
            Integer htmlSize,
            String htmlSnippet
    ) {}

    public record EdgeDetailDTO(
            UUID id,
            UUID from,
            UUID to,
            String actionType,
            String anchorText
    ) {}
}


