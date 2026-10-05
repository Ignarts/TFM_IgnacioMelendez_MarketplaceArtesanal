package com.marketplace.report;

import com.marketplace.support.FlowTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModerationFlowTests extends FlowTestSupport {

    private String reportBody(String type, long targetId, String reason) {
        return "{\"type\":\"" + type + "\",\"targetId\":" + targetId + ",\"reason\":\"" + reason + "\"}";
    }

    @Test
    void reportRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(APPLICATION_JSON).content(reportBody("PRODUCT", 1, "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReportProductOnlyOnceWhileOpen() throws Exception {
        String sellerToken = createSeller("Tienda reportada");
        long productId = createProduct(sellerToken, "Falsificación");
        String buyerToken = registerAndLogin(uniqueEmail("reporter"), "Reporter");

        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON).content(reportBody("PRODUCT", productId, "No es artesanal")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON).content(reportBody("PRODUCT", productId, "Otra vez")))
                .andExpect(status().isConflict());
    }

    @Test
    void reportingMissingContentReturns404() throws Exception {
        String buyerToken = registerAndLogin(uniqueEmail("reporter_404"), "Reporter");

        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON).content(reportBody("REVIEW", 999_999, "Spam")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminHidesReportedProductAndRestoresIt() throws Exception {
        String sellerToken = createSeller("Tienda moderada");
        long productId = createProduct(sellerToken, "Producto dudoso");
        String buyerToken = registerAndLogin(uniqueEmail("reporter_hide"), "Reporter");
        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(buyerToken))
                .contentType(APPLICATION_JSON).content(reportBody("PRODUCT", productId, "Contenido engañoso")));

        String adminToken = createAdminAndLogin();
        mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == " + productId + ")].reason").value("Contenido engañoso"));

        mockMvc.perform(post("/api/admin/products/" + productId + "/hide").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(true));

        // Withdrawn from the public catalog, its reports closed, still visible to its seller.
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products"))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) productId))));
        mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.productId == " + productId + ")]").isEmpty());
        mockMvc.perform(get("/api/admin/products/hidden").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.id == " + productId + ")]").isNotEmpty());
        mockMvc.perform(get("/api/seller/products").header(AUTHORIZATION, bearer(sellerToken)))
                .andExpect(jsonPath("$[?(@.id == " + productId + ")].hidden").value(true));

        mockMvc.perform(post("/api/admin/products/" + productId + "/restore").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(false));
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk());
    }

    @Test
    void adminDismissesReviewReport() throws Exception {
        String sellerToken = createSeller("Tienda reseñas");
        long productId = createProduct(sellerToken, "Jarrón");
        String buyerToken = registerAndLogin(uniqueEmail("reviewer"), "Reviewer");
        completePurchase(buyerToken, sellerToken, productId);
        long reviewId = createReview(buyerToken, productId, 1, "Horrible");

        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON).content(reportBody("REVIEW", reviewId, "Reseña ofensiva")))
                .andExpect(status().isCreated());

        String adminToken = createAdminAndLogin();
        String reports = mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.targetId == " + reviewId + " && @.type == 'REVIEW')].reviewComment")
                        .value("Horrible"))
                .andReturn().getResponse().getContentAsString();
        long reportId = -1;
        for (var node : mapper.readTree(reports)) {
            if (node.get("type").asText().equals("REVIEW") && node.get("targetId").asLong() == reviewId) {
                reportId = node.get("id").asLong();
            }
        }

        mockMvc.perform(post("/api/admin/reports/" + reportId + "/dismiss").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.id == " + reportId + ")]").isEmpty());
    }

    @Test
    void burstOfFiveStarReviewsFromOneBuyerIsFlaggedAutomatically() throws Exception {
        String sellerToken = createSeller("Tienda sospechosa");
        String buyerToken = registerAndLogin(uniqueEmail("burst"), "Burst");
        long lastReviewId = -1;
        for (int i = 1; i <= 3; i++) {
            long productId = createProduct(sellerToken, "Pieza " + i);
            completePurchase(buyerToken, sellerToken, productId);
            lastReviewId = createReview(buyerToken, productId, 5, "Perfecto");
        }

        String adminToken = createAdminAndLogin();
        mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.targetId == " + lastReviewId + " && @.type == 'REVIEW')].reporterName")
                        .value(ReportService.SYSTEM_REPORTER));
    }

    @Test
    void deletingReportedReviewAlsoRemovesItsReports() throws Exception {
        String sellerToken = createSeller("Tienda borrado");
        long productId = createProduct(sellerToken, "Bandeja");
        String buyerToken = registerAndLogin(uniqueEmail("reviewer_del"), "Reviewer");
        completePurchase(buyerToken, sellerToken, productId);
        long reviewId = createReview(buyerToken, productId, 1, "Spam");
        mockMvc.perform(post("/api/reports").header(AUTHORIZATION, bearer(sellerToken))
                .contentType(APPLICATION_JSON).content(reportBody("REVIEW", reviewId, "Spam")));

        String adminToken = createAdminAndLogin();
        mockMvc.perform(delete("/api/admin/reviews/" + reviewId).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/reports").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.targetId == " + reviewId + " && @.type == 'REVIEW')]").isEmpty());
    }
}
