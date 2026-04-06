package com.eagle.app.repository;

import com.eagle.app.model.NotificationJob;
import com.eagle.app.model.NotificationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {
    @Query("""
        SELECT j FROM NotificationJob j
        WHERE j.status IN :statuses
          AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now)
        ORDER BY j.createdAt ASC
    """)
    List<NotificationJob> findRunnable(@Param("statuses") List<NotificationJobStatus> statuses, @Param("now") Instant now);
}
