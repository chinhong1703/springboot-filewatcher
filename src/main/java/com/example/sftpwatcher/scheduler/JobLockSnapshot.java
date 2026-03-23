package com.example.sftpwatcher.scheduler;

import java.time.Instant;

public record JobLockSnapshot(
        String jobName,
        String ownerId,
        Instant lockedUntil
) {
}
