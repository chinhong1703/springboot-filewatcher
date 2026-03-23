package com.example.sftpwatcher.state;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobExecutionLockRepository extends JpaRepository<JobExecutionLockEntity, String> {

    @Modifying
    @Query("""
            update JobExecutionLockEntity j
               set j.ownerId = :ownerId,
                   j.lockedUntil = :lockedUntil,
                   j.lastHeartbeat = :lastHeartbeat,
                   j.updatedAt = :updatedAt,
                   j.version = j.version + 1
             where j.jobName = :jobName
               and j.lockedUntil <= :now
            """)
    int acquireExpired(
            @Param("jobName") String jobName,
            @Param("ownerId") String ownerId,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("lastHeartbeat") Instant lastHeartbeat,
            @Param("updatedAt") Instant updatedAt,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            update JobExecutionLockEntity j
               set j.lockedUntil = :lockedUntil,
                   j.lastHeartbeat = :lastHeartbeat,
                   j.updatedAt = :updatedAt,
                   j.version = j.version + 1
             where j.jobName = :jobName
               and j.ownerId = :ownerId
            """)
    int heartbeat(
            @Param("jobName") String jobName,
            @Param("ownerId") String ownerId,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("lastHeartbeat") Instant lastHeartbeat,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Query("""
            update JobExecutionLockEntity j
               set j.lockedUntil = :lockedUntil,
                   j.updatedAt = :updatedAt,
                   j.version = j.version + 1
             where j.jobName = :jobName
               and j.ownerId = :ownerId
            """)
    int release(
            @Param("jobName") String jobName,
            @Param("ownerId") String ownerId,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("updatedAt") Instant updatedAt
    );
}
