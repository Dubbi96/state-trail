package com.dubbi.statetrail.crawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "crawl_pages",
        uniqueConstraints = @UniqueConstraint(name = "uq_crawl_pages_run_url", columnNames = {"crawl_run_id", "url"})
)
public class CrawlPageEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_run_id", nullable = false)
    private CrawlRunEntity crawlRun;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "url_pattern", length = 2048)
    private String urlPattern;

    @Column(length = 512)
    private String title;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(nullable = false)
    private int depth;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @Column(name = "html_snapshot", columnDefinition = "text")
    private String htmlSnapshot;

    @Column(name = "screenshot_object_key", length = 512)
    private String screenshotObjectKey;

    @Column(name = "network_log_object_key", length = 512)
    private String networkLogObjectKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_signature", columnDefinition = "jsonb")
    private Map<String, Object> uiSignature;

    protected CrawlPageEntity() {}

    public CrawlPageEntity(UUID id, CrawlRunEntity crawlRun, String nodeKey, String url, int depth) {
        this.id = id;
        this.crawlRun = crawlRun;
        this.nodeKey = nodeKey;
        this.url = url;
        this.depth = depth;
        this.discoveredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CrawlRunEntity getCrawlRun() {
        return crawlRun;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getTitle() {
        return title;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public int getDepth() {
        return depth;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getHtmlSnapshot() {
        return htmlSnapshot;
    }

    public String getScreenshotObjectKey() {
        return screenshotObjectKey;
    }

    public void setScreenshotObjectKey(String screenshotObjectKey) {
        this.screenshotObjectKey = screenshotObjectKey;
    }

    public String getNetworkLogObjectKey() {
        return networkLogObjectKey;
    }

    public void setNetworkLogObjectKey(String networkLogObjectKey) {
        this.networkLogObjectKey = networkLogObjectKey;
    }

    public Map<String, Object> getUiSignature() {
        return uiSignature;
    }

    public void setUiSignature(Map<String, Object> uiSignature) {
        this.uiSignature = uiSignature;
    }

    public void markFetched(Integer httpStatus, String contentType, String title, String htmlSnapshot) {
        this.httpStatus = httpStatus;
        this.contentType = contentType;
        this.title = title;
        this.htmlSnapshot = htmlSnapshot;
        this.fetchedAt = Instant.now();
    }

    public void markStateCaptured(String screenshotObjectKey, String networkLogObjectKey, Map<String, Object> uiSignature) {
        this.screenshotObjectKey = screenshotObjectKey;
        this.networkLogObjectKey = networkLogObjectKey;
        this.uiSignature = uiSignature;
    }
}


