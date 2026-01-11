package com.dubbi.statetrail.crawl.web;

public enum CrawlStrategy {
    BFS,
    MCS;

    public static CrawlStrategy fromNullable(String raw) {
        if (raw == null) return BFS;
        try {
            return CrawlStrategy.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return BFS;
        }
    }
}


