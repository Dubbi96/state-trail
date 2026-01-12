package com.dubbi.statetrail.graph.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 시그니처 요약 생성 유틸리티
 * 전체 UI 시그니처에서 시각화에 필요한 요약 정보 추출
 */
public class UiSignatureSummary {
    
    /**
     * UI 시그니처에서 요약 정보 추출
     */
    public static Map<String, Object> summarize(Map<String, Object> uiSignature) {
        if (uiSignature == null || uiSignature.isEmpty()) {
            return Map.of();
        }
        
        Map<String, Object> summary = new HashMap<>();
        
        // CTA 텍스트 리스트
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ctas = (List<Map<String, Object>>) uiSignature.getOrDefault("ctas", List.of());
        List<String> ctaTexts = new ArrayList<>();
        for (Map<String, Object> cta : ctas) {
            Object text = cta.get("text");
            if (text != null && !text.toString().isBlank()) {
                ctaTexts.add(text.toString());
            }
        }
        if (!ctaTexts.isEmpty()) {
            summary.put("ctaTexts", ctaTexts);
        }
        
        // Form 필드 개수 및 타입
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forms = (List<Map<String, Object>>) uiSignature.getOrDefault("forms", List.of());
        int formCount = forms.size();
        List<String> formFields = new ArrayList<>();
        for (Map<String, Object> form : forms) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) form.getOrDefault("fields", List.of());
            for (Map<String, Object> field : fields) {
                Object type = field.get("type");
                if (type != null) {
                    formFields.add(type.toString());
                }
            }
        }
        if (formCount > 0) {
            summary.put("formCount", formCount);
            summary.put("formFieldTypes", formFields);
        }
        
        // Navigation 링크 개수
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> navElements = (List<Map<String, Object>>) uiSignature.getOrDefault("navElements", List.of());
        int navLinkCount = 0;
        for (Map<String, Object> nav : navElements) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) nav.getOrDefault("links", List.of());
            navLinkCount += links.size();
        }
        if (navLinkCount > 0) {
            summary.put("navLinkCount", navLinkCount);
        }
        
        // DOM 해시
        Object domHash = uiSignature.get("domHash");
        if (domHash != null) {
            summary.put("domHash", domHash);
        }
        
        return summary;
    }
    
    /**
     * 리스크 태그 추출 (간단한 휴리스틱)
     */
    public static Map<String, Object> extractRiskTags(Map<String, Object> uiSignature) {
        Map<String, Object> riskTags = new HashMap<>();
        
        if (uiSignature == null || uiSignature.isEmpty()) {
            return riskTags;
        }
        
        // 폼이 있으면 입력 필수
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forms = (List<Map<String, Object>>) uiSignature.getOrDefault("forms", List.of());
        if (!forms.isEmpty()) {
            riskTags.put("hasForms", true);
            
            // 필수 필드 확인
            boolean hasRequiredFields = false;
            for (Map<String, Object> form : forms) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fields = (List<Map<String, Object>>) form.getOrDefault("fields", List.of());
                for (Map<String, Object> field : fields) {
                    Object required = field.get("required");
                    if (required != null && Boolean.TRUE.equals(required)) {
                        hasRequiredFields = true;
                        break;
                    }
                }
            }
            if (hasRequiredFields) {
                riskTags.put("hasRequiredFields", true);
            }
        }
        
        // 외부 링크가 많으면 주의
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ctas = (List<Map<String, Object>>) uiSignature.getOrDefault("ctas", List.of());
        int externalLinkCount = 0;
        for (Map<String, Object> cta : ctas) {
            Object href = cta.get("href");
            if (href != null && href.toString().startsWith("http")) {
                externalLinkCount++;
            }
        }
        if (externalLinkCount > 5) {
            riskTags.put("manyExternalLinks", true);
        }
        
        return riskTags;
    }
}

