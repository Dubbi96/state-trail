package com.dubbi.statetrail.crawl.api;

import com.dubbi.statetrail.auth.domain.AuthProfileRepository;
import com.dubbi.statetrail.common.dto.ListResponse;
import com.dubbi.statetrail.crawl.api.dto.CrawlRunDtos.CrawlRunDTO;
import com.dubbi.statetrail.crawl.api.dto.CrawlRunDtos.CreateCrawlRunRequest;
import com.dubbi.statetrail.crawl.domain.CrawlRunEntity;
import com.dubbi.statetrail.crawl.domain.CrawlRunRepository;
import com.dubbi.statetrail.project.domain.ProjectRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/crawl-runs")
public class CrawlRunController {
    private final ProjectRepository projectRepository;
    private final AuthProfileRepository authProfileRepository;
    private final CrawlRunRepository crawlRunRepository;

    public CrawlRunController(ProjectRepository projectRepository, AuthProfileRepository authProfileRepository, CrawlRunRepository crawlRunRepository) {
        this.projectRepository = projectRepository;
        this.authProfileRepository = authProfileRepository;
        this.crawlRunRepository = crawlRunRepository;
    }

    @GetMapping
    public ListResponse<CrawlRunDTO> list(@PathVariable UUID projectId) {
        return ListResponse.of(crawlRunRepository.findByProjectId(projectId).stream().map(CrawlRunController::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<CrawlRunDTO> create(@PathVariable UUID projectId, @Valid @RequestBody CreateCrawlRunRequest req) {
        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return ResponseEntity.notFound().build();

        var authOpt = authProfileRepository.findById(req.authProfileId());
        if (authOpt.isEmpty()) return ResponseEntity.badRequest().build();

        if (!authOpt.get().getProject().getId().equals(projectId)) return ResponseEntity.badRequest().build();

        var entity = new CrawlRunEntity(UUID.randomUUID(), projectOpt.get(), authOpt.get(), req.startUrl(), req.budget());
        return ResponseEntity.ok(toDto(crawlRunRepository.save(entity)));
    }

    private static CrawlRunDTO toDto(CrawlRunEntity e) {
        return new CrawlRunDTO(
                e.getId(),
                e.getProject().getId(),
                e.getAuthProfile().getId(),
                e.getStatus(),
                e.getStartUrl(),
                e.getBudget()
        );
    }
}


