package com.example.sftpwatcher.service;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.sftp.SftpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class DefaultRemoteFileWriter implements RemoteFileWriter {

    private final AppSftpProperties properties;
    private final SftpClient sftpClient;

    public DefaultRemoteFileWriter(AppSftpProperties properties, SftpClient sftpClient) {
        this.properties = properties;
        this.sftpClient = sftpClient;
    }

    @Override
    public void upload(String serverRef, String remoteDirectory, String filename, byte[] bytes) {
        sftpClient.writeFile(serverRef, remoteDirectory, filename, bytes);
    }

    @Override
    public void uploadByJob(String jobName, String filename, byte[] bytes) {
        AppSftpProperties.JobProperties job = requireJob(jobName);
        if (job.getMode() != JobMode.WRITE) {
            throw new IllegalArgumentException("Job '%s' is not a WRITE job".formatted(jobName));
        }
        String resolvedFilename = (filename == null || filename.isBlank())
                ? resolveFilename(job)
                : filename;
        upload(job.getServerRef(), job.getRemoteDirectory(), resolvedFilename, bytes);
    }

    private AppSftpProperties.JobProperties requireJob(String jobName) {
        AppSftpProperties.JobProperties job = properties.getJobs().get(jobName);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job '%s'".formatted(jobName));
        }
        return job;
    }

    private String resolveFilename(AppSftpProperties.JobProperties job) {
        if (job.getFilenameTemplate() == null || job.getFilenameTemplate().isBlank()) {
            throw new IllegalArgumentException("WRITE job '%s' requires filename or filenameTemplate".formatted(job.getRemoteDirectory()));
        }
        return job.getFilenameTemplate()
                .replace("{{yyyyMMddHHmmss}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
    }
}
