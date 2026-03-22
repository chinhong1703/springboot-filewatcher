package com.example.sftpwatcher.processor;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SpringBeanProcessorRegistry implements ProcessorRegistry {

    private final Map<String, RemoteFileProcessor> processors;

    public SpringBeanProcessorRegistry(Map<String, RemoteFileProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public RemoteFileProcessor resolve(String processorRef) {
        RemoteFileProcessor processor = processors.get(processorRef);
        if (processor == null) {
            throw new IllegalArgumentException("Unknown processorRef '%s'".formatted(processorRef));
        }
        return processor;
    }
}
