package com.example.sftpwatcher.api;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobDescriptor;
import com.example.sftpwatcher.domain.JobRunSummary;
import com.example.sftpwatcher.service.JobCoordinator;
import com.example.sftpwatcher.service.JobStatusTracker;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/jobs")
public class AdminJobController {

    private final AppSftpProperties properties;
    private final JobCoordinator jobCoordinator;
    private final JobStatusTracker jobStatusTracker;

    public AdminJobController(AppSftpProperties properties, JobCoordinator jobCoordinator, JobStatusTracker jobStatusTracker) {
        this.properties = properties;
        this.jobCoordinator = jobCoordinator;
        this.jobStatusTracker = jobStatusTracker;
    }

    @GetMapping
    public List<JobDescriptor> listJobs() {
        return properties.getJobs().entrySet().stream()
                .map(entry -> new JobDescriptor(
                        entry.getKey(),
                        Boolean.TRUE.equals(entry.getValue().getEnabled()),
                        entry.getValue().getMode(),
                        entry.getValue().getServerRef(),
                        entry.getValue().getSchedule(),
                        entry.getValue().getRemoteDirectory(),
                        entry.getValue().getProcessorRef()
                ))
                .toList();
    }

    @PostMapping("/{jobName}/trigger")
    @ResponseStatus(HttpStatus.OK)
    public JobRunSummary trigger(@PathVariable String jobName) {
        if (!properties.getJobs().containsKey(jobName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown job");
        }
        return jobCoordinator.run(jobName);
    }

    @GetMapping("/{jobName}/status")
    public JobRunSummary status(@PathVariable String jobName) {
        return jobStatusTracker.get(jobName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No status for job"));
    }
}
