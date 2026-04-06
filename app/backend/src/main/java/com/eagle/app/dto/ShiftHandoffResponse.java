package com.eagle.app.dto;

import com.eagle.app.model.ShiftHandoff;
import java.time.Instant;

public record ShiftHandoffResponse(Long id, String fromUser, String toUser, Instant handoffTime, String summary, String pendingTasks) {
    public static ShiftHandoffResponse from(ShiftHandoff h) {
        return new ShiftHandoffResponse(h.id, h.fromUser.username, h.toUser.username, h.handoffTime, h.summary, h.pendingTasks);
    }
}
