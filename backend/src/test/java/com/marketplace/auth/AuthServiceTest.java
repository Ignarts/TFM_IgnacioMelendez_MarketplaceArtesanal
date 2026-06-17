package com.marketplace.auth;

import com.marketplace.auth.dto.RegisterRequest;
import com.marketplace.security.JwtService;
import com.marketplace.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        when(userService.existsByEmail("ana@test.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("ana@test.com", "password123", "Ana");

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userService, never()).save(any());
    }
}
