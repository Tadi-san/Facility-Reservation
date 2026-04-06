package com.eagle.app.controller;

import com.eagle.app.dto.AdminUserCreateRequest;
import com.eagle.app.dto.UserMaskedResponse;
import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import com.eagle.app.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserRepository users;
    private final AdminUserService admin;

    public UserController(UserRepository users, AdminUserService admin) {
        this.users = users;
        this.admin = admin;
    }

    @GetMapping
    public Page<UserMaskedResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(u -> new UserMaskedResponse(u.id, u.username, u.email, mask(u.staffContactInfo)));
    }

    @PostMapping
    public UserMaskedResponse create(@Valid @RequestBody AdminUserCreateRequest req) {
        User u = admin.createUser(req);
        return new UserMaskedResponse(u.id, u.username, u.email, mask(u.staffContactInfo));
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "";
        return "********";
    }
}
