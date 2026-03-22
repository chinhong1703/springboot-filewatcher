package com.example.sftpwatcher.domain;

import java.time.Instant;

public record RemoteFileMetadata(
        String remotePath,
        String filename,
        Instant modifiedTime,
        long size
) {
}
