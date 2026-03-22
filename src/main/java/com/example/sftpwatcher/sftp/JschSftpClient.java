package com.example.sftpwatcher.sftp;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.support.SftpPathUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Vector;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class JschSftpClient implements SftpClient {

    private final AppSftpProperties properties;
    private final ResourceLoader resourceLoader;

    public JschSftpClient(AppSftpProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public List<RemoteFileMetadata> listFiles(String serverRef, String remoteDirectory) {
        return execute(serverRef, channel -> {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channel.ls(remoteDirectory);
            return entries.stream()
                    .filter(entry -> !entry.getAttrs().isDir())
                    .map(entry -> toMetadata(remoteDirectory, entry))
                    .toList();
        });
    }

    @Override
    public byte[] readFile(String serverRef, String remotePath) {
        return execute(serverRef, channel -> {
            try (InputStream inputStream = channel.get(remotePath);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                inputStream.transferTo(outputStream);
                return outputStream.toByteArray();
            }
        });
    }

    @Override
    public void writeFile(String serverRef, String remoteDirectory, String filename, byte[] content) {
        execute(serverRef, channel -> {
            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                channel.put(inputStream, SftpPathUtils.join(remoteDirectory, filename));
            }
            return null;
        });
    }

    @Override
    public void moveFile(String serverRef, String sourcePath, String targetPath) {
        execute(serverRef, channel -> {
            channel.rename(sourcePath, targetPath);
            return null;
        });
    }

    @Override
    public void deleteFile(String serverRef, String remotePath) {
        execute(serverRef, channel -> {
            channel.rm(remotePath);
            return null;
        });
    }

    @Override
    public boolean exists(String serverRef, String remotePath) {
        return execute(serverRef, channel -> {
            try {
                channel.stat(remotePath);
                return true;
            } catch (SftpException ex) {
                return false;
            }
        });
    }

    private RemoteFileMetadata toMetadata(String remoteDirectory, ChannelSftp.LsEntry entry) {
        SftpATTRS attrs = entry.getAttrs();
        return new RemoteFileMetadata(
                SftpPathUtils.join(remoteDirectory, entry.getFilename()),
                entry.getFilename(),
                Instant.ofEpochSecond(attrs.getMTime()),
                attrs.getSize()
        );
    }

    private <T> T execute(String serverRef, SftpOperation<T> operation) {
        AppSftpProperties.ServerProperties server = properties.getServers().get(serverRef);
        if (server == null) {
            throw new IllegalArgumentException("Unknown serverRef '%s'".formatted(serverRef));
        }

        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(resourceLoader.getResource(server.getPrivateKeyLocation()).getFile().getAbsolutePath());
            if (server.getKnownHostsLocation() != null && !server.getKnownHostsLocation().isBlank()) {
                jsch.setKnownHosts(resourceLoader.getResource(server.getKnownHostsLocation()).getFile().getAbsolutePath());
            }
            session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());
            if (server.getKnownHostsLocation() == null || server.getKnownHostsLocation().isBlank()) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
            session.connect(server.getConnectTimeoutMs());
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(server.getSessionTimeoutMs());
            return operation.apply(channel);
        } catch (JSchException | SftpException | IOException ex) {
            throw new IllegalStateException("SFTP operation failed for server '%s'".formatted(serverRef), ex);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    @FunctionalInterface
    private interface SftpOperation<T> {
        T apply(ChannelSftp channel) throws SftpException, IOException;
    }
}
