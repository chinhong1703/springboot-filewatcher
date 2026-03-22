package com.example.sftpwatcher.config;

import com.example.sftpwatcher.domain.IdempotencyKeyStrategy;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.PatternType;
import com.example.sftpwatcher.domain.PostAction;
import com.example.sftpwatcher.domain.SelectionStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.sftp")
@Validated
public class AppSftpProperties {

    @Valid
    private Map<String, ServerProperties> servers = new LinkedHashMap<>();

    @Valid
    private Map<String, JobProperties> jobs = new LinkedHashMap<>();

    public Map<String, ServerProperties> getServers() {
        return servers;
    }

    public void setServers(Map<String, ServerProperties> servers) {
        this.servers = servers;
    }

    public Map<String, JobProperties> getJobs() {
        return jobs;
    }

    public void setJobs(Map<String, JobProperties> jobs) {
        this.jobs = jobs;
    }

    public static class ServerProperties {
        @NotBlank
        private String host;
        @NotNull
        @Min(1)
        private Integer port = 22;
        @NotBlank
        private String username;
        @NotBlank
        private String privateKeyLocation;
        private String knownHostsLocation;
        @NotNull
        @Min(1)
        private Integer connectTimeoutMs = 10000;
        @NotNull
        @Min(1)
        private Integer sessionTimeoutMs = 10000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPrivateKeyLocation() {
            return privateKeyLocation;
        }

        public void setPrivateKeyLocation(String privateKeyLocation) {
            this.privateKeyLocation = privateKeyLocation;
        }

        public String getKnownHostsLocation() {
            return knownHostsLocation;
        }

        public void setKnownHostsLocation(String knownHostsLocation) {
            this.knownHostsLocation = knownHostsLocation;
        }

        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public Integer getSessionTimeoutMs() {
            return sessionTimeoutMs;
        }

        public void setSessionTimeoutMs(Integer sessionTimeoutMs) {
            this.sessionTimeoutMs = sessionTimeoutMs;
        }
    }

    public static class JobProperties {
        @NotNull
        private Boolean enabled = Boolean.TRUE;
        @NotNull
        private JobMode mode;
        @NotBlank
        private String serverRef;
        private String schedule;
        @NotBlank
        private String remoteDirectory;
        private String filePattern;
        private PatternType patternType = PatternType.REGEX;
        private String exactFilename;
        private SelectionStrategy selectionStrategy = SelectionStrategy.ALL;
        private String processorRef;
        private PostAction postAction = PostAction.NONE;
        private String archiveDirectory;
        private IdempotencyKeyStrategy idempotencyKeyStrategy = IdempotencyKeyStrategy.PATH_SIZE_MTIME;
        private String filenameTemplate;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public JobMode getMode() {
            return mode;
        }

        public void setMode(JobMode mode) {
            this.mode = mode;
        }

        public String getServerRef() {
            return serverRef;
        }

        public void setServerRef(String serverRef) {
            this.serverRef = serverRef;
        }

        public String getSchedule() {
            return schedule;
        }

        public void setSchedule(String schedule) {
            this.schedule = schedule;
        }

        public String getRemoteDirectory() {
            return remoteDirectory;
        }

        public void setRemoteDirectory(String remoteDirectory) {
            this.remoteDirectory = remoteDirectory;
        }

        public String getFilePattern() {
            return filePattern;
        }

        public void setFilePattern(String filePattern) {
            this.filePattern = filePattern;
        }

        public PatternType getPatternType() {
            return patternType;
        }

        public void setPatternType(PatternType patternType) {
            this.patternType = patternType;
        }

        public String getExactFilename() {
            return exactFilename;
        }

        public void setExactFilename(String exactFilename) {
            this.exactFilename = exactFilename;
        }

        public SelectionStrategy getSelectionStrategy() {
            return selectionStrategy;
        }

        public void setSelectionStrategy(SelectionStrategy selectionStrategy) {
            this.selectionStrategy = selectionStrategy;
        }

        public String getProcessorRef() {
            return processorRef;
        }

        public void setProcessorRef(String processorRef) {
            this.processorRef = processorRef;
        }

        public PostAction getPostAction() {
            return postAction;
        }

        public void setPostAction(PostAction postAction) {
            this.postAction = postAction;
        }

        public String getArchiveDirectory() {
            return archiveDirectory;
        }

        public void setArchiveDirectory(String archiveDirectory) {
            this.archiveDirectory = archiveDirectory;
        }

        public IdempotencyKeyStrategy getIdempotencyKeyStrategy() {
            return idempotencyKeyStrategy;
        }

        public void setIdempotencyKeyStrategy(IdempotencyKeyStrategy idempotencyKeyStrategy) {
            this.idempotencyKeyStrategy = idempotencyKeyStrategy;
        }

        public String getFilenameTemplate() {
            return filenameTemplate;
        }

        public void setFilenameTemplate(String filenameTemplate) {
            this.filenameTemplate = filenameTemplate;
        }
    }
}
