package com.example.sftpwatcher.scheduler;

import com.example.sftpwatcher.config.AppSftpProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PropertyBackedNodeInstanceIdProvider implements NodeInstanceIdProvider {

    private final String ownerId;

    public PropertyBackedNodeInstanceIdProvider(AppSftpProperties properties) {
        String configured = properties.getScheduler().getOwnerId();
        if (configured != null && !configured.isBlank()) {
            this.ownerId = configured;
            return;
        }
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            this.ownerId = hostname;
            return;
        }
        this.ownerId = UUID.randomUUID().toString();
    }

    @Override
    public String currentId() {
        return ownerId;
    }
}
