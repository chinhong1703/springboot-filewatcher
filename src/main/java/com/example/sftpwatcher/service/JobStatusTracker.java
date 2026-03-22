package com.example.sftpwatcher.service;

import com.example.sftpwatcher.domain.JobRunSummary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class JobStatusTracker {

    private final Map<String, JobRunSummary> summaries = new ConcurrentHashMap<>();

    public void update(JobRunSummary summary) {
        summaries.put(summary.jobName(), summary);
    }

    public Optional<JobRunSummary> get(String jobName) {
        return Optional.ofNullable(summaries.get(jobName));
    }
}
