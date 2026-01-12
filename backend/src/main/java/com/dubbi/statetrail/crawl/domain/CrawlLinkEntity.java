package com.dubbi.statetrail.crawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.UniqueConstraint;
import com.dubbi.statetrail.crawl.web.ActionType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "crawl_links",
        uniqueConstraints = @UniqueConstraint(name = "uq_crawl_links_run_from_to", columnNames = {"crawl_run_id", "from_page_id", "to_page_id"})
)
public class CrawlLinkEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_run_id", nullable = false)
    private CrawlRunEntity crawlRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_page_id", nullable = false)
    private CrawlPageEntity fromPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_page_id", nullable = false)
    private CrawlPageEntity toPage;

    @Column(name = "anchor_text", length = 512)
    private String anchorText;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    @Column(length = 512)
    private String locator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_tags", columnDefinition = "jsonb")
    private Map<String, Object> riskTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "http_evidence", columnDefinition = "jsonb")
    private Map<String, Object> httpEvidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CrawlLinkEntity() {}

    public CrawlLinkEntity(UUID id, CrawlRunEntity crawlRun, CrawlPageEntity fromPage, CrawlPageEntity toPage, String anchorText) {
        this.id = id;
        this.crawlRun = crawlRun;
        this.fromPage = fromPage;
        this.toPage = toPage;
        this.anchorText = anchorText;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CrawlRunEntity getCrawlRun() {
        return crawlRun;
    }

    public CrawlPageEntity getFromPage() {
        return fromPage;
    }

    public CrawlPageEntity getToPage() {
        return toPage;
    }

    public String getAnchorText() {
        return anchorText;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, Object> getRiskTags() {
        return riskTags;
    }

    public void setRiskTags(Map<String, Object> riskTags) {
        this.riskTags = riskTags;
    }

    public Map<String, Object> getHttpEvidence() {
        return httpEvidence;
    }

    public void setHttpEvidence(Map<String, Object> httpEvidence) {
        this.httpEvidence = httpEvidence;
    }
}


