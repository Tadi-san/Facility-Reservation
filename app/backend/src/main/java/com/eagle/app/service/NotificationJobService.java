package com.eagle.app.service;

import com.eagle.app.model.NotificationJob;
import com.eagle.app.model.NotificationJobStatus;
import com.eagle.app.model.User;
import com.eagle.app.repository.NotificationJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class NotificationJobService {
    private static final String REFRESH_JOB = "REFRESH_NOTIFICATIONS";
    private static final String SEARCH_REINDEX_JOB = "REBUILD_SEARCH_INDEX";

    private final NotificationJobRepository jobs;
    private final NotificationService notifications;
    private final SearchIndexService searchIndexService;
    private final AuditLogService auditLogService;

    private int consecutiveFailures = 0;
    private Instant circuitOpenUntil;

    public NotificationJobService(NotificationJobRepository jobs,
                                  NotificationService notifications,
                                  SearchIndexService searchIndexService,
                                  AuditLogService auditLogService) {
        this.jobs = jobs;
        this.notifications = notifications;
        this.searchIndexService = searchIndexService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> enqueueRefresh(User actor) {
        NotificationJob saved = enqueue(REFRESH_JOB);
        auditLogService.log(actor, "NOTIFICATION_REFRESH_QUEUED", "NotificationJob", String.valueOf(saved.id), "Queued refresh job");
        return Map.of("queued", 1, "processed", 0, "jobId", saved.id, "mode", "async");
    }

    @Transactional
    public Map<String, Object> enqueueSearchReindex(User actor) {
        NotificationJob saved = enqueue(SEARCH_REINDEX_JOB);
        auditLogService.log(actor, "SEARCH_REINDEX_QUEUED", "NotificationJob", String.valueOf(saved.id), "Queued search reindex job");
        return Map.of("queued", 1, "processed", 0, "jobId", saved.id, "mode", "async", "type", "SEARCH_REINDEX");
    }

    @Scheduled(fixedDelay = 5000L)
    @Transactional
    public void processQueue() {
        if (circuitOpenUntil != null && circuitOpenUntil.isAfter(Instant.now())) {
            return;
        }
        List<NotificationJob> runnable = jobs.findRunnable(List.of(NotificationJobStatus.PENDING, NotificationJobStatus.FAILED), Instant.now());
        for (NotificationJob job : runnable) {
            runJob(job);
        }
    }

    private NotificationJob enqueue(String type) {
        NotificationJob job = new NotificationJob();
        job.jobType = type;
        job.payloadJson = "{}";
        job.status = NotificationJobStatus.PENDING;
        return jobs.save(job);
    }

    private void runJob(NotificationJob job) {
        job.status = NotificationJobStatus.PROCESSING;
        jobs.save(job);
        try {
            if (REFRESH_JOB.equals(job.jobType)) {
                notifications.queueTemplateBanners();
                notifications.processPendingBanners();
            } else if (SEARCH_REINDEX_JOB.equals(job.jobType)) {
                searchIndexService.rebuildIndex();
            }
            job.status = NotificationJobStatus.DONE;
            job.lastError = null;
            jobs.save(job);
            consecutiveFailures = 0;
        } catch (Exception ex) {
            job.status = NotificationJobStatus.FAILED;
            job.attempts++;
            job.lastError = ex.getMessage();
            job.nextAttemptAt = Instant.now().plusSeconds(Math.min(120, 5L * job.attempts));
            jobs.save(job);
            consecutiveFailures++;
            if (consecutiveFailures >= 3) {
                circuitOpenUntil = Instant.now().plusSeconds(60);
                consecutiveFailures = 0;
            }
        }
    }
}
