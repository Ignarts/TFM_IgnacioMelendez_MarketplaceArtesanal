package com.marketplace.wishlist;

import com.marketplace.support.FlowTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistFlowTests extends FlowTestSupport {

    @Test
    void buyerCanAddListAndRemoveFavourites() throws Exception {
        String sellerToken = createSeller("Tienda favoritos");
        long productId = createProduct(sellerToken, "Manta");
        String buyerToken = registerAndLogin(uniqueEmail("wishlist"), "Buyer");

        mockMvc.perform(put("/api/me/wishlist/" + productId).header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isNoContent());
        // Adding twice is idempotent.
        mockMvc.perform(put("/api/me/wishlist/" + productId).header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/wishlist").header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Manta"));

        mockMvc.perform(delete("/api/me/wishlist/" + productId).header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/wishlist").header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void hiddenProductsDisappearFromFavourites() throws Exception {
        String sellerToken = createSeller("Tienda oculta");
        long productId = createProduct(sellerToken, "Cojín");
        String buyerToken = registerAndLogin(uniqueEmail("wishlist_hidden"), "Buyer");
        mockMvc.perform(put("/api/me/wishlist/" + productId).header(AUTHORIZATION, bearer(buyerToken)));

        String adminToken = createAdminAndLogin();
        mockMvc.perform(post("/api/admin/products/" + productId + "/hide").header(AUTHORIZATION, bearer(adminToken)));

        mockMvc.perform(get("/api/me/wishlist").header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addingUnknownProductReturns404() throws Exception {
        String buyerToken = registerAndLogin(uniqueEmail("wishlist_404"), "Buyer");

        mockMvc.perform(put("/api/me/wishlist/999999").header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void wishlistRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/wishlist"))
                .andExpect(status().isUnauthorized());
    }
}
