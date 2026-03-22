package com.example.sftpwatcher.state;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import java.time.Instant;

public interface ProcessedFileStore {

    boolean hasProcessed(String jobName, String fileKey);

    void markProcessed(String jobName, String serverRef, RemoteFileMetadata metadata, String fileKey, Instant processedAt, String status);
}
