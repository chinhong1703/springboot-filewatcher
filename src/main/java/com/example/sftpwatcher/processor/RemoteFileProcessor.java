package com.example.sftpwatcher.processor;

import com.example.sftpwatcher.domain.ProcessingResult;
import com.example.sftpwatcher.domain.RemoteFilePayload;

public interface RemoteFileProcessor {

    ProcessingResult process(RemoteFilePayload payload);
}
