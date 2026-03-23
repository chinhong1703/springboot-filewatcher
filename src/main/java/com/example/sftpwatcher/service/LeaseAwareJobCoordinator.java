package com.example.sftpwatcher.service;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.JobRunSummary;
import com.example.sftpwatcher.scheduler.JobLockHandle;
import com.example.sftpwatcher.scheduler.JobLockService;
import com.example.sftpwatcher.scheduler.JobLockSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class LeaseAwareJobCoordinator implements JobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(LeaseAwareJobCoordinator.class);

    private final AppSftpProperties properties;
    private final JobExecutor jobExecutor;
    private final JobLockService jobLockService;
    private final JobStatusTracker jobStatusTracker;
    private final TaskScheduler taskScheduler;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public LeaseAwareJobCoordinator(
            AppSftpProperties properties,
            JobExecutor jobExecutor,
            JobLockService jobLockService,
            JobStatusTracker jobStatusTracker,
            TaskScheduler taskScheduler,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.properties = properties;
        this.jobExecutor = jobExecutor;
        this.jobLockService = jobLockService;
        this.jobStatusTracker = jobStatusTracker;
        this.taskScheduler = taskScheduler;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    public JobRunSummary run(String jobName) {
        AppSftpProperties.JobProperties job = requireReadJob(jobName);
        if (!Boolean.TRUE.equals(properties.getScheduler().getLockEnabled())) {
            return jobExecutor.execute(jobName);
        }

        Optional<JobLockHandle> maybeHandle = jobLockService.tryAcquire(jobName);
        if (maybeHandle.isEmpty()) {
            JobLockSnapshot snapshot = jobLockService.currentLock(jobName)
                    .orElse(new JobLockSnapshot(jobName, "unknown", clock.instant()));
            meterRegistry.counter("sftp.job.lock.skipped", "jobName", jobName).increment();
            log.atInfo()
                    .setMessage("Skipped job execution because lease is held by another node")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ownerId", snapshot.ownerId())
                    .addKeyValue("lockedUntil", snapshot.lockedUntil())
                    .addKeyValue("leaseDuration", properties.getScheduler().getLeaseDuration())
                    .log();
            JobRunSummary summary = new JobRunSummary(
                    jobName,
                    job.getServerRef(),
                    clock.instant(),
                    clock.instant(),
                    true,
                    0,
                    0,
                    0,
                    0,
                    "Skipped: job lease is held by " + snapshot.ownerId()
            );
            jobStatusTracker.update(summary);
            return summary;
        }

        JobLockHandle handle = maybeHandle.get();
        ScheduledFuture<?> heartbeatFuture = null;
        try (handle) {
            heartbeatFuture = taskScheduler.scheduleAtFixedRate(() -> handle.heartbeat(), properties.getScheduler().getHeartbeatInterval());
            return jobExecutor.execute(jobName);
        } finally {
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(true);
            }
        }
    }

    private AppSftpProperties.JobProperties requireReadJob(String jobName) {
        AppSftpProperties.JobProperties job = properties.getJobs().get(jobName);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job '%s'".formatted(jobName));
        }
        if (job.getMode() != JobMode.READ) {
            throw new IllegalArgumentException("Job '%s' is not a READ job".formatted(jobName));
        }
        return job;
    }
}
