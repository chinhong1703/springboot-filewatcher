package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.config.SftpConfigurationValidator;
import com.example.sftpwatcher.domain.JobMode;
import jakarta.annotation.PostConstruct;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

@Component
public class DynamicJobScheduler {

    private final AppSftpProperties properties;
    private final SftpConfigurationValidator validator;
    private final Scheduler scheduler;

    public DynamicJobScheduler(
            AppSftpProperties properties,
            SftpConfigurationValidator validator,
            Scheduler scheduler
    ) {
        this.properties = properties;
        this.validator = validator;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void registerJobs() {
        validator.validate();
        properties.getJobs().forEach((jobName, job) -> {
            if (Boolean.TRUE.equals(job.getEnabled()) && job.getMode() == JobMode.READ) {
                try {
                    registerQuartzJob(jobName, job);
                } catch (SchedulerException ex) {
                    throw new IllegalStateException("Failed to register Quartz job " + jobName, ex);
                }
            }
        });
    }

    public boolean isRegistered(String jobName) {
        try {
            return scheduler.checkExists(jobKey(jobName));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to inspect Quartz scheduler", ex);
        }
    }

    public int registeredTaskCount() {
        try {
            return scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals("sftp-read-jobs")).size();
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to inspect Quartz scheduler", ex);
        }
    }

    private void registerQuartzJob(String jobName, AppSftpProperties.JobProperties job) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(QuartzReadJob.class)
                .withIdentity(jobKey(jobName))
                .usingJobData(new JobDataMap(java.util.Map.of(QuartzReadJob.JOB_NAME_KEY, jobName)))
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(jobName))
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(toQuartzCron(job.getSchedule())).withMisfireHandlingInstructionDoNothing())
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (ObjectAlreadyExistsException ex) {
            scheduler.addJob(jobDetail, true);
            scheduler.rescheduleJob(triggerKey(jobName), trigger);
        }
    }

    private JobKey jobKey(String jobName) {
        return JobKey.jobKey(jobName, "sftp-read-jobs");
    }

    private TriggerKey triggerKey(String jobName) {
        return TriggerKey.triggerKey(jobName + "-trigger", "sftp-read-triggers");
    }

    static String toQuartzCron(String expression) {
        String[] parts = expression.trim().split("\\s+");
        if (parts.length == 6 && "*".equals(parts[3]) && "*".equals(parts[5])) {
            parts[5] = "?";
            return String.join(" ", parts);
        }
        if (parts.length == 6 && !"*".equals(parts[3]) && !"*".equals(parts[5])
                && !"?".equals(parts[3]) && !"?".equals(parts[5])) {
            throw new IllegalArgumentException("Cron expression must leave either day-of-month or day-of-week unspecified for Quartz: " + expression);
        }
        return expression;
    }
}
