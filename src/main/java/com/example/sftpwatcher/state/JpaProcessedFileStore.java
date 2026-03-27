package com.example.sftpwatcher.state;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaProcessedFileStore implements ProcessedFileStore {

    private final ProcessedRemoteFileJpaRepository repository;
    private final EntityManager entityManager;

    public JpaProcessedFileStore(ProcessedRemoteFileJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProcessed(String jobName, String fileKey) {
        return repository.existsByJobNameAndFileKey(jobName, fileKey);
    }

    @Override
    @Transactional
    public boolean recordSuccessIfAbsent(String jobName, String serverRef, RemoteFileMetadata metadata, String fileKey, Instant processedAt, String status) {
        try {
            ProcessedRemoteFileEntity entity = new ProcessedRemoteFileEntity();
            entity.setJobName(jobName);
            entity.setServerRef(serverRef);
            entity.setRemotePath(metadata.remotePath());
            entity.setFilename(metadata.filename());
            entity.setFileKey(fileKey);
            entity.setModifiedTime(metadata.modifiedTime());
            entity.setSizeBytes(metadata.size());
            entity.setProcessedAt(processedAt);
            entity.setStatus(status);
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException ex) {
            entityManager.clear();
            return false;
        }
    }
}
