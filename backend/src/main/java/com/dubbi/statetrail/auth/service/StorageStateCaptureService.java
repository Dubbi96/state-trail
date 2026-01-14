package com.dubbi.statetrail.auth.service;

import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * Storage State 캡처 서비스
 * 브라우저를 열고 사용자가 로그인한 후, 수동으로 완료 버튼을 눌러 storage state를 저장
 */
@Service
public class StorageStateCaptureService {
    private final AuthProfileRepository authProfileRepository;
    private final ObjectStorageService objectStorageService;
    
    // 진행 중인 캡처 세션 관리: authProfileId -> SessionInfo
    private static class SessionInfo {
        final BrowserContext context;
        final Browser browser;
        final Playwright playwright;
        
        SessionInfo(BrowserContext context, Browser browser, Playwright playwright) {
            this.context = context;
            this.browser = browser;
            this.playwright = playwright;
        }
    }
    
    private final ConcurrentMap<UUID, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public StorageStateCaptureService(
            AuthProfileRepository authProfileRepository,
            ObjectStorageService objectStorageService
    ) {
        this.authProfileRepository = authProfileRepository;
        this.objectStorageService = objectStorageService;
    }

    /**
     * 브라우저 열기 (로그인 페이지로 이동)
     * 사용자가 로그인한 후 completeCaptureStorageState를 호출해야 함
     */
    public void startCaptureSession(UUID authProfileId, String loginUrl) {
        try {
            // 기존 세션이 있으면 먼저 취소
            SessionInfo existingSession = activeSessions.remove(authProfileId);
            if (existingSession != null) {
                try {
                    if (existingSession.browser != null) {
                        existingSession.browser.close();
                    }
                    if (existingSession.playwright != null) {
                        existingSession.playwright.close();
                    }
                } catch (Exception e) {
                    // 기존 세션 정리 실패는 무시
                }
            }
            
            Playwright playwright = Playwright.create();
            // 헤드리스 모드 끄기 (사용자가 로그인할 수 있도록)
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false));
            
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            
            // 로그인 페이지로 이동 (타임아웃 설정)
            page.navigate(loginUrl, new Page.NavigateOptions().setTimeout(30000));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            
            System.out.printf("[StorageStateCapture] Opened browser for auth profile %s at %s%n", 
                    authProfileId, loginUrl);
            
            // 세션 저장 (나중에 완료할 때 사용)
            activeSessions.put(authProfileId, new SessionInfo(context, browser, playwright));
        } catch (Exception e) {
            System.err.printf("[StorageStateCapture] Failed to start capture session: %s%n", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start capture session: " + e.getMessage(), e);
        }
    }

    /**
     * Storage State 캡처 완료 (수동 호출)
     * 사용자가 로그인을 완료한 후 이 메서드를 호출하여 storage state를 저장
     */
    public String completeCaptureStorageState(UUID authProfileId) {
        SessionInfo session = activeSessions.remove(authProfileId);
        if (session == null) {
            throw new IllegalStateException("No active capture session found for auth profile: " + authProfileId);
        }
        
        try {
            // Storage state 추출 (임시 파일에 저장 후 읽기)
            Path tempStorageStateFile = Files.createTempFile("capture-storage-state-", ".json");
            try {
                session.context.storageState(new BrowserContext.StorageStateOptions()
                        .setPath(tempStorageStateFile));
                
                // 파일에서 JSON 읽기
                String storageStateJson = Files.readString(tempStorageStateFile);
            
                // MinIO에 저장
                String objectKey = objectStorageService.saveStorageState(authProfileId, storageStateJson);
                
                // AuthProfile 업데이트
                var profileOpt = authProfileRepository.findById(authProfileId);
                if (profileOpt.isPresent()) {
                    var profile = profileOpt.get();
                    profile.setStorageStateObjectKey(objectKey);
                    authProfileRepository.save(profile);
                    System.out.printf("[StorageStateCapture] Storage state saved to %s for auth profile %s%n", 
                            objectKey, authProfileId);
                }
                
                return objectKey;
            } finally {
                // 임시 파일 삭제
                try {
                    Files.deleteIfExists(tempStorageStateFile);
                } catch (IOException e) {
                    // ignore cleanup errors
                }
                
                // 브라우저 닫기
                try {
                    if (session.browser != null) {
                        session.browser.close();
                    }
                    if (session.playwright != null) {
                        session.playwright.close();
                    }
                } catch (Exception e) {
                    // ignore cleanup errors
                }
            }
        } catch (Exception e) {
            System.err.printf("[StorageStateCapture] Failed to complete capture: %s%n", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to complete capture: " + e.getMessage(), e);
        }
    }

    /**
     * 진행 중인 세션 취소
     */
    public void cancelCaptureSession(UUID authProfileId) {
        SessionInfo session = activeSessions.remove(authProfileId);
        if (session != null) {
            try {
                if (session.browser != null) {
                    session.browser.close();
                }
                if (session.playwright != null) {
                    session.playwright.close();
                }
            } catch (Exception e) {
                // ignore cleanup errors
            }
        }
    }
}
