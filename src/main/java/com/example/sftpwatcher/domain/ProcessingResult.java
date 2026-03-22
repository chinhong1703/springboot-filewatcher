package com.example.sftpwatcher.domain;

public record ProcessingResult(
        boolean successful,
        String message
) {
    public static ProcessingResult success(String message) {
        return new ProcessingResult(true, message);
    }

    public static ProcessingResult failure(String message) {
        return new ProcessingResult(false, message);
    }
}
