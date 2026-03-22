package com.example.sftpwatcher.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sftpwatcher.domain.ProcessingResult;
import com.example.sftpwatcher.domain.RemoteFilePayload;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpringBeanProcessorRegistryTest {

    @Test
    void resolvesProcessorByBeanName() {
        RemoteFileProcessor processor = payload -> ProcessingResult.success("ok");
        SpringBeanProcessorRegistry registry = new SpringBeanProcessorRegistry(Map.of("sampleProcessor", processor));

        assertThat(registry.resolve("sampleProcessor")).isSameAs(processor);
    }

    @Test
    void failsForMissingProcessor() {
        SpringBeanProcessorRegistry registry = new SpringBeanProcessorRegistry(Map.of());

        assertThatThrownBy(() -> registry.resolve("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }
}
