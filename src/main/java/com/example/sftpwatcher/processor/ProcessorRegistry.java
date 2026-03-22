package com.example.sftpwatcher.processor;

public interface ProcessorRegistry {

    RemoteFileProcessor resolve(String processorRef);
}
