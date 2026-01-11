package com.dubbi.statetrail.crawl.web;

public enum CrawlStrategy {
    BFS,
    MCS,
    BROWSER_BFS,
    BROWSER_MCS;

    public static CrawlStrategy fromNullable(String raw) {
        if (raw == null) return BFS;
        try {
            return CrawlStrategy.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return BFS;
        }
    }

    public boolean isBrowser() {
        return this == BROWSER_BFS || this == BROWSER_MCS;
    }

    public CrawlStrategy base() {
        return switch (this) {
            case BROWSER_BFS -> BFS;
            case BROWSER_MCS -> MCS;
            default -> this;
        };
    }
}


