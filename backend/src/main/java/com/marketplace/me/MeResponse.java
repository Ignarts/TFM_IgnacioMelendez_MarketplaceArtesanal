package com.marketplace.me;

import com.marketplace.user.User;

import java.util.Set;
import java.util.stream.Collectors;

public record MeResponse(Long id, String email, String name, Set<String> roles) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
