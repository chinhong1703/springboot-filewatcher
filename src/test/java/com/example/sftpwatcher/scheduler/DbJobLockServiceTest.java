package com.example.sftpwatcher.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.state.JobExecutionLockRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DbJobLockServiceTest.Config.class)
class DbJobLockServiceTest {

    @Autowired
    private DbJobLockService service;

    @Autowired
    private JobExecutionLockRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void acquiresLockWhenRowDoesNotExist() {
        assertThat(service.tryAcquire("job1")).isPresent();
        assertThat(repository.findById("job1")).isPresent();
    }

    @Test
    void preventsSecondOwnerFromAcquiringActiveLock() {
        assertThat(service.tryAcquire("job1")).isPresent();
        DbJobLockService second = new DbJobLockService(
                properties(),
                repository,
                () -> "node-2",
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(second.tryAcquire("job1")).isEmpty();
    }

    @Test
    void allowsExpiredLockToBeTakenOverByAnotherOwner() {
        assertThat(service.tryAcquire("job1")).isPresent();
        DbJobLockService second = new DbJobLockService(
                properties(),
                repository,
                () -> "node-2",
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:04:00Z"), ZoneOffset.UTC)
        );

        assertThat(second.tryAcquire("job1")).isPresent();
        entityManager.clear();
        assertThat(repository.findById("job1")).get()
                .extracting(lock -> lock.getOwnerId())
                .isEqualTo("node-2");
    }

    @Test
    void releasesLockBySettingLockedUntilNow() {
        JobLockHandle handle = service.tryAcquire("job1").orElseThrow();
        handle.close();

        entityManager.clear();
        assertThat(repository.findById("job1")).get()
                .extracting(lock -> lock.getLockedUntil())
                .isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void heartbeatsOnlyForCurrentOwner() {
        JobLockHandle handle = service.tryAcquire("job1").orElseThrow();

        assertThat(handle.heartbeat()).isTrue();
        DbJobLockService second = new DbJobLockService(
                properties(),
                repository,
                () -> "node-2",
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:00:30Z"), ZoneOffset.UTC)
        );

        assertThat(second.tryAcquire("job1")).isEmpty();
    }

    @Test
    void preservesCreatedAtAndAdvancesUpdatedAtOnTakeover() {
        assertThat(service.tryAcquire("job1")).isPresent();
        var before = repository.findById("job1").orElseThrow();
        DbJobLockService second = new DbJobLockService(
                properties(),
                repository,
                () -> "node-2",
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2024-01-01T00:04:00Z"), ZoneOffset.UTC)
        );

        second.tryAcquire("job1");

        entityManager.clear();
        var after = repository.findById("job1").orElseThrow();
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUpdatedAt()).isAfter(before.getUpdatedAt());
    }

    @TestConfiguration
    static class Config {
        @Bean
        AppSftpProperties appSftpProperties() {
            return properties();
        }

        @Bean
        NodeInstanceIdProvider nodeInstanceIdProvider() {
            return () -> "node-1";
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        DbJobLockService dbJobLockService(
                AppSftpProperties properties,
                JobExecutionLockRepository repository,
                NodeInstanceIdProvider nodeInstanceIdProvider,
                SimpleMeterRegistry meterRegistry,
                Clock clock
        ) {
            return new DbJobLockService(properties, repository, nodeInstanceIdProvider, meterRegistry, clock);
        }
    }

    private static AppSftpProperties properties() {
        AppSftpProperties properties = new AppSftpProperties();
        properties.getScheduler().setLeaseDuration(Duration.ofMinutes(3));
        properties.getScheduler().setHeartbeatInterval(Duration.ofSeconds(45));
        return properties;
    }
}
