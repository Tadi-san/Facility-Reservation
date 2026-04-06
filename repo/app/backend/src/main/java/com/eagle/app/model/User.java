package com.eagle.app.model;

import com.eagle.app.config.SensitiveDataConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends SyncableEntity {
    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RoleName roleName;

    @Convert(converter = SensitiveDataConverter.class)
    @Column(length = 255)
    public String staffContactInfo;

    @Column(nullable = false)
    public int failedAttempts = 0;

    public Instant lockUntil;
}
