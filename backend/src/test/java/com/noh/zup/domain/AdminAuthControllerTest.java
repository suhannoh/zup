package com.noh.zup.domain;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:zup-admin-auth-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_EMAIL=admin@zup.local",
        "ADMIN_PASSWORD=admin1234",
        "JWT_SECRET=test-secret-with-enough-length-for-hmac",
        "JWT_EXPIRATION_SECONDS=86400"
})
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSuccessReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@zup.local",
                                "password", "admin1234"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("admin login success"))
                .andExpect(jsonPath("$.data.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400))
                .andExpect(jsonPath("$.data.email").value("admin@zup.local"))
                .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"));
    }

    @Test
    void loginFailureReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@zup.local",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminApiWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void adminApiWithTokenSucceeds() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publicApiWithoutTokenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/brands").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@zup.local",
                                "password", "admin1234"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
