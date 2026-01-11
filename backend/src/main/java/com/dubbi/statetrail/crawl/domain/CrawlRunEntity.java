package com.dubbi.statetrail.crawl.domain;

import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
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
@Table(name = "crawl_runs")
public class CrawlRunEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_profile_id", nullable = false)
    private AuthProfileEntity authProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlRunStatus status;

    @Column(name = "start_url", nullable = false)
    private String startUrl;

    @Column(name = "strategy")
    private String strategy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> budget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> stats;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected CrawlRunEntity() {}

    public CrawlRunEntity(UUID id, ProjectEntity project, AuthProfileEntity authProfile, String startUrl, Map<String, Object> budget) {
        this.id = id;
        this.project = project;
        this.authProfile = authProfile;
        this.status = CrawlRunStatus.QUEUED;
        this.startUrl = startUrl;
        this.strategy = "BFS";
        this.budget = budget;
        this.stats = Map.of("nodes", 0, "edges", 0, "errors", 0);
        this.createdAt = Instant.now();
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

    public CrawlRunStatus getStatus() {
        return status;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public String getStrategy() {
        return strategy == null || strategy.isBlank() ? "BFS" : strategy;
    }

    public Map<String, Object> getBudget() {
        return budget;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void markRunning() {
        this.status = CrawlRunStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markSucceeded(Map<String, Object> stats) {
        this.status = CrawlRunStatus.SUCCEEDED;
        this.stats = stats;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String errorMessage, Map<String, Object> stats) {
        this.status = CrawlRunStatus.FAILED;
        this.errorMessage = errorMessage;
        this.stats = stats;
        this.finishedAt = Instant.now();
    }

    public void updateStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public void setStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) return;
        this.strategy = strategy;
    }
}


