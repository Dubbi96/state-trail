package com.dubbi.statetrail.auth.api;

import com.dubbi.statetrail.auth.api.dto.AuthProfileDtos.AuthProfileDTO;
import com.dubbi.statetrail.auth.api.dto.AuthProfileDtos.CreateAuthProfileRequest;
import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.auth.service.StorageStateCaptureService;
import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.common.storage.ObjectStorageService;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.project.domain.ProjectRepository;
import java.util.Map;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/auth-profiles")
public class AuthProfileController {
    private final ProjectRepository projectRepository;
    private final AuthProfileRepository authProfileRepository;
    private final ObjectStorageService objectStorageService;
    private final StorageStateCaptureService storageStateCaptureService;
    private final CrawlRunRepository crawlRunRepository;

    public AuthProfileController(
            ProjectRepository projectRepository, 
            AuthProfileRepository authProfileRepository,
            ObjectStorageService objectStorageService,
            StorageStateCaptureService storageStateCaptureService,
            CrawlRunRepository crawlRunRepository
    ) {
        this.projectRepository = projectRepository;
        this.authProfileRepository = authProfileRepository;
        this.objectStorageService = objectStorageService;
        this.storageStateCaptureService = storageStateCaptureService;
        this.crawlRunRepository = crawlRunRepository;
    }

    @GetMapping
    public ListResponse<AuthProfileDTO> list(@PathVariable UUID projectId) {
        return ListResponse.of(authProfileRepository.findByProjectId(projectId).stream().map(AuthProfileController::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<AuthProfileDTO> create(@PathVariable UUID projectId, @Valid @RequestBody CreateAuthProfileRequest req) {
        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return ResponseEntity.notFound().build();

        var entity = new AuthProfileEntity(UUID.randomUUID(), projectOpt.get(), req.name(), req.type(), req.tags());
        return ResponseEntity.ok(toDto(authProfileRepository.save(entity)));
    }

    @PutMapping("/{authProfileId}/storage-state")
    public ResponseEntity<AuthProfileDTO> uploadStorageState(
            @PathVariable UUID projectId,
            @PathVariable UUID authProfileId,
            @RequestPart("file") MultipartFile file
    ) {
        var profileOpt = authProfileRepository.findById(authProfileId);
        if (profileOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var profile = profileOpt.get();
        if (!profile.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (profile.getType() != com.dubbi.statetrail.auth.domain.AuthProfileType.STORAGE_STATE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // 파일 내용 읽기
            byte[] fileBytes = file.getBytes();
            String fileContent = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            // MinIO에 storage state 저장
            String objectKey = objectStorageService.saveStorageState(authProfileId, fileContent);
            
            // AuthProfile에 objectKey 저장
            profile.setStorageStateObjectKey(objectKey);
            authProfileRepository.save(profile);
            
            return ResponseEntity.ok(toDto(profile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{authProfileId}/capture-storage-state")
    public ResponseEntity<Map<String, Object>> startCaptureStorageState(
            @PathVariable UUID projectId,
            @PathVariable UUID authProfileId,
            @RequestBody CaptureStorageStateRequest req
    ) {
        var profileOpt = authProfileRepository.findById(authProfileId);
        if (profileOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var profile = profileOpt.get();
        if (!profile.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (profile.getType() != com.dubbi.statetrail.auth.domain.AuthProfileType.STORAGE_STATE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 브라우저 열기
        storageStateCaptureService.startCaptureSession(authProfileId, req.loginUrl());

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "브라우저가 열렸습니다. 로그인을 완료한 후 '완료' 버튼을 눌러주세요."
        ));
    }

    @PostMapping("/{authProfileId}/complete-capture-storage-state")
    public ResponseEntity<Map<String, Object>> completeCaptureStorageState(
            @PathVariable UUID projectId,
            @PathVariable UUID authProfileId
    ) {
        var profileOpt = authProfileRepository.findById(authProfileId);
        if (profileOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var profile = profileOpt.get();
        if (!profile.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String objectKey = storageStateCaptureService.completeCaptureStorageState(authProfileId);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Storage state가 성공적으로 저장되었습니다.",
                    "objectKey", objectKey
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "ok", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "ok", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/{authProfileId}/login-script")
    public ResponseEntity<AuthProfileDTO> updateLoginScript(
            @PathVariable UUID projectId,
            @PathVariable UUID authProfileId,
            @RequestBody UpdateLoginScriptRequest req
    ) {
        var profileOpt = authProfileRepository.findById(authProfileId);
        if (profileOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var profile = profileOpt.get();
        if (!profile.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (profile.getType() != com.dubbi.statetrail.auth.domain.AuthProfileType.SCRIPT_LOGIN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        profile.setLoginScript(req.script());
        return ResponseEntity.ok(toDto(authProfileRepository.save(profile)));
    }

    private static AuthProfileDTO toDto(AuthProfileEntity e) {
        return new AuthProfileDTO(e.getId(), e.getProject().getId(), e.getName(), e.getType(), e.getTags());
    }

    public record UpdateLoginScriptRequest(String script) {}
    
    public record CaptureStorageStateRequest(String loginUrl) {}
    
    @DeleteMapping("/{authProfileId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID authProfileId
    ) {
        var profileOpt = authProfileRepository.findById(authProfileId);
        if (profileOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var profile = profileOpt.get();
        if (!profile.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // 관련된 CrawlRun들 먼저 삭제
        var relatedRuns = crawlRunRepository.findByAuthProfileId(authProfileId);
        if (!relatedRuns.isEmpty()) {
            crawlRunRepository.deleteAll(relatedRuns);
        }
        
        // 진행 중인 캡처 세션이 있으면 취소
        try {
            storageStateCaptureService.cancelCaptureSession(authProfileId);
        } catch (Exception e) {
            // ignore
        }
        
        // AuthProfile 삭제
        authProfileRepository.delete(profile);
        
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", String.format("Auth Profile과 관련된 %d개의 Crawl Run이 함께 삭제되었습니다.", relatedRuns.size())
        ));
    }
}


