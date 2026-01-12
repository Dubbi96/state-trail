package com.dubbi.statetrail.auth.api;

import com.dubbi.statetrail.auth.api.dto.AuthProfileDtos.AuthProfileDTO;
import com.dubbi.statetrail.auth.api.dto.AuthProfileDtos.CreateAuthProfileRequest;
import com.dubbi.statetrail.auth.domain.AuthProfileEntity;
import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.project.domain.ProjectRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects/{projectId}/auth-profiles")
public class AuthProfileController {
    private final ProjectRepository projectRepository;
    private final AuthProfileRepository authProfileRepository;

    public AuthProfileController(ProjectRepository projectRepository, AuthProfileRepository authProfileRepository) {
        this.projectRepository = projectRepository;
        this.authProfileRepository = authProfileRepository;
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

        // TODO: MinIO에 파일 업로드하고 objectKey 저장
        // 현재는 파일명만 저장 (임시)
        String objectKey = "storage-states/" + authProfileId + "/" + file.getOriginalFilename();
        profile.setStorageStateObjectKey(objectKey);
        
        return ResponseEntity.ok(toDto(authProfileRepository.save(profile)));
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
}


