package com.example.sftpwatcher.support;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.state.ProcessedFileStore;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class InMemoryProcessedFileStore implements ProcessedFileStore {

    private final Set<String> keys = new HashSet<>();

    @Override
    public boolean isProcessed(String jobName, String fileKey) {
        return keys.contains(jobName + "|" + fileKey);
    }

    @Override
    public boolean recordSuccessIfAbsent(String jobName, String serverRef, RemoteFileMetadata metadata, String fileKey, Instant processedAt, String status) {
        return keys.add(jobName + "|" + fileKey);
    }
}
