package com.dubbi.statetrail.flow.domain;

import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
import com.dubbi.statetrail.crawl.domain.CrawlRunEntity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flows")
public class FlowEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_profile_id", nullable = false)
    private AuthProfileEntity authProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_run_id", nullable = false)
    private CrawlRunEntity crawlRun;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowSource source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> steps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FlowEntity() {}

    public FlowEntity(UUID id, ProjectEntity project, AuthProfileEntity authProfile, CrawlRunEntity crawlRun, String name, FlowSource source, List<Map<String, Object>> steps, Map<String, Object> tags) {
        this.id = id;
        this.project = project;
        this.authProfile = authProfile;
        this.crawlRun = crawlRun;
        this.name = name;
        this.source = source;
        this.steps = steps;
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

    public AuthProfileEntity getAuthProfile() {
        return authProfile;
    }

    public CrawlRunEntity getCrawlRun() {
        return crawlRun;
    }

    public String getName() {
        return name;
    }

    public FlowSource getSource() {
        return source;
    }

    public List<Map<String, Object>> getSteps() {
        return steps;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void setSteps(List<Map<String, Object>> steps) {
        this.steps = steps;
        this.updatedAt = Instant.now();
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
        this.updatedAt = Instant.now();
    }
}


