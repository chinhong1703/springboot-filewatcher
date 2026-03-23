package com.example.sftpwatcher.scheduler;

import java.time.Instant;

public interface JobLockHandle extends AutoCloseable {

    String jobName();

    String ownerId();

    Instant lockedUntil();

    boolean heartbeat();

    @Override
    void close();
}
