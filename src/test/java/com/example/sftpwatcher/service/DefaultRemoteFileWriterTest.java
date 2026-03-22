package com.example.sftpwatcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.support.InMemorySftpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultRemoteFileWriterTest {

    @Test
    void uploadsUsingWriteJobPreset() {
        AppSftpProperties properties = new AppSftpProperties();
        AppSftpProperties.ServerProperties server = new AppSftpProperties.ServerProperties();
        server.setHost("localhost");
        server.setPort(22);
        server.setUsername("user");
        server.setPrivateKeyLocation("classpath:key.pem");
        properties.getServers().put("server-a", server);

        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setMode(JobMode.WRITE);
        job.setEnabled(true);
        job.setServerRef("server-a");
        job.setRemoteDirectory("/out");
        properties.getJobs().put("upload-job", job);

        InMemorySftpClient sftpClient = new InMemorySftpClient();
        DefaultRemoteFileWriter writer = new DefaultRemoteFileWriter(properties, sftpClient);
        writer.uploadByJob("upload-job", "report.csv", "hello".getBytes(StandardCharsets.UTF_8));

        assertThat(sftpClient.exists("server-a", "/out/report.csv")).isTrue();
    }
}
