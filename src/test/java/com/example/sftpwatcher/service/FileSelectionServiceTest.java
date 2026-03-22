package com.example.sftpwatcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.PatternType;
import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.domain.SelectionStrategy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileSelectionServiceTest {

    private final FileSelectionService service = new FileSelectionService();

    @Test
    void selectsLatestMatchingRegex() {
        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setFilePattern("BATCH_.*\\.csv");
        job.setPatternType(PatternType.REGEX);
        job.setSelectionStrategy(SelectionStrategy.LATEST);

        List<RemoteFileMetadata> selected = service.select(job, List.of(
                new RemoteFileMetadata("/in/BATCH_1.csv", "BATCH_1.csv", Instant.parse("2024-01-01T00:00:00Z"), 10),
                new RemoteFileMetadata("/in/BATCH_2.csv", "BATCH_2.csv", Instant.parse("2024-01-02T00:00:00Z"), 10),
                new RemoteFileMetadata("/in/IGNORE.txt", "IGNORE.txt", Instant.parse("2024-01-03T00:00:00Z"), 10)
        ));

        assertThat(selected).extracting(RemoteFileMetadata::filename).containsExactly("BATCH_2.csv");
    }

    @Test
    void selectsExactNameWhenConfigured() {
        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setExactFilename("control.txt");
        job.setSelectionStrategy(SelectionStrategy.EXACT_NAME);

        List<RemoteFileMetadata> selected = service.select(job, List.of(
                new RemoteFileMetadata("/in/control.txt", "control.txt", Instant.now(), 10),
                new RemoteFileMetadata("/in/other.txt", "other.txt", Instant.now(), 10)
        ));

        assertThat(selected).extracting(RemoteFileMetadata::filename).containsExactly("control.txt");
    }

    @Test
    void supportsGlobPatterns() {
        AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
        job.setFilePattern("*.csv");
        job.setPatternType(PatternType.GLOB);
        job.setSelectionStrategy(SelectionStrategy.ALL);

        List<RemoteFileMetadata> selected = service.select(job, List.of(
                new RemoteFileMetadata("/in/a.csv", "a.csv", Instant.now(), 10),
                new RemoteFileMetadata("/in/a.txt", "a.txt", Instant.now(), 10)
        ));

        assertThat(selected).extracting(RemoteFileMetadata::filename).containsExactly("a.csv");
    }
}
