package com.example.sftpwatcher.support;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.sftp.SftpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySftpClient implements SftpClient {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final Map<String, RemoteFileMetadata> metadata = new ConcurrentHashMap<>();

    public void putFile(String remotePath, byte[] content, Instant modifiedTime) {
        String filename = remotePath.substring(remotePath.lastIndexOf('/') + 1);
        files.put(remotePath, content);
        metadata.put(remotePath, new RemoteFileMetadata(remotePath, filename, modifiedTime, content.length));
    }

    @Override
    public List<RemoteFileMetadata> listFiles(String serverRef, String remoteDirectory) {
        return metadata.values().stream()
                .filter(file -> file.remotePath().startsWith(remoteDirectory + "/"))
                .sorted(Comparator.comparing(RemoteFileMetadata::remotePath))
                .toList();
    }

    @Override
    public byte[] readFile(String serverRef, String remotePath) {
        return files.get(remotePath);
    }

    @Override
    public void writeFile(String serverRef, String remoteDirectory, String filename, byte[] content) {
        putFile(SftpPathUtils.join(remoteDirectory, filename), content, Instant.now());
    }

    @Override
    public void moveFile(String serverRef, String sourcePath, String targetPath) {
        byte[] content = files.remove(sourcePath);
        RemoteFileMetadata source = metadata.remove(sourcePath);
        if (content != null && source != null) {
            putFile(targetPath, content, source.modifiedTime());
        }
    }

    @Override
    public void deleteFile(String serverRef, String remotePath) {
        files.remove(remotePath);
        metadata.remove(remotePath);
    }

    @Override
    public boolean exists(String serverRef, String remotePath) {
        return files.containsKey(remotePath);
    }
}
