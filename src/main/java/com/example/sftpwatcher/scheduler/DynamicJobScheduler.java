package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.config.SftpConfigurationValidator;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.service.JobExecutor;
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
    private final JobExecutor jobExecutor;
    private final Map<String, java.util.concurrent.ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJobScheduler(
            AppSftpProperties properties,
            SftpConfigurationValidator validator,
            TaskScheduler taskScheduler,
            JobExecutor jobExecutor
    ) {
        this.properties = properties;
        this.validator = validator;
        this.taskScheduler = taskScheduler;
        this.jobExecutor = jobExecutor;
    }

    @PostConstruct
    public void registerJobs() {
        validator.validate();
        properties.getJobs().forEach((jobName, job) -> {
            if (Boolean.TRUE.equals(job.getEnabled()) && job.getMode() == JobMode.READ) {
                Runnable task = () -> jobExecutor.execute(jobName);
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
