package com.marketplace.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Shared HTTP helpers for MockMvc flow tests: users, shops, products and a full purchase. */
public abstract class FlowTestSupport {

    protected static final String PASSWORD = "password123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected String uniqueEmail(String prefix) {
        return prefix + "_" + System.nanoTime() + "@test.com";
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    /** Registers a regular user and returns their token. */
    protected String registerAndLogin(String email, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"" + name + "\"}"));
        return login(email, PASSWORD);
    }

    /** Creates an ADMIN user directly in the DB (bypasses normal registration flow). */
    protected String createAdminAndLogin(String email) throws Exception {
        userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "Admin", Set.of(Role.ADMIN)));
        return login(email, PASSWORD);
    }

    protected String createAdminAndLogin() throws Exception {
        return createAdminAndLogin(uniqueEmail("admin"));
    }

    /** Registers a user, opens a shop for them and returns their token. */
    protected String createSeller(String shopName) throws Exception {
        String token = registerAndLogin(uniqueEmail("seller"), "Seller");
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content("{\"name\":\"" + shopName + "\"}"));
        return token;
    }

    protected long createProduct(String sellerToken, String title) throws Exception {
        String categories = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();
        String created = mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, bearer(sellerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"price\":20,\"stock\":5,\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(created).get("id").asLong();
    }

    /** Orders, pays, ships and confirms one unit of the product, so the buyer may review it. */
    protected void completePurchase(String buyerToken, String sellerToken, long productId) throws Exception {
        String orders = mockMvc.perform(post("/api/orders").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(orders).get(0).get("id").asLong();
        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header(AUTHORIZATION, bearer(buyerToken)));
        mockMvc.perform(post("/api/seller/orders/" + orderId + "/ship").header(AUTHORIZATION, bearer(sellerToken)));
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header(AUTHORIZATION, bearer(buyerToken)));
    }

    protected long createReview(String buyerToken, long productId, int rating, String comment) throws Exception {
        String review = mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"rating\":" + rating + ",\"comment\":\"" + comment + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(review).get("id").asLong();
    }
}
