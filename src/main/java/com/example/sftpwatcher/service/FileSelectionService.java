package com.example.sftpwatcher.service;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.PatternType;
import com.example.sftpwatcher.domain.RemoteFileMetadata;
import com.example.sftpwatcher.domain.SelectionStrategy;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class FileSelectionService {

    public List<RemoteFileMetadata> select(AppSftpProperties.JobProperties job, List<RemoteFileMetadata> candidates) {
        List<RemoteFileMetadata> filtered = candidates.stream()
                .filter(file -> matches(job, file))
                .toList();

        if (job.getSelectionStrategy() == SelectionStrategy.LATEST) {
            return filtered.stream()
                    .max(Comparator.comparing(RemoteFileMetadata::modifiedTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .stream()
                    .toList();
        }
        if (job.getSelectionStrategy() == SelectionStrategy.EXACT_NAME) {
            return filtered.stream()
                    .filter(file -> Objects.equals(file.filename(), job.getExactFilename()))
                    .findFirst()
                    .stream()
                    .toList();
        }
        return filtered;
    }

    private boolean matches(AppSftpProperties.JobProperties job, RemoteFileMetadata file) {
        if (job.getExactFilename() != null && !job.getExactFilename().isBlank()) {
            return job.getExactFilename().equals(file.filename());
        }
        if (job.getFilePattern() == null || job.getFilePattern().isBlank()) {
            return true;
        }
        if (job.getPatternType() == PatternType.GLOB) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + job.getFilePattern());
            return matcher.matches(java.nio.file.Path.of(file.filename()));
        }
        return Pattern.compile(job.getFilePattern()).matcher(file.filename()).matches();
    }
}
