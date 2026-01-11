package com.dubbi.statetrail.crawl.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlPageRepository extends JpaRepository<CrawlPageEntity, UUID> {
    @Query("select p from CrawlPageEntity p where p.crawlRun.id = :runId order by p.discoveredAt asc")
    List<CrawlPageEntity> findByRunId(@Param("runId") UUID runId);

    @Query("select p from CrawlPageEntity p where p.crawlRun.id = :runId and p.url = :url")
    Optional<CrawlPageEntity> findByRunIdAndUrl(@Param("runId") UUID runId, @Param("url") String url);
}


