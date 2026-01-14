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
                    const seenTexts = new Set();
                    
                    // 1. 기본 버튼과 링크 요소 추출
                    document.querySelectorAll('button, a[href], [role="button"], [role="link"], [role="menuitem"]').forEach(el => {
                        const text = (el.innerText || el.textContent || '').trim();
                        const type = el.tagName.toLowerCase();
                        const href = el.getAttribute('href') || el.getAttribute('data-href') || el.getAttribute('data-to') || el.getAttribute('data-path');
                        
                        // 중복 제거 (동일한 텍스트가 이미 있으면 스킵)
                        if (text.length > 0 && seenTexts.has(text)) return;
                        if (text.length > 0 || href) {
                            seenTexts.add(text);
                            ctas.push({
                                type: type,
                                text: text.slice(0, 100),
                                href: href || null,
                                selector: generateSelector(el)
                            });
                        }
                    });
                    
                    // 2. SPA 네비게이션 패턴: data-to, data-href, data-path 속성
                    document.querySelectorAll('[data-to], [data-href], [data-path]').forEach(el => {
                        const text = (el.innerText || el.textContent || '').trim();
                        const href = el.getAttribute('data-to') || el.getAttribute('data-href') || el.getAttribute('data-path');
                        if (!seenTexts.has(text) && text.length > 0) {
                            seenTexts.add(text);
                            ctas.push({
                                type: el.tagName.toLowerCase(),
                                text: text.slice(0, 100),
                                href: href || null,
                                selector: generateSelector(el)
                            });
                        }
                    });
                    
                    // 3. cursor:pointer인 클릭 가능한 요소 (MUI Stack/div 기반 메뉴 대응)
                    // 단, 너무 큰 컨테이너는 제외 (화면의 50% 이상 덮는 요소 제외)
                    const viewportWidth = window.innerWidth;
                    const viewportHeight = window.innerHeight;
                    const viewportArea = viewportWidth * viewportHeight;
                    const thresholdArea = viewportArea * 0.5;
                    
                    document.querySelectorAll('*').forEach(el => {
                        // 이미 처리한 요소는 스킵
                        if (el.tagName === 'BUTTON' || el.tagName === 'A' || el.getAttribute('role') === 'button' || el.getAttribute('role') === 'link' || el.getAttribute('role') === 'menuitem') {
                            return;
                        }
                        
                        // data 속성이 이미 처리되었으면 스킵
                        if (el.hasAttribute('data-to') || el.hasAttribute('data-href') || el.hasAttribute('data-path')) {
                            return;
                        }
                        
                        const style = window.getComputedStyle(el);
                        const cursor = style.cursor;
                        const display = style.display;
                        const visibility = style.visibility;
                        const opacity = parseFloat(style.opacity) || 1;
                        
                        // cursor:pointer이고 보이는 요소만
                        if (cursor === 'pointer' && 
                            display !== 'none' && 
                            visibility !== 'hidden' && 
                            opacity > 0 &&
                            el.offsetParent !== null) {
                            
                            // 너무 큰 컨테이너는 제외
                            const rect = el.getBoundingClientRect();
                            const area = rect.width * rect.height;
                            if (area > thresholdArea) {
                                return;
                            }
                            
                            const text = (el.innerText || el.textContent || '').trim();
                            // 텍스트가 있고, 중복이 아니고, 의미있는 텍스트인 경우만 (너무 짧거나 긴 것 제외)
                            if (text.length >= 2 && text.length <= 200 && !seenTexts.has(text)) {
                                // nav/aside/drawer 하위 요소에 가중치 (네비게이션 가능성 높음)
                                const isInNav = el.closest('nav, aside, [role="navigation"], [role="complementary"], [class*="Drawer"], [class*="Sidebar"], [class*="Lnb"]') !== null;
                                
                                seenTexts.add(text);
                                ctas.push({
                                    type: el.tagName.toLowerCase(),
                                    text: text.slice(0, 100),
                                    href: null, // cursor:pointer는 href가 없을 수 있음
                                    selector: generateSelector(el)
                                });
                            }
                        }
                    });
                    
                    return ctas;
                }
                function generateSelector(el) {
                    // id → data-testid → aria-label → 안정 클래스 순으로 선택자 생성
                    if (el.id) return '#' + el.id;
                    if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                    if (el.getAttribute('aria-label')) return '[aria-label="' + el.getAttribute('aria-label') + '"]';
                    if (el.className) {
                        // 해시 클래스(css-xxxx) 제외하고 안정 클래스만 사용
                        const classes = el.className.split(' ').filter(c => c && !c.match(/^css-[a-z0-9]+$/)).slice(0, 2).join('.');
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

