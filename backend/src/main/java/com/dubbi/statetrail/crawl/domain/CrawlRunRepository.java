package com.dubbi.statetrail.crawl.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlRunRepository extends JpaRepository<CrawlRunEntity, UUID> {
    @Query("select r from CrawlRunEntity r where r.project.id = :projectId order by r.createdAt desc")
    List<CrawlRunEntity> findByProjectId(@Param("projectId") UUID projectId);

    @Query("select r from CrawlRunEntity r where r.authProfile.id = :authProfileId")
    List<CrawlRunEntity> findByAuthProfileId(@Param("authProfileId") UUID authProfileId);

    @Query("""
            select r from CrawlRunEntity r
            join fetch r.project p
            join fetch r.authProfile ap
            join fetch ap.project
            where r.id = :runId
            """)
    Optional<CrawlRunEntity> findByIdWithRelations(@Param("runId") UUID runId);
}


