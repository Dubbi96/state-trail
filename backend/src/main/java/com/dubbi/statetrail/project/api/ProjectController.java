package com.dubbi.statetrail.project.api;

import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.flow.domain.FlowRepository;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.CreateProjectRequest;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.ProjectDTO;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.UpdateProjectRequest;
import com.dubbi.statetrail.project.domain.ProjectEntity;
import com.dubbi.statetrail.project.domain.ProjectRepository;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final AuthProfileRepository authProfileRepository;
    private final CrawlRunRepository crawlRunRepository;
    private final FlowRepository flowRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            AuthProfileRepository authProfileRepository,
            CrawlRunRepository crawlRunRepository,
            FlowRepository flowRepository
    ) {
        this.projectRepository = projectRepository;
        this.authProfileRepository = authProfileRepository;
        this.crawlRunRepository = crawlRunRepository;
        this.flowRepository = flowRepository;
    }

    @GetMapping
    public ListResponse<ProjectDTO> list() {
        return ListResponse.of(projectRepository.findAll().stream().map(ProjectController::toDto).toList());
    }

    @PostMapping
    public ProjectDTO create(@Valid @RequestBody CreateProjectRequest req) {
        var entity = new ProjectEntity(UUID.randomUUID(), req.name(), req.baseUrl(), req.allowlistRules());
        return toDto(projectRepository.save(entity));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> get(@PathVariable UUID projectId) {
        return projectRepository.findById(projectId)
                .map(ProjectController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> update(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest req) {
        var entityOpt = projectRepository.findById(projectId);
        if (entityOpt.isEmpty()) return ResponseEntity.notFound().build();
        var entity = entityOpt.get();
        entity.update(req.name(), req.baseUrl(), req.allowlistRules());
        return ResponseEntity.ok(toDto(projectRepository.save(entity)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID projectId) {
        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 관련된 데이터 삭제 순서 (외래 키 제약 조건을 고려)
        // 1. Flows 삭제 (crawl_run 참조)
        var flows = flowRepository.findByProjectId(projectId);
        if (!flows.isEmpty()) {
            flowRepository.deleteAll(flows);
        }
        
        // 2. CrawlRuns 삭제 (auth_profile, project 참조)
        var crawlRuns = crawlRunRepository.findByProjectId(projectId);
        if (!crawlRuns.isEmpty()) {
            crawlRunRepository.deleteAll(crawlRuns);
        }
        
        // 3. AuthProfiles 삭제 (project 참조)
        var authProfiles = authProfileRepository.findByProjectId(projectId);
        if (!authProfiles.isEmpty()) {
            authProfileRepository.deleteAll(authProfiles);
        }
        
        // 4. Project 삭제
        projectRepository.deleteById(projectId);
        
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", String.format(
                        "프로젝트와 함께 %d개의 Auth Profile, %d개의 Crawl Run, %d개의 Flow가 삭제되었습니다.",
                        authProfiles.size(),
                        crawlRuns.size(),
                        flows.size()
                )
        ));
    }

    private static ProjectDTO toDto(ProjectEntity e) {
        return new ProjectDTO(e.getId(), e.getName(), e.getBaseUrl(), e.getAllowlistRules());
    }
}


