package com.example.sftpwatcher.state;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import java.time.Instant;

public interface ProcessedFileStore {

    boolean isProcessed(String jobName, String fileKey);

    boolean recordSuccessIfAbsent(String jobName, String serverRef, RemoteFileMetadata metadata, String fileKey, Instant processedAt, String status);
}
