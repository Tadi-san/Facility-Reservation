package com.eagle.app.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {
    public void validate(String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters long");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUpper = true;
            else if (Character.isLowerCase(ch)) hasLower = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, digit, and special character");
        }
    }
}
