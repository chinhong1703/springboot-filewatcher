package com.example.sftpwatcher.config;

import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.PostAction;
import com.example.sftpwatcher.processor.ProcessorRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

@Component
public class SftpConfigurationValidator {

    private final AppSftpProperties properties;
    private final ProcessorRegistry processorRegistry;

    public SftpConfigurationValidator(AppSftpProperties properties, ProcessorRegistry processorRegistry) {
        this.properties = properties;
        this.processorRegistry = processorRegistry;
    }

    public void validate() {
        List<String> errors = new ArrayList<>();
        validateScheduler(errors);
        properties.getJobs().forEach((jobName, job) -> {
            if (!properties.getServers().containsKey(job.getServerRef())) {
                errors.add("Job '%s' references unknown serverRef '%s'".formatted(jobName, job.getServerRef()));
            }
            if (job.getMode() == JobMode.READ) {
                validateReadJob(jobName, job, errors);
            } else if (job.getProcessorRef() != null && !job.getProcessorRef().isBlank()) {
                errors.add("WRITE job '%s' must not define processorRef".formatted(jobName));
            }
        });
        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
    }

    private void validateScheduler(List<String> errors) {
        Duration leaseDuration = properties.getScheduler().getLeaseDuration();
        Duration heartbeatInterval = properties.getScheduler().getHeartbeatInterval();
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            errors.add("Scheduler leaseDuration must be positive");
        }
        if (heartbeatInterval == null || heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            errors.add("Scheduler heartbeatInterval must be positive");
        }
        if (leaseDuration != null && heartbeatInterval != null && !leaseDuration.isNegative() && !heartbeatInterval.isNegative()
                && !heartbeatInterval.minus(leaseDuration).isNegative()) {
            errors.add("Scheduler heartbeatInterval must be less than leaseDuration");
        }
    }

    private void validateReadJob(String jobName, AppSftpProperties.JobProperties job, List<String> errors) {
        if (job.getSchedule() == null || job.getSchedule().isBlank()) {
            errors.add("READ job '%s' requires a schedule".formatted(jobName));
        } else {
            try {
                CronExpression.parse(job.getSchedule());
            } catch (IllegalArgumentException ex) {
                errors.add("READ job '%s' has invalid schedule '%s'".formatted(jobName, job.getSchedule()));
            }
        }
        if ((job.getExactFilename() == null || job.getExactFilename().isBlank())
                && (job.getFilePattern() == null || job.getFilePattern().isBlank())) {
            errors.add("READ job '%s' must define exactFilename or filePattern".formatted(jobName));
        }
        if (job.getSelectionStrategy() == null) {
            errors.add("READ job '%s' requires selectionStrategy".formatted(jobName));
        }
        if (job.getSelectionStrategy() == com.example.sftpwatcher.domain.SelectionStrategy.EXACT_NAME
                && (job.getExactFilename() == null || job.getExactFilename().isBlank())) {
            errors.add("READ job '%s' uses EXACT_NAME but exactFilename is missing".formatted(jobName));
        }
        if (job.getPostAction() == PostAction.MOVE
                && (job.getArchiveDirectory() == null || job.getArchiveDirectory().isBlank())) {
            errors.add("READ job '%s' uses MOVE but archiveDirectory is missing".formatted(jobName));
        }
        if (job.getProcessorRef() == null || job.getProcessorRef().isBlank()) {
            errors.add("READ job '%s' requires processorRef".formatted(jobName));
        } else {
            try {
                processorRegistry.resolve(job.getProcessorRef());
            } catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
            }
        }
    }
}
