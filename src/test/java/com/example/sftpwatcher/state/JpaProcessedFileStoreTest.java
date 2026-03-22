package com.example.sftpwatcher.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaProcessedFileStore.class)
class JpaProcessedFileStoreTest {

    @Autowired
    private JpaProcessedFileStore store;

    @Test
    void persistsAndFindsProcessedFiles() {
        RemoteFileMetadata metadata = new RemoteFileMetadata("/in/file.csv", "file.csv", Instant.parse("2024-01-01T00:00:00Z"), 10);

        store.markProcessed("job1", "server-a", metadata, "key1", Instant.now(), "SUCCESS");

        assertThat(store.hasProcessed("job1", "key1")).isTrue();
    }
}
