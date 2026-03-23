package com.example.sftpwatcher.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sftpwatcher.config.AppSftpProperties;
import com.example.sftpwatcher.domain.JobMode;
import com.example.sftpwatcher.domain.JobRunSummary;
import com.example.sftpwatcher.service.JobCoordinator;
import com.example.sftpwatcher.service.JobStatusTracker;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminJobController.class)
@Import(AdminJobControllerTest.TestConfig.class)
class AdminJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobStatusTracker jobStatusTracker;

    @MockBean
    private JobCoordinator jobCoordinator;

    @Test
    void listsConfiguredJobs() throws Exception {
        mockMvc.perform(get("/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobName").value("job1"));
    }

    @Test
    void triggersJobManually() throws Exception {
        when(jobCoordinator.run("job1")).thenReturn(new JobRunSummary(
                "job1", "server-a", Instant.now(), Instant.now(), true, 1, 1, 1, 0, "ok"
        ));

        mockMvc.perform(post("/admin/jobs/job1/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("job1"))
                .andExpect(jsonPath("$.successful").value(true));
    }

    @Test
    void returnsLastStatus() throws Exception {
        jobStatusTracker.update(new JobRunSummary(
                "job1", "server-a", Instant.now(), Instant.now(), true, 1, 1, 1, 0, "ok"
        ));

        mockMvc.perform(get("/admin/jobs/job1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("job1"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        AppSftpProperties appSftpProperties() {
            AppSftpProperties properties = new AppSftpProperties();
            AppSftpProperties.JobProperties job = new AppSftpProperties.JobProperties();
            job.setEnabled(true);
            job.setMode(JobMode.READ);
            job.setServerRef("server-a");
            job.setSchedule("0 */5 * * * *");
            job.setRemoteDirectory("/in");
            job.setProcessorRef("processor");
            properties.getJobs().put("job1", job);
            return properties;
        }

        @Bean
        JobStatusTracker jobStatusTracker() {
            return new JobStatusTracker();
        }
    }
}
