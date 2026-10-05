package com.marketplace.report;

import com.marketplace.report.dto.ReportRequest;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report inappropriate products or reviews to the admins")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a product or a review")
    public void create(@AuthenticationPrincipal UserDetails principal,
                       @Valid @RequestBody ReportRequest request) {
        User reporter = userService.getByEmail(principal.getUsername());
        reportService.create(reporter, request);
    }
}
