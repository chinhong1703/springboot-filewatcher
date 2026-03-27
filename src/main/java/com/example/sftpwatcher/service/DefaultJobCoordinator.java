package com.example.sftpwatcher.service;

import com.example.sftpwatcher.domain.JobRunSummary;
import org.springframework.stereotype.Service;

@Service
public class DefaultJobCoordinator implements JobCoordinator {

    private final JobExecutor jobExecutor;

    public DefaultJobCoordinator(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Override
    public JobRunSummary run(String jobName) {
        return jobExecutor.execute(jobName);
    }
}
