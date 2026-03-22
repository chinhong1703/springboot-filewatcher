package com.example.sftpwatcher.state;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "processed_remote_files", uniqueConstraints = {
        @UniqueConstraint(name = "ux_processed_remote_files_job_key", columnNames = {"job_name", "file_key"})
})
public class ProcessedRemoteFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 150)
    private String jobName;

    @Column(name = "server_ref", nullable = false, length = 120)
    private String serverRef;

    @Column(name = "remote_path", nullable = false, length = 1024)
    private String remotePath;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "file_key", nullable = false, length = 512)
    private String fileKey;

    @Column(name = "modified_time")
    private Instant modifiedTime;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    public Long getId() {
        return id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getServerRef() {
        return serverRef;
    }

    public void setServerRef(String serverRef) {
        this.serverRef = serverRef;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public Instant getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Instant modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
