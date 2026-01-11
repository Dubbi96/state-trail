package com.dubbi.statetrail.crawl.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record AllowlistRules(
        List<String> domains,
        List<String> pathPrefixes,
        List<String> deny
) {
    public static AllowlistRules from(Map<String, Object> json) {
        return new AllowlistRules(
                stringList(json, "domains"),
                stringList(json, "pathPrefixes"),
                stringList(json, "deny")
        );
    }

    public boolean allows(URI uri) {
        if (uri == null) return false;
        String host = uri.getHost() == null ? "" : uri.getHost();
        String rawPath = uri.getPath();
        String path = (rawPath == null || rawPath.isBlank()) ? "/" : rawPath;

        if (!domains.isEmpty() && domains.stream().noneMatch(d -> host.equalsIgnoreCase(d) || host.endsWith("." + d))) {
            return false;
        }
        if (!pathPrefixes.isEmpty() && pathPrefixes.stream().noneMatch(path::startsWith)) {
            return false;
        }
        if (!deny.isEmpty() && deny.stream().anyMatch(path::startsWith)) {
            return false;
        }
        return true;
    }

    private static List<String> stringList(Map<String, Object> json, String key) {
        if (json == null) return List.of();
        Object v = json.get(key);
        if (v == null) return List.of();
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}


