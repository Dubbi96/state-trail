package com.dubbi.statetrail.crawl.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlLinkRepository extends JpaRepository<CrawlLinkEntity, UUID> {
    @Query("select e from CrawlLinkEntity e where e.crawlRun.id = :runId order by e.createdAt asc")
    List<CrawlLinkEntity> findByRunId(@Param("runId") UUID runId);
}


