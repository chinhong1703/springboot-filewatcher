package com.example.sftpwatcher.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.JobRunSummary;
import com.example.sftpwatcher.service.JobCoordinator;
import com.example.sftpwatcher.service.JobStatusTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class LeaseAwareJobCoordinatorTest {

    @Test
    void executesJobWhenLockAcquired() {
        AppSftpProperties properties = properties();
        AtomicInteger executions = new AtomicInteger();
        RecordingTaskScheduler scheduler = new RecordingTaskScheduler();
        JobCoordinator coordinator = new com.example.sftpwatcher.service.LeaseAwareJobCoordinator(
                properties,
                jobName -> {
                    executions.incrementAndGet();
                    return summary(jobName, "Completed");
                },
                new FixedJobLockService(true),
                new JobStatusTracker(),
                scheduler,
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
        );

        coordinator.run("job1");

        assertThat(executions.get()).isEqualTo(1);
        assertThat(scheduler.fixedRateScheduled.get()).isTrue();
        assertThat(scheduler.cancelled.get()).isTrue();
    }

    @Test
    void skipsJobWhenLockNotAcquired() {
        AppSftpProperties properties = properties();
        AtomicInteger executions = new AtomicInteger();
        JobCoordinator coordinator = new com.example.sftpwatcher.service.LeaseAwareJobCoordinator(
                properties,
                jobName -> {
                    executions.incrementAndGet();
                    return summary(jobName, "Completed");
                },
                new FixedJobLockService(false),
                new JobStatusTracker(),
                new RecordingTaskScheduler(),
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
        );

        JobRunSummary summary = coordinator.run("job1");

        assertThat(executions.get()).isZero();
        assertThat(summary.message()).contains("Skipped");
    }

    private AppSftpProperties properties() {
        AppSftpProperties properties = new AppSftpProperties();
        properties.getScheduler().setLeaseDuration(Duration.ofMinutes(3));
        properties.getScheduler().setHeartbeatInterval(Duration.ofSeconds(45));
        AppSftpProperties.ServerProperties server = new AppSftpProperties.ServerProperties();
        server.setHost("localhost");
        server.setPort(22);
        server.setUsername("user");
        server.setPrivateKeyLocation("classpath:key.pem");
        properties.getServers().put("server-a", server);

        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setEnabled(true);
        job.setMode(JobMode.READ);
        job.setServerRef("server-a");
        job.setSchedule("0 */5 * * * *");
        job.setRemoteDirectory("/in");
        job.setFilePattern(".*");
        job.setProcessorRef("processor");
        properties.getJobs().put("job1", job);
        return properties;
    }

    private JobRunSummary summary(String jobName, String message) {
        return new JobRunSummary(jobName, "server-a", Instant.now(), Instant.now(), true, 0, 0, 0, 0, message);
    }

    private static final class FixedJobLockService implements JobLockService {
        private final boolean acquired;

        private FixedJobLockService(boolean acquired) {
            this.acquired = acquired;
        }

        @Override
        public Optional<JobLockHandle> tryAcquire(String jobName) {
            if (!acquired) {
                return Optional.empty();
            }
            return Optional.of(new JobLockHandle() {
                @Override
                public String jobName() {
                    return jobName;
                }

                @Override
                public String ownerId() {
                    return "node-1";
                }

                @Override
                public Instant lockedUntil() {
                    return Instant.parse("2024-01-01T00:03:00Z");
                }

                @Override
                public boolean heartbeat() {
                    return true;
                }

                @Override
                public void close() {
                }
            });
        }

        @Override
        public Optional<JobLockSnapshot> currentLock(String jobName) {
            return Optional.of(new JobLockSnapshot(jobName, "node-2", Instant.parse("2024-01-01T00:03:00Z")));
        }
    }

    private static final class RecordingTaskScheduler implements TaskScheduler {
        private final AtomicBoolean fixedRateScheduled = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            fixedRateScheduled.set(true);
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            fixedRateScheduled.set(true);
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            fixedRateScheduled.set(true);
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.util.Date startTime, long period) {
            fixedRateScheduled.set(true);
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, java.util.Date startTime) {
            return new RecordingFuture(cancelled);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.util.Date startTime, long delay) {
            return new RecordingFuture(cancelled);
        }
    }

    private record RecordingFuture(AtomicBoolean cancelled) implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled.set(true);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return cancelled.get();
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
