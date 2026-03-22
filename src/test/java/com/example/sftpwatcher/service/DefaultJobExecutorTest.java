package com.example.sftpwatcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.PatternType;
import com.example.sftpwatcher.domain.PostAction;
import com.example.sftpwatcher.domain.ProcessingResult;
import com.example.sftpwatcher.processor.ProcessorRegistry;
import com.example.sftpwatcher.processor.RemoteFileProcessor;
import com.example.sftpwatcher.support.InMemoryProcessedFileStore;
import com.example.sftpwatcher.support.InMemorySftpClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultJobExecutorTest {

    @Test
    void executesJobMarksProcessedAndMovesFile() {
        AppSftpProperties properties = propertiesForReadJob(PostAction.MOVE);
        InMemorySftpClient sftpClient = new InMemorySftpClient();
        sftpClient.putFile("/in/file.csv", "hello".getBytes(StandardCharsets.UTF_8), Instant.parse("2024-01-01T00:00:00Z"));
        List<String> processedFiles = new ArrayList<>();
        RemoteFileProcessor processor = payload -> {
            processedFiles.add(payload.filename());
            return ProcessingResult.success("ok");
        };

        DefaultJobExecutor executor = new DefaultJobExecutor(
                properties,
                sftpClient,
                new FileSelectionService(),
                processorRef -> processor,
                new InMemoryProcessedFileStore(),
                new JobStatusTracker(),
                new SimpleMeterRegistry()
        );

        var summary = executor.execute("job1");

        assertThat(summary.successful()).isTrue();
        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(processedFiles).containsExactly("file.csv");
        assertThat(sftpClient.exists("server-a", "/archive/file.csv")).isTrue();
        assertThat(sftpClient.exists("server-a", "/in/file.csv")).isFalse();
    }

    @Test
    void skipsAlreadyProcessedFileOnSecondRun() {
        AppSftpProperties properties = propertiesForReadJob(PostAction.NONE);
        InMemorySftpClient sftpClient = new InMemorySftpClient();
        sftpClient.putFile("/in/file.csv", "hello".getBytes(StandardCharsets.UTF_8), Instant.parse("2024-01-01T00:00:00Z"));
        InMemoryProcessedFileStore store = new InMemoryProcessedFileStore();

        DefaultJobExecutor executor = new DefaultJobExecutor(
                properties,
                sftpClient,
                new FileSelectionService(),
                processorRef -> payload -> ProcessingResult.success("ok"),
                store,
                new JobStatusTracker(),
                new SimpleMeterRegistry()
        );

        executor.execute("job1");
        var second = executor.execute("job1");

        assertThat(second.skippedCount()).isEqualTo(1);
        assertThat(second.processedCount()).isZero();
    }

    @Test
    void deletesFileWhenConfigured() {
        AppSftpProperties properties = propertiesForReadJob(PostAction.DELETE);
        InMemorySftpClient sftpClient = new InMemorySftpClient();
        sftpClient.putFile("/in/file.csv", "hello".getBytes(StandardCharsets.UTF_8), Instant.parse("2024-01-01T00:00:00Z"));

        DefaultJobExecutor executor = new DefaultJobExecutor(
                properties,
                sftpClient,
                new FileSelectionService(),
                processorRef -> payload -> ProcessingResult.success("ok"),
                new InMemoryProcessedFileStore(),
                new JobStatusTracker(),
                new SimpleMeterRegistry()
        );

        executor.execute("job1");

        assertThat(sftpClient.exists("server-a", "/in/file.csv")).isFalse();
    }

    @Test
    void continuesWhenOneFileFails() {
        AppSftpProperties properties = propertiesForReadJob(PostAction.NONE);
        properties.getJobs().get("job1").setSelectionStrategy(com.example.sftpwatcher.domain.SelectionStrategy.ALL);
        properties.getJobs().get("job1").setFilePattern(".*\\.csv");
        properties.getJobs().get("job1").setPatternType(PatternType.REGEX);
        InMemorySftpClient sftpClient = new InMemorySftpClient();
        sftpClient.putFile("/in/fail.csv", "bad".getBytes(StandardCharsets.UTF_8), Instant.parse("2024-01-01T00:00:00Z"));
        sftpClient.putFile("/in/ok.csv", "good".getBytes(StandardCharsets.UTF_8), Instant.parse("2024-01-02T00:00:00Z"));

        ProcessorRegistry registry = processorRef -> payload -> payload.filename().equals("fail.csv")
                ? ProcessingResult.failure("boom")
                : ProcessingResult.success("ok");

        DefaultJobExecutor executor = new DefaultJobExecutor(
                properties,
                sftpClient,
                new FileSelectionService(),
                registry,
                new InMemoryProcessedFileStore(),
                new JobStatusTracker(),
                new SimpleMeterRegistry()
        );

        var summary = executor.execute("job1");

        assertThat(summary.processedCount()).isEqualTo(1);
        assertThat(summary.skippedCount()).isEqualTo(1);
    }

    private AppSftpProperties propertiesForReadJob(PostAction postAction) {
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
        job.setFilePattern("file\\.csv");
        job.setPatternType(PatternType.REGEX);
        job.setProcessorRef("processor");
        job.setPostAction(postAction);
        job.setArchiveDirectory("/archive");
        properties.getJobs().put("job1", job);
        return properties;
    }
}
