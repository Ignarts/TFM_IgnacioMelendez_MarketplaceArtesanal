package com.marketplace.seller;

import com.marketplace.support.FlowTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerShopFlowTests extends FlowTestSupport {

    @Test
    void sellerCanEditShopNameAndDescription() throws Exception {
        String sellerToken = createSeller("Nombre inicial");

        mockMvc.perform(put("/api/seller/shop").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"  Taller Nuevo  \",\"description\":\"Cerámica hecha a mano\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Taller Nuevo"))
                .andExpect(jsonPath("$.description").value("Cerámica hecha a mano"));

        mockMvc.perform(get("/api/seller/shop").header(AUTHORIZATION, bearer(sellerToken)))
                .andExpect(jsonPath("$.name").value("Taller Nuevo"));
    }

    @Test
    void shopUpdateRequiresAName() throws Exception {
        String sellerToken = createSeller("Tienda válida");

        mockMvc.perform(put("/api/seller/shop").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellerCanReplyToReviewsOfOwnProducts() throws Exception {
        String sellerToken = createSeller("Tienda respuestas");
        long productId = createProduct(sellerToken, "Cuenco");
        String buyerToken = registerAndLogin(uniqueEmail("buyer_reply"), "Buyer");
        completePurchase(buyerToken, sellerToken, productId);
        long reviewId = createReview(buyerToken, productId, 4, "Muy bonito");

        mockMvc.perform(get("/api/seller/reviews").header(AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId))
                .andExpect(jsonPath("$[0].productTitle").value("Cuenco"));

        mockMvc.perform(put("/api/seller/reviews/" + reviewId + "/reply").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON).content("{\"reply\":\"¡Gracias por tu compra!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerReply").value("¡Gracias por tu compra!"))
                .andExpect(jsonPath("$.sellerReplyAt").isNotEmpty());

        // The reply is public, shown together with the review.
        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(jsonPath("$[?(@.id == " + reviewId + ")].sellerReply").value("¡Gracias por tu compra!"));
    }

    @Test
    void sellerCannotReplyToReviewsOfAnotherShop() throws Exception {
        String sellerToken = createSeller("Tienda dueña");
        long productId = createProduct(sellerToken, "Plato");
        String buyerToken = registerAndLogin(uniqueEmail("buyer_other"), "Buyer");
        completePurchase(buyerToken, sellerToken, productId);
        long reviewId = createReview(buyerToken, productId, 5, "Perfecto");

        String otherSellerToken = createSeller("Tienda ajena");
        mockMvc.perform(put("/api/seller/reviews/" + reviewId + "/reply").header(AUTHORIZATION, bearer(otherSellerToken))
                        .contentType(APPLICATION_JSON).content("{\"reply\":\"No es mío\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderedProductCannotBeDeleted() throws Exception {
        String sellerToken = createSeller("Tienda pedidos");
        long productId = createProduct(sellerToken, "Taza");
        String buyerToken = registerAndLogin(uniqueEmail("buyer_delete"), "Buyer");
        completePurchase(buyerToken, sellerToken, productId);

        mockMvc.perform(delete("/api/seller/products/" + productId).header(AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isConflict());
    }
}
