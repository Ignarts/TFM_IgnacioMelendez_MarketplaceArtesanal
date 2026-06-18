package com.marketplace.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseReviewFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"Test\"}"));
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    private long createProductWithShop(String sellerEmail) throws Exception {
        String seller = registerAndLogin(sellerEmail);
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, seller)
                .contentType(APPLICATION_JSON).content("{\"name\":\"Tienda\"}"));
        String categories = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();
        String created = mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, seller)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Vasija\",\"price\":20,\"stock\":5,\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(created).get("id").asLong();
    }

    @Test
    void fullPurchaseThenReview() throws Exception {
        String seller = registerAndLogin("seller-flow@test.com");
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, seller)
                .contentType(APPLICATION_JSON).content("{\"name\":\"Tienda\"}"));
        String categories = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();
        String created = mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, seller)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Vasija\",\"price\":20,\"stock\":5,\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString();
        long productId = mapper.readTree(created).get("id").asLong();

        String buyer = registerAndLogin("buyer-flow@test.com");

        // Checkout → one order, PENDING
        String orders = mockMvc.perform(post("/api/orders").header(AUTHORIZATION, buyer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].total").value(40))
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(orders).get(0).get("id").asLong();

        // Buyer cannot review yet (no delivered order)
        mockMvc.perform(post("/api/products/" + productId + "/reviews").header(AUTHORIZATION, buyer)
                        .contentType(APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"\"}"))
                .andExpect(status().isForbidden());

        // PENDING → PAID → SHIPPED → DELIVERED
        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header(AUTHORIZATION, buyer))
                .andExpect(jsonPath("$.status").value("PAID"));
        mockMvc.perform(post("/api/seller/orders/" + orderId + "/ship").header(AUTHORIZATION, seller))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header(AUTHORIZATION, buyer))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Now the verified review is allowed and visible publicly
        mockMvc.perform(post("/api/products/" + productId + "/reviews").header(AUTHORIZATION, buyer)
                        .contentType(APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"Preciosa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comment").value("Preciosa"));
    }

    @Test
    void reviewWithoutPurchaseIsForbidden() throws Exception {
        long productId = createProductWithShop("seller-np@test.com");
        String stranger = registerAndLogin("stranger@test.com");

        mockMvc.perform(post("/api/products/" + productId + "/reviews").header(AUTHORIZATION, stranger)
                        .contentType(APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
