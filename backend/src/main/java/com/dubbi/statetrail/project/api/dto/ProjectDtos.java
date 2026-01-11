package com.dubbi.statetrail.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectDTO(
            UUID id,
            String name,
            String baseUrl,
            Map<String, Object> allowlistRules
    ) {}

    public record CreateProjectRequest(
            @NotBlank String name,
            @NotBlank String baseUrl,
            @NotNull Map<String, Object> allowlistRules
    ) {}

    public record UpdateProjectRequest(
            @NotBlank String name,
            @NotBlank String baseUrl,
            @NotNull Map<String, Object> allowlistRules
    ) {}
}


