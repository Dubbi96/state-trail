package com.dubbi.statetrail.crawl.web;

import java.time.Duration;
import java.util.Map;

public record CrawlBudget(
        int maxNodes,
        int maxEdges,
        int maxDepth,
        Duration maxDuration
) {
    public static CrawlBudget from(Map<String, Object> budget) {
        int maxNodes = intOrDefault(budget, "maxNodes", 100);
        int maxEdges = intOrDefault(budget, "maxEdges", 400);
        int maxDepth = intOrDefault(budget, "maxDepth", 6);
        int maxMinutes = intOrDefault(budget, "maxMinutes", 5);
        return new CrawlBudget(maxNodes, maxEdges, maxDepth, Duration.ofMinutes(maxMinutes));
    }

    private static int intOrDefault(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}


