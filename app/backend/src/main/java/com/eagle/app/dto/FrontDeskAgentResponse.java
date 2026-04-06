package com.eagle.app.dto;

import com.eagle.app.model.User;

public record FrontDeskAgentResponse(Long id, String username) {
    public static FrontDeskAgentResponse from(User u) {
        return new FrontDeskAgentResponse(u.id, u.username);
    }
}
