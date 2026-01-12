package com.dubbi.statetrail.crawl.web;

/**
 * 사용자 행동 타입
 */
public enum ActionType {
    CLICK,      // 버튼/링크 클릭
    INPUT,      // 폼 입력
    SUBMIT,     // 폼 제출
    NAVIGATE    // 네비게이션 (URL 직접 입력, 리다이렉트 등)
}

