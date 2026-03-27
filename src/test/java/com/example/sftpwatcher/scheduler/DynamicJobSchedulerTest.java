package com.example.sftpwatcher.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.config.SftpConfigurationValidator;
import com.example.sftpwatcher.domain.JobMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;

class DynamicJobSchedulerTest {

    private Scheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = mock(Scheduler.class);
    }

    @Test
    void registersEnabledReadJobsOnly() throws Exception {
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

        SftpConfigurationValidator validator = new SftpConfigurationValidator(properties, processorRef -> payload -> null);
        DynamicJobScheduler dynamicJobScheduler = new DynamicJobScheduler(properties, validator, scheduler);

        dynamicJobScheduler.registerJobs();

        verify(scheduler).scheduleJob(org.mockito.ArgumentMatchers.any(org.quartz.JobDetail.class), org.mockito.ArgumentMatchers.any(org.quartz.Trigger.class));
    }

    @Test
    void reportsRegisteredJobsFromQuartzScheduler() throws Exception {
        when(scheduler.checkExists(JobKey.jobKey("read-job", "sftp-read-jobs"))).thenReturn(true);
        when(scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals("sftp-read-jobs")))
                .thenReturn(java.util.Set.of(JobKey.jobKey("read-job", "sftp-read-jobs")));

        DynamicJobScheduler dynamicJobScheduler = new DynamicJobScheduler(new AppSftpProperties(), mock(SftpConfigurationValidator.class), scheduler);

        assertThat(dynamicJobScheduler.isRegistered("read-job")).isTrue();
        assertThat(dynamicJobScheduler.registeredTaskCount()).isEqualTo(1);
    }

    @Test
    void convertsSpringStyleCronToQuartzSyntax() {
        assertThat(DynamicJobScheduler.toQuartzCron("0 */5 * * * *")).isEqualTo("0 */5 * * * ?");
    }
}
