package com.marketplace.security;

import com.marketplace.user.Role;
import com.marketplace.user.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-key-that-is-long-enough-for-256-bits-hmac-sha", 86_400_000L);

    @Test
    void generatesAndValidatesToken() {
        User user = new User("ana@test.com", "hash", "Ana", Set.of(Role.BUYER));

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("ana@test.com", jwtService.extractEmail(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(jwtService.isTokenValid("not-a-real-token"));
    }
}
