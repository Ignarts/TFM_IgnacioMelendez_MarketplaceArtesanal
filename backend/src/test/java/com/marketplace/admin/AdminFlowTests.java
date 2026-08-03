package com.marketplace.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    /** Creates an ADMIN user directly in the DB (bypasses normal registration flow). */
    private String createAdminAndLogin() throws Exception {
        String email = "admin_" + System.nanoTime() + "@test.com";
        User admin = new User(email, passwordEncoder.encode("password123"), "Admin", Set.of(Role.ADMIN));
        userRepository.save(admin);
        return login(email, "password123");
    }

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        // Register regular buyer
        String buyerEmail = "buyer_admin_test_" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + buyerEmail + "\",\"password\":\"password123\",\"name\":\"Buyer\"}"));
        String buyerToken = login(buyerEmail, "password123");

        // BUYER tries admin endpoint → 403
        mockMvc.perform(get("/api/admin/shops")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestToAdminReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/shops"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListAndVerifyShop() throws Exception {
        // Create a seller with a shop
        String sellerEmail = "seller_verify_" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + sellerEmail + "\",\"password\":\"password123\",\"name\":\"Seller\"}"));
        String sellerToken = login(sellerEmail, "password123");

        String shopResponse = mockMvc.perform(post("/api/seller/shop")
                        .header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Tienda artesana\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long shopId = mapper.readTree(shopResponse).get("id").asLong();

        String adminToken = createAdminAndLogin();

        // Admin lists pending shops — contains our shop
        mockMvc.perform(get("/api/admin/shops/pending")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + shopId + ")].verified").value(false));

        // Admin verifies shop
        mockMvc.perform(post("/api/admin/shops/" + shopId + "/verify")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void adminCanSuspendAndUnsuspendUser() throws Exception {
        String targetEmail = "target_" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + targetEmail + "\",\"password\":\"password123\",\"name\":\"Target\"}"));
        long userId = userRepository.findByEmail(targetEmail).get().getId();

        String adminToken = createAdminAndLogin();

        mockMvc.perform(post("/api/admin/users/" + userId + "/suspend")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true));

        mockMvc.perform(post("/api/admin/users/" + userId + "/unsuspend")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(false));
    }

    @Test
    void adminCanListAndDeleteReview() throws Exception {
        String sellerEmail = "seller_review_" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + sellerEmail + "\",\"password\":\"password123\",\"name\":\"Seller\"}"));
        String sellerToken = login(sellerEmail, "password123");
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, bearer(sellerToken))
                .contentType(APPLICATION_JSON).content("{\"name\":\"Tienda\"}"));
        String categories = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();
        String created = mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Vasija\",\"price\":20,\"stock\":5,\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString();
        long productId = mapper.readTree(created).get("id").asLong();

        String buyerEmail = "buyer_review_" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + buyerEmail + "\",\"password\":\"password123\",\"name\":\"Buyer\"}"));
        String buyerToken = login(buyerEmail, "password123");

        String orders = mockMvc.perform(post("/api/orders").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(orders).get(0).get("id").asLong();
        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header(AUTHORIZATION, bearer(buyerToken)));
        mockMvc.perform(post("/api/seller/orders/" + orderId + "/ship").header(AUTHORIZATION, bearer(sellerToken)));
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header(AUTHORIZATION, bearer(buyerToken)));

        String review = mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON).content("{\"rating\":1,\"comment\":\"Spam\"}"))
                .andReturn().getResponse().getContentAsString();
        long reviewId = mapper.readTree(review).get("id").asLong();

        String adminToken = createAdminAndLogin();

        mockMvc.perform(get("/api/admin/reviews")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + reviewId + ")].comment").value("Spam"));

        mockMvc.perform(delete("/api/admin/reviews/" + reviewId)
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/reviews")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + reviewId + ")]").isEmpty());
    }
}
