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
            String urlPattern,
            String title,
            String screenshotThumbUrl,
            int depth,
            Map<String, Object> uiSignatureSummary,
            Map<String, Object> riskTags,
            Map<String, Object> metadata
    ) {}

    public record EdgeDTO(
            UUID id,
            UUID from,
            UUID to,
            String actionType,
            String locator,
            Map<String, Object> riskTags,
            Map<String, Object> tags
    ) {}
}


