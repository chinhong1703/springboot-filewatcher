package com.example.sftpwatcher.domain;

import java.time.Instant;

public record JobRunSummary(
        String jobName,
        String serverRef,
        Instant startedAt,
        Instant finishedAt,
        boolean successful,
        int discoveredCount,
        int selectedCount,
        int processedCount,
        int skippedCount,
        String message
) {
}
