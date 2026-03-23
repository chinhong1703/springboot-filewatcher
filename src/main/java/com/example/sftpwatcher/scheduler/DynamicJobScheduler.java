package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.config.SftpConfigurationValidator;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.service.JobCoordinator;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
public class DynamicJobScheduler {

    private final AppSftpProperties properties;
    private final SftpConfigurationValidator validator;
    private final TaskScheduler taskScheduler;
    private final JobCoordinator jobCoordinator;
    private final Map<String, java.util.concurrent.ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJobScheduler(
            AppSftpProperties properties,
            SftpConfigurationValidator validator,
            TaskScheduler taskScheduler,
            JobCoordinator jobCoordinator
    ) {
        this.properties = properties;
        this.validator = validator;
        this.taskScheduler = taskScheduler;
        this.jobCoordinator = jobCoordinator;
    }

    @PostConstruct
    public void registerJobs() {
        validator.validate();
        properties.getJobs().forEach((jobName, job) -> {
            if (Boolean.TRUE.equals(job.getEnabled()) && job.getMode() == JobMode.READ) {
                Runnable task = () -> jobCoordinator.run(jobName);
                scheduledTasks.put(jobName, taskScheduler.schedule(task, new CronTrigger(job.getSchedule())));
            }
        });
    }

    public boolean isRegistered(String jobName) {
        return scheduledTasks.containsKey(jobName);
    }

    public int registeredTaskCount() {
        return scheduledTasks.size();
    }
}
