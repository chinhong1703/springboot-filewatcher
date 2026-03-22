package com.example.sftpwatcher.support;

public final class SftpPathUtils {

    private SftpPathUtils() {
    }

    public static String join(String directory, String filename) {
        if (directory.endsWith("/")) {
            return directory + filename;
        }
        return directory + "/" + filename;
    }
}
