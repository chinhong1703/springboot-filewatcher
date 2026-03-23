package com.example.sftpwatcher.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.PatternType;
import com.example.sftpwatcher.domain.SelectionStrategy;
import com.example.sftpwatcher.processor.ProcessorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SftpConfigurationValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ProcessorRegistry.class, () -> processorRef -> payload -> null)
            .withBean(SftpConfigurationValidator.class);

    @Test
    void acceptsValidConfiguration() {
        contextRunner.withBean(AppSftpProperties.class, this::validProperties)
                .run(context -> {
                    SftpConfigurationValidator validator = context.getBean(SftpConfigurationValidator.class);
                    validator.validate();
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void rejectsMissingServerReference() {
        contextRunner.withBean(AppSftpProperties.class, () -> {
                    AppSftpProperties properties = validProperties();
                    properties.getJobs().get("job1").setServerRef("missing");
                    return properties;
                })
                .run(context -> {
                    SftpConfigurationValidator validator = context.getBean(SftpConfigurationValidator.class);
                    org.assertj.core.api.Assertions.assertThatThrownBy(validator::validate)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("unknown serverRef");
                });
    }

    @Test
    void rejectsInvalidHeartbeatConfiguration() {
        contextRunner.withBean(AppSftpProperties.class, () -> {
                    AppSftpProperties properties = validProperties();
                    properties.getScheduler().setLeaseDuration(java.time.Duration.ofSeconds(30));
                    properties.getScheduler().setHeartbeatInterval(java.time.Duration.ofSeconds(45));
                    return properties;
                })
                .run(context -> {
                    SftpConfigurationValidator validator = context.getBean(SftpConfigurationValidator.class);
                    org.assertj.core.api.Assertions.assertThatThrownBy(validator::validate)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("heartbeatInterval");
                });
    }

    private AppSftpProperties validProperties() {
        AppSftpProperties properties = new AppSftpProperties();
        AppSftpProperties.ServerProperties server = new AppSftpProperties.ServerProperties();
        server.setHost("localhost");
        server.setPort(22);
        server.setUsername("user");
        server.setPrivateKeyLocation("classpath:key.pem");
        properties.getServers().put("server-a", server);

        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setEnabled(true);
        job.setMode(JobMode.READ);
        job.setServerRef("server-a");
        job.setSchedule("0 */5 * * * *");
        job.setRemoteDirectory("/in");
        job.setFilePattern(".*\\.csv");
        job.setPatternType(PatternType.REGEX);
        job.setSelectionStrategy(SelectionStrategy.ALL);
        job.setProcessorRef("processor");
        properties.getJobs().put("job1", job);
        return properties;
    }
}
