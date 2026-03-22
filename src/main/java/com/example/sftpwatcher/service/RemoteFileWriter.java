package com.example.sftpwatcher.service;

public interface RemoteFileWriter {

    void upload(String serverRef, String remoteDirectory, String filename, byte[] bytes);

    void uploadByJob(String jobName, String filename, byte[] bytes);
}
