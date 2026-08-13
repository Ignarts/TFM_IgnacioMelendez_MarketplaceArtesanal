package com.marketplace.me;

import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Me", description = "Datos del usuario autenticado")
public class MeController {

    private final UserService userService;
    private final ProfileStatsService profileStatsService;

    public MeController(UserService userService, ProfileStatsService profileStatsService) {
        this.userService = userService;
        this.profileStatsService = profileStatsService;
    }

    @GetMapping
    @Operation(summary = "Obtener datos del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public MeResponse me(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow();
        return MeResponse.from(user);
    }

    @GetMapping("/profile-stats")
    @Operation(summary = "KPIs agregados del perfil del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public ProfileStatsResponse profileStats(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow();
        return profileStatsService.forUser(user);
    }
}
