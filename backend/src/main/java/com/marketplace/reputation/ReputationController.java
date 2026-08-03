package com.marketplace.reputation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}/reputation")
@Tag(name = "Reputation", description = "Shop reputation score and badges")
public class ReputationController {

    private final ReputationService reputationService;

    public ReputationController(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @GetMapping
    @Operation(summary = "Get reputation for a shop")
    public ResponseEntity<ReputationResponse> getReputation(@PathVariable Long shopId) {
        return reputationService.findByShopId(shopId)
                .map(ReputationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
