package com.example.sftpwatcher.domain;

public record JobDescriptor(
        String jobName,
        boolean enabled,
        JobMode mode,
        String serverRef,
        String schedule,
        String remoteDirectory,
        String processorRef
) {
}
