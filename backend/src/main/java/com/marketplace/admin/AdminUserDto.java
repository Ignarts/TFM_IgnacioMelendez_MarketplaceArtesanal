package com.marketplace.admin;

import com.marketplace.user.Role;
import com.marketplace.user.User;

import java.time.Instant;
import java.util.Set;

public record AdminUserDto(Long id, String email, String name, Set<Role> roles, boolean suspended, Instant createdAt) {
    public static AdminUserDto from(User u) {
        return new AdminUserDto(u.getId(), u.getEmail(), u.getName(), u.getRoles(), u.isSuspended(), u.getCreatedAt());
    }
}
