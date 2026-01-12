package com.dubbi.statetrail.auth.service;

import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.dubbi.statetrail.project.domain.ProjectEntity;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Storage State 캡처 서비스
 * Playwright 브라우저를 열고 사용자가 로그인한 후 storage state를 자동으로 추출하여 저장
 */
@Service
public class StorageStateCaptureService {
    private final AuthProfileRepository authProfileRepository;
    private final ObjectStorageService objectStorageService;

    public StorageStateCaptureService(
            AuthProfileRepository authProfileRepository,
            ObjectStorageService objectStorageService
    ) {
        this.authProfileRepository = authProfileRepository;
        this.objectStorageService = objectStorageService;
    }

    /**
     * Storage State 캡처 (비동기)
     * 브라우저를 열고 로그인 페이지로 이동 후, 사용자가 로그인 완료할 때까지 대기
     * 로그인 완료 후 storage state를 추출하여 저장
     */
    @Async
    public CompletableFuture<String> captureStorageState(
            UUID authProfileId,
            String loginUrl,
            Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Playwright playwright = null;
            Browser browser = null;
            BrowserContext context = null;
            Page page = null;
            
            try {
                playwright = Playwright.create();
                // 헤드리스 모드 끄기 (사용자가 로그인할 수 있도록)
                browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(false));
                
                context = browser.newContext();
                page = context.newPage();
                
                // 로그인 페이지로 이동
                page.navigate(loginUrl);
                System.out.printf("[StorageStateCapture] Opened browser for auth profile %s, waiting for login at %s%n", 
                        authProfileId, loginUrl);
                
                // 사용자가 로그인할 때까지 대기
                // URL이 변경되거나 특정 요소가 나타날 때까지 대기하는 것이 좋지만,
                // 일단 타임아웃 시간만큼 대기하도록 함
                // 향후: 특정 URL 패턴이나 요소를 감지하도록 개선 가능
                Instant start = Instant.now();
                while (Instant.now().isBefore(start.plus(timeout))) {
                    Thread.sleep(2000); // 2초마다 체크
                    
                    // 현재 URL 확인 (로그인 완료 후 리다이렉트된 URL 확인)
                    String currentUrl = page.url();
                    if (!currentUrl.equals(loginUrl) && !currentUrl.contains("/login")) {
                        // 로그인 페이지가 아니면 로그인 완료로 간주
                        System.out.printf("[StorageStateCapture] Login detected, current URL: %s%n", currentUrl);
                        
                        // 추가 2초 대기 (쿠키/로컬스토리지 저장 완료 대기)
                        Thread.sleep(2000);
                        break;
                    }
                }
                
                // Storage state 추출 (임시 파일에 저장 후 읽기)
                Path tempStorageStateFile = Files.createTempFile("capture-storage-state-", ".json");
                try {
                    context.storageState(new BrowserContext.StorageStateOptions()
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
                }
            } catch (Exception e) {
                System.err.printf("[StorageStateCapture] Failed to capture storage state: %s%n", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to capture storage state: " + e.getMessage(), e);
            } finally {
                // 브라우저는 사용자가 확인할 수 있도록 열어둠
                // 자동 닫기를 원하면 주석 해제
                // if (browser != null) browser.close();
                // if (playwright != null) playwright.close();
            }
        });
    }

    /**
     * 동기 버전 (타임아웃 내에 완료 대기)
     */
    public String captureStorageStateSync(
            UUID authProfileId,
            String loginUrl,
            Duration timeout
    ) {
        try {
            return captureStorageState(authProfileId, loginUrl, timeout)
                    .get(timeout.toSeconds() + 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to capture storage state synchronously: " + e.getMessage(), e);
        }
    }
}

