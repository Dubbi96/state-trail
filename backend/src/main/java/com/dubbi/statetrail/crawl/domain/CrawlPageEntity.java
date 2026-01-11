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
import java.util.UUID;

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

    public void markFetched(Integer httpStatus, String contentType, String title, String htmlSnapshot) {
        this.httpStatus = httpStatus;
        this.contentType = contentType;
        this.title = title;
        this.htmlSnapshot = htmlSnapshot;
        this.fetchedAt = Instant.now();
    }
}


