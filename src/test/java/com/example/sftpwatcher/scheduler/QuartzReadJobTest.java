package com.example.sftpwatcher.scheduler;

import static org.mockito.Mockito.verify;

import com.example.sftpwatcher.service.JobCoordinator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

class QuartzReadJobTest {

    @Test
    void dispatchesToCoordinatorUsingConfiguredJobName() throws Exception {
        JobCoordinator coordinator = Mockito.mock(JobCoordinator.class);
        QuartzReadJob quartzReadJob = new QuartzReadJob();
        quartzReadJob.setJobCoordinator(coordinator);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(java.util.Map.of(QuartzReadJob.JOB_NAME_KEY, "job1")));

        quartzReadJob.executeInternal(context);

        verify(coordinator).run("job1");
    }
}
