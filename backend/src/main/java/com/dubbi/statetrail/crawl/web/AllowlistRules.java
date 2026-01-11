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

        // If domains list is empty, allow all domains (for external site crawling)
        if (!domains.isEmpty()) {
            boolean domainMatch = domains.stream().anyMatch(d -> host.equalsIgnoreCase(d) || host.endsWith("." + d));
            if (!domainMatch) {
                System.out.printf("[Allowlist] Domain not allowed: %s (allowed: %s)%n", host, domains);
                return false;
            }
        }
        
        // If pathPrefixes list is empty, allow all paths
        if (!pathPrefixes.isEmpty()) {
            boolean pathMatch = pathPrefixes.stream().anyMatch(path::startsWith);
            if (!pathMatch) {
                System.out.printf("[Allowlist] Path not allowed: %s (allowed prefixes: %s)%n", path, pathPrefixes);
                return false;
            }
        }
        
        // Check deny list
        if (!deny.isEmpty() && deny.stream().anyMatch(path::startsWith)) {
            System.out.printf("[Allowlist] Path denied: %s (deny list: %s)%n", path, deny);
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


