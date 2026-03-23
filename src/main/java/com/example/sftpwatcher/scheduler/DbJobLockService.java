package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.state.JobExecutionLockEntity;
import com.example.sftpwatcher.state.JobExecutionLockRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbJobLockService implements JobLockService {

    private static final Logger log = LoggerFactory.getLogger(DbJobLockService.class);

    private final AppSftpProperties properties;
    private final JobExecutionLockRepository repository;
    private final NodeInstanceIdProvider nodeInstanceIdProvider;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public DbJobLockService(
            AppSftpProperties properties,
            JobExecutionLockRepository repository,
            NodeInstanceIdProvider nodeInstanceIdProvider,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.properties = properties;
        this.repository = repository;
        this.nodeInstanceIdProvider = nodeInstanceIdProvider;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<JobLockHandle> tryAcquire(String jobName) {
        Instant now = clock.instant();
        Instant lockedUntil = now.plus(properties.getScheduler().getLeaseDuration());
        String ownerId = nodeInstanceIdProvider.currentId();

        if (repository.acquireExpired(jobName, ownerId, lockedUntil, now, now, now) > 0) {
            return acquiredHandle(jobName, ownerId, lockedUntil);
        }

        try {
            JobExecutionLockEntity entity = new JobExecutionLockEntity();
            entity.setJobName(jobName);
            entity.setOwnerId(ownerId);
            entity.setLockedUntil(lockedUntil);
            entity.setLastHeartbeat(now);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setVersion(0L);
            repository.saveAndFlush(entity);
            return acquiredHandle(jobName, ownerId, lockedUntil);
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobLockSnapshot> currentLock(String jobName) {
        return repository.findById(jobName)
                .map(entity -> new JobLockSnapshot(entity.getJobName(), entity.getOwnerId(), entity.getLockedUntil()));
    }

    @Transactional
    boolean heartbeatInternal(String jobName, String ownerId) {
        Instant now = clock.instant();
        Instant lockedUntil = now.plus(properties.getScheduler().getLeaseDuration());
        boolean updated = repository.heartbeat(jobName, ownerId, lockedUntil, now, now) > 0;
        if (updated) {
            meterRegistry.counter("sftp.job.lock.heartbeat.success", "jobName", jobName).increment();
            log.atDebug()
                    .setMessage("Extended job lease")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ownerId", ownerId)
                    .addKeyValue("lockedUntil", lockedUntil)
                    .addKeyValue("leaseDuration", properties.getScheduler().getLeaseDuration())
                    .log();
        } else {
            meterRegistry.counter("sftp.job.lock.heartbeat.failure", "jobName", jobName).increment();
            log.atWarn()
                    .setMessage("Failed to extend job lease")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ownerId", ownerId)
                    .addKeyValue("leaseDuration", properties.getScheduler().getLeaseDuration())
                    .log();
        }
        return updated;
    }

    @Transactional
    void releaseInternal(String jobName, String ownerId) {
        Instant now = clock.instant();
        if (repository.release(jobName, ownerId, now, now) > 0) {
            meterRegistry.counter("sftp.job.lock.released", "jobName", jobName).increment();
            log.atDebug()
                    .setMessage("Released job lease")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ownerId", ownerId)
                    .addKeyValue("lockedUntil", now)
                    .log();
        } else {
            log.atWarn()
                    .setMessage("Failed to release job lease")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ownerId", ownerId)
                    .log();
        }
    }

    private Optional<JobLockHandle> acquiredHandle(String jobName, String ownerId, Instant lockedUntil) {
        meterRegistry.counter("sftp.job.lock.acquired", "jobName", jobName).increment();
        log.atInfo()
                .setMessage("Acquired job lease")
                .addKeyValue("jobName", jobName)
                .addKeyValue("ownerId", ownerId)
                .addKeyValue("lockedUntil", lockedUntil)
                .addKeyValue("leaseDuration", properties.getScheduler().getLeaseDuration())
                .log();
        return Optional.of(new DbJobLockHandle(this, jobName, ownerId, lockedUntil));
    }

    private record DbJobLockHandle(
            DbJobLockService service,
            String jobName,
            String ownerId,
            Instant lockedUntil
    ) implements JobLockHandle {

        @Override
        public boolean heartbeat() {
            return service.heartbeatInternal(jobName, ownerId);
        }

        @Override
        public void close() {
            service.releaseInternal(jobName, ownerId);
        }
    }
}
