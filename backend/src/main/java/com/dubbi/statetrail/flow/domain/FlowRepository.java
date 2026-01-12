package com.dubbi.statetrail.flow.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlowRepository extends JpaRepository<FlowEntity, UUID> {
    @Query("select f from FlowEntity f where f.crawlRun.id = :runId order by f.createdAt desc")
    List<FlowEntity> findByRunId(@Param("runId") UUID runId);

    @Query("select f from FlowEntity f where f.project.id = :projectId")
    List<FlowEntity> findByProjectId(@Param("projectId") UUID projectId);
}


