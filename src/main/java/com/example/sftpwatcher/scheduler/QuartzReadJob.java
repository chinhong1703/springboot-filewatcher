package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.service.JobCoordinator;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class QuartzReadJob extends QuartzJobBean {

    static final String JOB_NAME_KEY = "jobName";

    private JobCoordinator jobCoordinator;

    public void setJobCoordinator(JobCoordinator jobCoordinator) {
        this.jobCoordinator = jobCoordinator;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getMergedJobDataMap().getString(JOB_NAME_KEY);
        try {
            jobCoordinator.run(jobName);
        } catch (RuntimeException ex) {
            throw new JobExecutionException("Quartz job execution failed for " + jobName, ex, false);
        }
    }
}
