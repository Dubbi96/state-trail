package com.dubbi.statetrail.auth.domain;

import com.dubbi.statetrail.project.domain.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auth_profiles")
public class AuthProfileEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProfileType type;

    @Column(name = "storage_state_object_key")
    private String storageStateObjectKey;

    @Column(name = "login_script", columnDefinition = "text")
    private String loginScript;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuthProfileEntity() {}

    public AuthProfileEntity(UUID id, ProjectEntity project, String name, AuthProfileType type, Map<String, Object> tags) {
        this.id = id;
        this.project = project;
        this.name = name;
        this.type = type;
        this.tags = tags;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public AuthProfileType getType() {
        return type;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public String getStorageStateObjectKey() {
        return storageStateObjectKey;
    }

    public String getLoginScript() {
        return loginScript;
    }

    public void setStorageStateObjectKey(String storageStateObjectKey) {
        this.storageStateObjectKey = storageStateObjectKey;
        this.updatedAt = Instant.now();
    }

    public void setLoginScript(String loginScript) {
        this.loginScript = loginScript;
        this.updatedAt = Instant.now();
    }
}


