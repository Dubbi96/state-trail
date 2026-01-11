package com.dubbi.statetrail.auth.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthProfileRepository extends JpaRepository<AuthProfileEntity, UUID> {
    @Query("select a from AuthProfileEntity a where a.project.id = :projectId order by a.name asc")
    List<AuthProfileEntity> findByProjectId(@Param("projectId") UUID projectId);
}


