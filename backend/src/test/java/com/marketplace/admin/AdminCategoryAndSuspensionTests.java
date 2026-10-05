package com.marketplace.admin;

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
class AdminCategoryAndSuspensionTests extends FlowTestSupport {

    // ── Suspension ───────────────────────────────────────────────────────────

    @Test
    void suspendedUserLosesAccessImmediatelyAndCannotLogIn() throws Exception {
        String email = uniqueEmail("suspended");
        String token = registerAndLogin(email, "Target");
        long userId = userRepository.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(get("/api/me").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        String adminToken = createAdminAndLogin();
        mockMvc.perform(post("/api/admin/users/" + userId + "/suspend").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        // The token issued before the suspension no longer authenticates.
        mockMvc.perform(get("/api/me").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account suspended"));
    }

    @Test
    void adminCannotSuspendOwnAccount() throws Exception {
        String email = uniqueEmail("self_admin");
        String adminToken = createAdminAndLogin(email);
        long adminId = userRepository.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/" + adminId + "/suspend").header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    // ── Categories ───────────────────────────────────────────────────────────

    @Test
    void nonAdminCannotManageCategories() throws Exception {
        String buyerToken = registerAndLogin(uniqueEmail("buyer_cat"), "Buyer");

        mockMvc.perform(post("/api/admin/categories").header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Vidrio\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteCategory() throws Exception {
        String adminToken = createAdminAndLogin();
        String suffix = String.valueOf(System.nanoTime());

        String created = mockMvc.perform(post("/api/admin/categories").header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Cestería Fina " + suffix + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("cesteria-fina-" + suffix))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());

        mockMvc.perform(put("/api/admin/categories/" + id).header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Cestería\",\"slug\":\"cesteria-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cestería"))
                .andExpect(jsonPath("$.slug").value("cesteria-" + suffix));

        mockMvc.perform(delete("/api/admin/categories/" + id).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void duplicateCategorySlugIsRejected() throws Exception {
        String adminToken = createAdminAndLogin();
        String name = "Duplicada " + System.nanoTime();

        mockMvc.perform(post("/api/admin/categories").header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/categories").header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void categoryInUseCannotBeDeleted() throws Exception {
        String sellerToken = createSeller("Tienda categorías");
        long productId = createProduct(sellerToken, "Jarra");
        String product = mockMvc.perform(get("/api/products/" + productId))
                .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(product).get("categoryId").asLong();

        String adminToken = createAdminAndLogin();
        mockMvc.perform(delete("/api/admin/categories/" + categoryId).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());
    }
}
