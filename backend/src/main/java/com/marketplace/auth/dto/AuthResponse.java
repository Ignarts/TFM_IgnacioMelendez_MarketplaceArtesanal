package com.marketplace.auth.dto;

import java.util.Set;

public record AuthResponse(
        String token,
        String tipo,
        String email,
        Set<String> roles
) {
    public static AuthResponse of(String token, String email, Set<String> roles) {
        return new AuthResponse(token, "Bearer", email, roles);
    }
}
