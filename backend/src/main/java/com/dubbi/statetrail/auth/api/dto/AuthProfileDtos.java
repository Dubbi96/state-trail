package com.dubbi.statetrail.auth.api.dto;

import com.dubbi.statetrail.auth.domain.AuthProfileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public final class AuthProfileDtos {
    private AuthProfileDtos() {}

    public record AuthProfileDTO(
            UUID id,
            UUID projectId,
            String name,
            AuthProfileType type,
            Map<String, Object> tags,
            String storageStateObjectKey
    ) {}

    public record CreateAuthProfileRequest(
            @NotBlank String name,
            @NotNull AuthProfileType type,
            @NotNull Map<String, Object> tags
    ) {}
}


