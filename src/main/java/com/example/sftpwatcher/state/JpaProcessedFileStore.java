package com.example.sftpwatcher.state;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaProcessedFileStore implements ProcessedFileStore {

    private final ProcessedRemoteFileJpaRepository repository;

    public JpaProcessedFileStore(ProcessedRemoteFileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProcessed(String jobName, String fileKey) {
        return repository.existsByJobNameAndFileKey(jobName, fileKey);
    }

    @Override
    @Transactional
    public void markProcessed(String jobName, String serverRef, RemoteFileMetadata metadata, String fileKey, Instant processedAt, String status) {
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
        repository.save(entity);
    }
}
