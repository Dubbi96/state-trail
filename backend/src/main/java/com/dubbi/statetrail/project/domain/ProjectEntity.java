package com.dubbi.statetrail.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowlist_rules", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> allowlistRules;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectEntity() {}

    public ProjectEntity(UUID id, String name, String baseUrl, Map<String, Object> allowlistRules) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.allowlistRules = allowlistRules;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Map<String, Object> getAllowlistRules() {
        return allowlistRules;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String baseUrl, Map<String, Object> allowlistRules) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.allowlistRules = allowlistRules;
        this.updatedAt = Instant.now();
    }
}


