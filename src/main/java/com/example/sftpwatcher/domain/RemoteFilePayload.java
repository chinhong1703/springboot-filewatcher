package com.example.sftpwatcher.domain;

import java.time.Instant;

public record RemoteFilePayload(
        String jobName,
        String serverRef,
        String remotePath,
        String filename,
        Instant modifiedTime,
        long size,
        byte[] content
) {
}
