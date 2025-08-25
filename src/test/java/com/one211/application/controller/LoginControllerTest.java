package com.one211.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.application.model.LoginRequest;
import com.one211.application.model.SignUp;
import com.one211.application.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class LoginControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void loginSuccessTest() throws Exception {
        Long orgId = signUpAndGetOrgId("gagan@example.com", "PVR");
        LoginRequest login = loginRequest("gagan@example.com", "tanejaGagan");
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    public void loginFailsWithIncorrectPassword() throws Exception {
        Long orgId = signUpAndGetOrgId("meena@example.com", "Reliance");
        LoginRequest login = loginRequest("meena@example.com", "wrongPassword123");
        mockMvc.perform(post("/api/login/org/{orgId}", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginFailsWhenUserNotInOrg() throws Exception {
        Long orgId = signUpAndGetOrgId("tanu@example.com", "MRF");
        LoginRequest login = loginRequest("tanu@example.com", "tanejaGagan"); // wrong org
        mockMvc.perform(post("/api/login/org/{orgId}", orgId - 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isNotFound());
    }

    private Long signUpAndGetOrgId(String email, String orgName) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SignUp user = new SignUp("Gagan Taneja", email, "tanejaGagan", orgName, "Big Data Com.", now, now);
        MvcResult result = mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        var org = objectMapper.readValue(json, SignUp.class).orgName();
        return organizationService.getOrgByName(org).id();
    }

    private LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }
}
