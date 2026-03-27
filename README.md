# SFTP Watcher

Config-driven Spring Boot 3 / Java 21 SFTP polling framework with dynamic scheduler registration, processor dispatch by Spring bean name, idempotent processing, and reusable upload APIs.

## Running

```bash
mvn test
```

## Adding a new server

Add a new named entry under `app.sftp.servers` in [application.yml](/Users/user/Documents/GitHub/springboot-sftp-design/src/main/resources/application.yml). Jobs reference that server through `server-ref`.

## Adding a new READ job

Add a new named entry under `app.sftp.jobs` with `mode: READ`, a cron `schedule`, selection settings, and a Spring processor bean name in `processor-ref`. Enabled READ jobs are registered programmatically at startup by the scheduler bootstrap.

## Adding a new WRITE job

Add a new named entry under `app.sftp.jobs` with `mode: WRITE`, `server-ref`, and `remote-directory`. Business services can then call `RemoteFileWriter.uploadByJob(jobName, filename, bytes)`.

## Implementing a processor bean

Create a Spring bean implementing `RemoteFileProcessor` and give it the bean name you want to reference from YAML, for example:

```java
@Component("batchFile1Processor")
class BatchFile1Processor implements RemoteFileProcessor { ... }
```

## Dynamic scheduling

The framework does not use per-job `@Scheduled` methods. `DynamicJobScheduler` scans enabled READ jobs during startup and registers a Quartz job plus cron trigger for each configured READ job.

Quartz is configured with a JDBC job store and clustered mode, so multiple service nodes can share the same scheduler state and only one node fires a given trigger at a time. Cron misfires are configured with `doNothing` so the scheduler does not backfill missed runs by default.
