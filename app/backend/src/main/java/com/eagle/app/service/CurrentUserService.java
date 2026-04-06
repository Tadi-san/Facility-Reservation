package com.eagle.app.service;

import com.eagle.app.model.RoleName;
import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user context is missing");
        }
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    public boolean hasAnyRole(User user, RoleName... roles) {
        for (RoleName role : roles) {
            if (user.roleName == role) return true;
        }
        return false;
    }
}
