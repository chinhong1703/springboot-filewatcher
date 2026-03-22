package com.example.sftpwatcher.processor;

import com.example.sftpwatcher.domain.ProcessingResult;
import com.example.sftpwatcher.domain.RemoteFilePayload;
import org.springframework.stereotype.Component;

@Component("batchFile2Processor")
public class BatchFile2Processor implements RemoteFileProcessor {

    @Override
    public ProcessingResult process(RemoteFilePayload payload) {
        return ProcessingResult.success("Processed " + payload.filename());
    }
}
