package com.dubbi.statetrail.crawl.api.dto;

import com.dubbi.statetrail.crawl.domain.CrawlRunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public final class CrawlRunDtos {
    private CrawlRunDtos() {}

    public record CrawlRunDTO(
            UUID id,
            UUID projectId,
            UUID authProfileId,
            CrawlRunStatus status,
            String startUrl,
            Map<String, Object> budget,
            String strategy,
            Map<String, Object> stats,
            String errorMessage
    ) {}

    public record CreateCrawlRunRequest(
            @NotNull UUID authProfileId,
            @NotBlank String startUrl,
            @NotNull Map<String, Object> budget,
            String strategy
    ) {}
}


