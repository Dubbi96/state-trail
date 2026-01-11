package com.dubbi.statetrail.common.util;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * URL 패턴 정규화 유틸리티
 * 동일한 패턴의 URL들을 하나로 묶기 위해 쿼리 파라미터의 숫자 값을 placeholder로 치환
 * 
 * 예:
 * - https://example.com/main?referenceID=1&type=1 -> https://example.com/main?referenceID={id}&type={id}
 * - https://example.com/main?referenceID=2&type=2 -> https://example.com/main?referenceID={id}&type={id}
 */
public class UrlPattern {
    private static final Pattern NUMERIC_VALUE = Pattern.compile("\\d+");
    
    /**
     * URL을 패턴으로 정규화
     * 쿼리 파라미터의 숫자 값들을 {id} placeholder로 치환
     */
    public static String normalizeToPattern(String url) {
        if (url == null || url.isBlank()) return url;
        
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            
            // 쿼리 파라미터의 숫자 값들을 {id}로 치환
            // 예: referenceID=1&type=2 -> referenceID={id}&type={id}
            String normalizedQuery = null;
            if (query != null && !query.isBlank()) {
                // Split by & and process each parameter
                String[] params = query.split("&");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < params.length; i++) {
                    String param = params[i];
                    int eqIdx = param.indexOf('=');
                    if (eqIdx > 0 && eqIdx < param.length() - 1) {
                        String key = param.substring(0, eqIdx);
                        String value = param.substring(eqIdx + 1);
                        // Replace numeric values with {id}
                        if (NUMERIC_VALUE.matcher(value).matches()) {
                            sb.append(key).append("={id}");
                        } else {
                            sb.append(param);
                        }
                    } else {
                        sb.append(param);
                    }
                    if (i < params.length - 1) sb.append("&");
                }
                normalizedQuery = sb.toString();
            }
            
            // 포트가 기본 포트면 생략
            String authority = host;
            if (port > 0 && port != 80 && port != 443) {
                authority = host + ":" + port;
            }
            
            // URI 재구성
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(authority);
            if (path != null) sb.append(path);
            if (normalizedQuery != null) sb.append("?").append(normalizedQuery);
            
            return sb.toString();
        } catch (Exception e) {
            // URI 파싱 실패 시 원본 URL 반환
            return url;
        }
    }
    
    /**
     * 두 URL이 같은 패턴인지 확인
     */
    public static boolean isSamePattern(String url1, String url2) {
        if (url1 == null || url2 == null) return false;
        return normalizeToPattern(url1).equals(normalizeToPattern(url2));
    }
}

