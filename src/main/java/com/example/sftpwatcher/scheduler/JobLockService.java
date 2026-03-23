package com.example.sftpwatcher.scheduler;

import java.util.Optional;

public interface JobLockService {

    Optional<JobLockHandle> tryAcquire(String jobName);

    Optional<JobLockSnapshot> currentLock(String jobName);
}
