package com.dubbi.statetrail.graph.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GraphDtos {
    private GraphDtos() {}

    public record GraphDTO(List<NodeDTO> nodes, List<EdgeDTO> edges) {}

    public record NodeDTO(
            UUID id,
            String nodeKey,
            String url,
            String title,
            String screenshotThumbUrl
    ) {}

    public record EdgeDTO(
            UUID id,
            UUID from,
            UUID to,
            String actionType,
            Map<String, Object> tags
    ) {}
}


