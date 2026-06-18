package com.marketplace.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"nombre\":\"Test\"}"));
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void sellerCanPublishAndPublicCanBrowse() throws Exception {
        String token = registerAndLogin("seller@test.com");

        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Mi tienda\"}"))
                .andExpect(status().isCreated());

        String categories = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();

        mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Vasija\",\"price\":19.90,\"stock\":5,\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Vasija"));

        // Public browsing, no token
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Vasija")));
    }

    @Test
    void cannotEditProductFromAnotherShop() throws Exception {
        String owner = registerAndLogin("owner@test.com");
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, bearer(owner))
                .contentType(APPLICATION_JSON).content("{\"name\":\"Tienda A\"}"));

        String categories = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categories).get(0).get("id").asLong();

        String created = mockMvc.perform(post("/api/seller/products").header(AUTHORIZATION, bearer(owner))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Anillo\",\"price\":30,\"stock\":2,\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString();
        long productId = mapper.readTree(created).get("id").asLong();

        String intruder = registerAndLogin("intruder@test.com");
        mockMvc.perform(post("/api/seller/shop").header(AUTHORIZATION, bearer(intruder))
                .contentType(APPLICATION_JSON).content("{\"name\":\"Tienda B\"}"));

        mockMvc.perform(put("/api/seller/products/" + productId).header(AUTHORIZATION, bearer(intruder))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Hackeado\",\"price\":1,\"stock\":1,\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isForbidden());
    }
}
