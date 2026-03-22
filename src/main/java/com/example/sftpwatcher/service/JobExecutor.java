package com.example.sftpwatcher.service;

import com.example.sftpwatcher.domain.JobRunSummary;

public interface JobExecutor {

    JobRunSummary execute(String jobName);
}
