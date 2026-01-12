package com.dubbi.statetrail.common.util;

import com.dubbi.statetrail.common.util.Hashing;
import java.util.Map;

/**
 * State 노드의 nodeKey 계산 유틸리티
 * nodeKey = SHA256(url + authContext + uiSignatureHash)
 */
public class NodeKeyCalculator {
    
    /**
     * State 노드의 nodeKey를 계산
     * 
     * @param url 페이지 URL
     * @param authContext 인증 컨텍스트 (null 가능, AuthProfile ID 또는 "anonymous")
     * @param uiSignature UI 시그니처 맵 (null 가능)
     * @return nodeKey (SHA256 해시)
     */
    public static String calculateNodeKey(String url, String authContext, Map<String, Object> uiSignature) {
        StringBuilder input = new StringBuilder();
        input.append(url != null ? url : "");
        input.append("|");
        input.append(authContext != null ? authContext : "anonymous");
        input.append("|");
        
        // UI 시그니처를 안정적인 문자열로 변환
        if (uiSignature != null && !uiSignature.isEmpty()) {
            String signatureHash = Hashing.sha256Hex(uiSignature.toString());
            input.append(signatureHash);
        }
        
        return Hashing.sha256Hex(input.toString());
    }
    
    /**
     * 간단한 URL 기반 nodeKey (기존 호환성용, UI 시그니처 없이)
     */
    public static String calculateSimpleNodeKey(String url) {
        return Hashing.sha256Hex(url);
    }
}

