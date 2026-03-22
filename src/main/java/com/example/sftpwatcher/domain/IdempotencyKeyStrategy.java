package com.example.sftpwatcher.domain;

import java.time.Instant;

public enum IdempotencyKeyStrategy {
    PATH_ONLY {
        @Override
        public String buildKey(RemoteFileMetadata file) {
            return file.remotePath();
        }
    },
    PATH_AND_MTIME {
        @Override
        public String buildKey(RemoteFileMetadata file) {
            return file.remotePath() + "|" + epoch(file.modifiedTime());
        }
    },
    PATH_SIZE_MTIME {
        @Override
        public String buildKey(RemoteFileMetadata file) {
            return file.remotePath() + "|" + file.size() + "|" + epoch(file.modifiedTime());
        }
    };

    public abstract String buildKey(RemoteFileMetadata file);

    private static long epoch(Instant value) {
        return value == null ? -1L : value.toEpochMilli();
    }
}
