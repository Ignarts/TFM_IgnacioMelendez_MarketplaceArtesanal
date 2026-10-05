package com.marketplace.admin;

import com.marketplace.product.ProductService;
import com.marketplace.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Moderation queue: reported content and withdrawn products. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Back-office operations (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminModerationController {

    private final ReportService reportService;
    private final ProductService productService;

    public AdminModerationController(ReportService reportService, ProductService productService) {
        this.reportService = reportService;
        this.productService = productService;
    }

    // ── Reports ──────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    @Operation(summary = "List open reports, most recent first")
    public List<AdminReportDto> listReports() {
        return reportService.listOpen().stream().map(AdminReportDto::from).toList();
    }

    @PostMapping("/reports/{id}/dismiss")
    @Transactional
    @Operation(summary = "Dismiss a report without moderating the content")
    public AdminReportDto dismissReport(@PathVariable Long id) {
        return AdminReportDto.from(reportService.dismiss(id));
    }

    // ── Products ─────────────────────────────────────────────────────────────

    @GetMapping("/products/hidden")
    @Transactional(readOnly = true)
    @Operation(summary = "List products withdrawn from the catalog")
    public List<AdminProductDto> listHiddenProducts() {
        return productService.listHidden().stream().map(AdminProductDto::from).toList();
    }

    @PostMapping("/products/{id}/hide")
    @Transactional
    @Operation(summary = "Withdraw a reported product from the public catalog")
    public AdminProductDto hideProduct(@PathVariable Long id) {
        AdminProductDto product = AdminProductDto.from(productService.setHidden(id, true));
        reportService.resolveForProduct(id);
        return product;
    }

    @PostMapping("/products/{id}/restore")
    @Transactional
    @Operation(summary = "Put a withdrawn product back in the public catalog")
    public AdminProductDto restoreProduct(@PathVariable Long id) {
        return AdminProductDto.from(productService.setHidden(id, false));
    }
}
