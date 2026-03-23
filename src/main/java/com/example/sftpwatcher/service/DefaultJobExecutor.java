package com.example.sftpwatcher.service;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.JobRunSummary;
import com.example.sftpwatcher.domain.PostAction;
import com.example.sftpwatcher.domain.ProcessingResult;
import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.domain.RemoteFilePayload;
import com.example.sftpwatcher.processor.ProcessorRegistry;
import com.example.sftpwatcher.sftp.SftpClient;
import com.example.sftpwatcher.state.ProcessedFileStore;
import com.example.sftpwatcher.support.SftpPathUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultJobExecutor.class);

    private final AppSftpProperties properties;
    private final SftpClient sftpClient;
    private final FileSelectionService fileSelectionService;
    private final ProcessorRegistry processorRegistry;
    private final ProcessedFileStore processedFileStore;
    private final JobStatusTracker jobStatusTracker;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public DefaultJobExecutor(
            AppSftpProperties properties,
            SftpClient sftpClient,
            FileSelectionService fileSelectionService,
            ProcessorRegistry processorRegistry,
            ProcessedFileStore processedFileStore,
            JobStatusTracker jobStatusTracker,
            MeterRegistry meterRegistry
    ) {
        this(properties, sftpClient, fileSelectionService, processorRegistry, processedFileStore, jobStatusTracker, meterRegistry, Clock.systemUTC());
    }

    DefaultJobExecutor(
            AppSftpProperties properties,
            SftpClient sftpClient,
            FileSelectionService fileSelectionService,
            ProcessorRegistry processorRegistry,
            ProcessedFileStore processedFileStore,
            JobStatusTracker jobStatusTracker,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.properties = properties;
        this.sftpClient = sftpClient;
        this.fileSelectionService = fileSelectionService;
        this.processorRegistry = processorRegistry;
        this.processedFileStore = processedFileStore;
        this.jobStatusTracker = jobStatusTracker;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    public JobRunSummary execute(String jobName) {
        AppSftpProperties.JobProperties job = requireReadJob(jobName);
        Instant startedAt = clock.instant();
        try {
            List<RemoteFileMetadata> discovered = sftpClient.listFiles(job.getServerRef(), job.getRemoteDirectory());
            meterRegistry.counter("sftp.files.discovered", "jobName", jobName).increment(discovered.size());
            List<RemoteFileMetadata> selected = fileSelectionService.select(job, discovered);
            meterRegistry.counter("sftp.files.selected", "jobName", jobName).increment(selected.size());

            int processed = 0;
            int skipped = 0;
            for (RemoteFileMetadata metadata : selected) {
                String fileKey = job.getIdempotencyKeyStrategy().buildKey(metadata);
                if (processedFileStore.isProcessed(jobName, fileKey)) {
                    skipped++;
                    meterRegistry.counter("sftp.files.skipped", "jobName", jobName).increment();
                    continue;
                }
                Timer.Sample sample = Timer.start(meterRegistry);
                try {
                    byte[] content = sftpClient.readFile(job.getServerRef(), metadata.remotePath());
                    ProcessingResult result = processorRegistry.resolve(job.getProcessorRef())
                            .process(new RemoteFilePayload(
                                    jobName,
                                    job.getServerRef(),
                                    metadata.remotePath(),
                                    metadata.filename(),
                                    metadata.modifiedTime(),
                                    metadata.size(),
                                    content
                            ));
                    if (!result.successful()) {
                        throw new IllegalStateException(result.message());
                    }
                    if (!processedFileStore.recordSuccessIfAbsent(jobName, job.getServerRef(), metadata, fileKey, clock.instant(), "SUCCESS")) {
                        skipped++;
                        meterRegistry.counter("sftp.files.skipped", "jobName", jobName).increment();
                        log.atInfo()
                                .setMessage("Skipped duplicate file after atomic processed-state check")
                                .addKeyValue("jobName", jobName)
                                .addKeyValue("serverRef", job.getServerRef())
                                .addKeyValue("remotePath", metadata.remotePath())
                                .addKeyValue("filename", metadata.filename())
                                .log();
                        continue;
                    }
                    applyPostAction(job, metadata);
                    processed++;
                    meterRegistry.counter("sftp.files.processed", "jobName", jobName).increment();
                } catch (RuntimeException ex) {
                    skipped++;
                    log.atError()
                            .setMessage("Failed processing remote file")
                            .addKeyValue("jobName", jobName)
                            .addKeyValue("serverRef", job.getServerRef())
                            .addKeyValue("remoteDirectory", job.getRemoteDirectory())
                            .addKeyValue("remotePath", metadata.remotePath())
                            .addKeyValue("filename", metadata.filename())
                            .setCause(ex)
                            .log();
                } finally {
                    sample.stop(meterRegistry.timer("sftp.file.process.duration", "jobName", jobName));
                }
            }

            JobRunSummary summary = new JobRunSummary(
                    jobName,
                    job.getServerRef(),
                    startedAt,
                    clock.instant(),
                    true,
                    discovered.size(),
                    selected.size(),
                    processed,
                    skipped,
                    "Completed"
            );
            meterRegistry.counter("sftp.job.run.success", "jobName", jobName).increment();
            jobStatusTracker.update(summary);
            return summary;
        } catch (RuntimeException ex) {
            meterRegistry.counter("sftp.job.run.failure", "jobName", jobName).increment();
            JobRunSummary summary = new JobRunSummary(
                    jobName,
                    job.getServerRef(),
                    startedAt,
                    clock.instant(),
                    false,
                    0,
                    0,
                    0,
                    0,
                    ex.getMessage()
            );
            jobStatusTracker.update(summary);
            log.atError()
                    .setMessage("Job execution failed")
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("serverRef", job.getServerRef())
                    .addKeyValue("remoteDirectory", job.getRemoteDirectory())
                    .setCause(ex)
                    .log();
            return summary;
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

    private void applyPostAction(AppSftpProperties.JobProperties job, RemoteFileMetadata metadata) {
        if (job.getPostAction() == PostAction.DELETE) {
            sftpClient.deleteFile(job.getServerRef(), metadata.remotePath());
        } else if (job.getPostAction() == PostAction.MOVE) {
            sftpClient.moveFile(
                    job.getServerRef(),
                    metadata.remotePath(),
                    SftpPathUtils.join(job.getArchiveDirectory(), metadata.filename())
            );
        }
    }
}
