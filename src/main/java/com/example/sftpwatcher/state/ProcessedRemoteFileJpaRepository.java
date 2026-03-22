package com.example.sftpwatcher.state;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRemoteFileJpaRepository extends JpaRepository<ProcessedRemoteFileEntity, Long> {

    boolean existsByJobNameAndFileKey(String jobName, String fileKey);
}
