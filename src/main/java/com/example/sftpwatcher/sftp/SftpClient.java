package com.example.sftpwatcher.sftp;

import com.example.sftpwatcher.domain.RemoteFileMetadata;
import java.util.List;

public interface SftpClient {

    List<RemoteFileMetadata> listFiles(String serverRef, String remoteDirectory);

    byte[] readFile(String serverRef, String remotePath);

    void writeFile(String serverRef, String remoteDirectory, String filename, byte[] content);

    void moveFile(String serverRef, String sourcePath, String targetPath);

    void deleteFile(String serverRef, String remotePath);

    boolean exists(String serverRef, String remotePath);
}
