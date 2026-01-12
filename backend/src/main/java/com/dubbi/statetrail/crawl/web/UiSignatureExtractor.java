package com.dubbi.statetrail.crawl.web;

import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 시그니처 추출 유틸리티
 * 페이지의 DOM 구조, CTA, 폼 필드 등을 분석하여 UI 시그니처 생성
 */
public class UiSignatureExtractor {
    
    /**
     * Playwright Page에서 UI 시그니처 추출
     */
    public static Map<String, Object> extractFromPage(Page page) {
        Map<String, Object> signature = new HashMap<>();
        
        try {
            // DOM 해시 계산 (간단한 구조 해시)
            String domHash = extractDomHash(page);
            signature.put("domHash", domHash);
            
            // CTA (Call-to-Action) 요소들 추출
            List<Map<String, Object>> ctas = extractCTAs(page);
            signature.put("ctas", ctas);
            
            // 폼 필드 추출
            List<Map<String, Object>> forms = extractForms(page);
            signature.put("forms", forms);
            
            // 네비게이션 요소 (링크, 버튼) 추출
            List<Map<String, Object>> navElements = extractNavigationElements(page);
            signature.put("navElements", navElements);
            
            // 페이지 메타데이터
            Map<String, Object> metadata = extractMetadata(page);
            signature.put("metadata", metadata);
            
        } catch (Exception e) {
            // 추출 실패 시 빈 시그니처 반환
            signature.put("error", e.getMessage());
        }
        
        return signature;
    }
    
    private static String extractDomHash(Page page) {
        try {
            // 주요 구조 요소들의 선택자 기반 해시 계산
            Object result = page.evaluate("""
                () => {
                    const selectors = [
                        'h1', 'h2', 'h3', 'form', 'button', 'input', 
                        'a[href]', '[role="button"]', '[role="link"]'
                    ];
                    const counts = selectors.map(s => document.querySelectorAll(s).length);
                    return counts.join(',');
                }
            """);
            return String.valueOf(result);
        } catch (Exception e) {
            return "";
        }
    }
    
    private static List<Map<String, Object>> extractCTAs(Page page) {
        try {
            Object result = page.evaluate("""
                () => {
                    const ctas = [];
                    // 버튼과 링크 요소 추출
                    document.querySelectorAll('button, a[href], [role="button"], [role="link"]').forEach(el => {
                        const text = (el.innerText || el.textContent || '').trim();
                        const type = el.tagName.toLowerCase();
                        const href = el.getAttribute('href');
                        if (text.length > 0 || href) {
                            ctas.push({
                                type: type,
                                text: text.slice(0, 100),
                                href: href || null,
                                selector: generateSelector(el)
                            });
                        }
                    });
                    return ctas;
                }
                function generateSelector(el) {
                    if (el.id) return '#' + el.id;
                    if (el.className) {
                        const classes = el.className.split(' ').filter(c => c).slice(0, 2).join('.');
                        if (classes) return el.tagName.toLowerCase() + '.' + classes;
                    }
                    return el.tagName.toLowerCase();
                }
            """);
            
            if (result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) result;
                return list;
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }
    
    private static List<Map<String, Object>> extractForms(Page page) {
        try {
            Object result = page.evaluate("""
                () => {
                    const forms = [];
                    document.querySelectorAll('form').forEach(form => {
                        const fields = [];
                        form.querySelectorAll('input, select, textarea').forEach(input => {
                            fields.push({
                                type: input.type || input.tagName.toLowerCase(),
                                name: input.name || null,
                                id: input.id || null,
                                required: input.required || false
                            });
                        });
                        if (fields.length > 0) {
                            forms.push({
                                action: form.action || null,
                                method: form.method || 'GET',
                                fields: fields
                            });
                        }
                    });
                    return forms;
                }
            """);
            
            if (result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) result;
                return list;
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }
    
    private static List<Map<String, Object>> extractNavigationElements(Page page) {
        try {
            Object result = page.evaluate("""
                () => {
                    const navs = [];
                    document.querySelectorAll('nav, [role="navigation"]').forEach(nav => {
                        const links = [];
                        nav.querySelectorAll('a[href]').forEach(a => {
                            links.push({
                                text: (a.innerText || '').trim().slice(0, 50),
                                href: a.href
                            });
                        });
                        if (links.length > 0) {
                            navs.push({ links: links });
                        }
                    });
                    return navs;
                }
            """);
            
            if (result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) result;
                return list;
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }
    
    private static Map<String, Object> extractMetadata(Page page) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            String title = page.title();
            metadata.put("title", title);
            
            // viewport 정보
            Object viewport = page.evaluate("() => ({ width: window.innerWidth, height: window.innerHeight })");
            if (viewport instanceof Map<?, ?>) {
                metadata.put("viewport", viewport);
            }
        } catch (Exception e) {
            // ignore
        }
        return metadata;
    }
}

