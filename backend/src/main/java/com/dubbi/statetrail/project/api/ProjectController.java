package com.dubbi.statetrail.project.api;

import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.CreateProjectRequest;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.ProjectDTO;
import com.dubbi.statetrail.project.api.dto.ProjectDtos.UpdateProjectRequest;
import com.dubbi.statetrail.project.domain.ProjectEntity;
import com.dubbi.statetrail.project.domain.ProjectRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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

    private static ProjectDTO toDto(ProjectEntity e) {
        return new ProjectDTO(e.getId(), e.getName(), e.getBaseUrl(), e.getAllowlistRules());
    }
}


