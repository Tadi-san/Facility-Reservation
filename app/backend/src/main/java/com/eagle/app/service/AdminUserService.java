package com.eagle.app.service;

import com.eagle.app.dto.AdminUserCreateRequest;
import com.eagle.app.model.RoleName;
import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AdminUserService(UserRepository users,
                            PasswordEncoder encoder,
                            PasswordPolicyService passwordPolicyService,
                            AuditLogService auditLogService,
                            CurrentUserService currentUserService) {
        this.users = users;
        this.encoder = encoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    public User createUser(AdminUserCreateRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        passwordPolicyService.validate(req.password());
        User u = new User();
        u.username = req.username();
        u.email = req.email();
        u.passwordHash = encoder.encode(req.password());
        u.roleName = RoleName.valueOf(req.role().toUpperCase());
        u.staffContactInfo = req.staffContactInfo();
        User saved = users.save(u);
        auditLogService.log(currentUserService.requireCurrentUser(), "USER_CREATED", "User", String.valueOf(saved.id), "Created user " + saved.username + " with role " + saved.roleName);
        return saved;
    }
}
