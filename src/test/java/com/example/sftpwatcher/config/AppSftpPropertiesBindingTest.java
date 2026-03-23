package com.example.sftpwatcher.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.SelectionStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppSftpPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "app.sftp.scheduler.lock-enabled=true",
                    "app.sftp.scheduler.lease-duration=PT3M",
                    "app.sftp.scheduler.heartbeat-interval=PT45S",
                    "app.sftp.servers.server-a.host=sftp-a.example.com",
                    "app.sftp.servers.server-a.port=22",
                    "app.sftp.servers.server-a.username=myuser",
                    "app.sftp.servers.server-a.private-key-location=classpath:keys/server-a.pem",
                    "app.sftp.servers.server-a.connect-timeout-ms=10000",
                    "app.sftp.servers.server-a.session-timeout-ms=10000",
                    "app.sftp.jobs.job1.enabled=true",
                    "app.sftp.jobs.job1.mode=READ",
                    "app.sftp.jobs.job1.server-ref=server-a",
                    "app.sftp.jobs.job1.schedule=0 */5 * * * *",
                    "app.sftp.jobs.job1.remote-directory=/in",
                    "app.sftp.jobs.job1.file-pattern=.*\\\\.csv",
                    "app.sftp.jobs.job1.selection-strategy=LATEST",
                    "app.sftp.jobs.job1.processor-ref=batchFile1Processor"
            );

    @Test
    void bindsNestedServerAndJobMaps() {
        contextRunner.run(context -> {
            AppSftpProperties properties = context.getBean(AppSftpProperties.class);
            assertThat(properties.getServers().get("server-a").getHost()).isEqualTo("sftp-a.example.com");
            assertThat(properties.getJobs().get("job1").getMode()).isEqualTo(JobMode.READ);
            assertThat(properties.getJobs().get("job1").getSelectionStrategy()).isEqualTo(SelectionStrategy.LATEST);
            assertThat(properties.getScheduler().getLeaseDuration()).hasToString("PT3M");
        });
    }

    @Configuration
    @EnableConfigurationProperties(AppSftpProperties.class)
    static class TestConfig {
    }
}
