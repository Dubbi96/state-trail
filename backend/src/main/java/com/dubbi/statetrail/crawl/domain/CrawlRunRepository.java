package com.dubbi.statetrail.crawl.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlRunRepository extends JpaRepository<CrawlRunEntity, UUID> {
    @Query("select r from CrawlRunEntity r where r.project.id = :projectId order by r.createdAt desc")
    List<CrawlRunEntity> findByProjectId(@Param("projectId") UUID projectId);
}


