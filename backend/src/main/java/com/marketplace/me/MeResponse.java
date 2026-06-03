package com.marketplace.me;

import com.marketplace.user.User;

import java.util.Set;
import java.util.stream.Collectors;

public record MeResponse(Long id, String email, String nombre, Set<String> roles) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNombre(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
