package com.example.sftpwatcher.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.config.SftpConfigurationValidator;
import com.example.sftpwatcher.domain.JobMode;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class DynamicJobSchedulerTest {

    @Test
    void registersEnabledReadJobsOnly() {
        AppSftpProperties properties = new AppSftpProperties();
        AppSftpProperties.ServerProperties server = new AppSftpProperties.ServerProperties();
        server.setHost("localhost");
        server.setPort(22);
        server.setUsername("user");
        server.setPrivateKeyLocation("classpath:key.pem");
        properties.getServers().put("server-a", server);

        AppSftpProperties.JobProperties readJob = new AppSftpProperties.JobProperties();
        readJob.setEnabled(true);
        readJob.setMode(JobMode.READ);
        readJob.setServerRef("server-a");
        readJob.setSchedule("0 */5 * * * *");
        readJob.setRemoteDirectory("/in");
        readJob.setFilePattern(".*");
        readJob.setProcessorRef("processor");
        properties.getJobs().put("read-job", readJob);

        AppSftpProperties.JobProperties writeJob = new AppSftpProperties.JobProperties();
        writeJob.setEnabled(true);
        writeJob.setMode(JobMode.WRITE);
        writeJob.setServerRef("server-a");
        writeJob.setRemoteDirectory("/out");
        properties.getJobs().put("write-job", writeJob);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        SftpConfigurationValidator validator = new SftpConfigurationValidator(properties, processorRef -> payload -> null);
        DynamicJobScheduler dynamicJobScheduler = new DynamicJobScheduler(properties, validator, scheduler, jobName -> null);

        dynamicJobScheduler.registerJobs();

        assertThat(dynamicJobScheduler.isRegistered("read-job")).isTrue();
        assertThat(dynamicJobScheduler.isRegistered("write-job")).isFalse();
        scheduler.shutdown();
    }
}
