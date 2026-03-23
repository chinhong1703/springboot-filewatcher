package com.example.sftpwatcher.service;

import com.example.sftpwatcher.domain.JobRunSummary;

public interface JobCoordinator {

    JobRunSummary run(String jobName);
}
