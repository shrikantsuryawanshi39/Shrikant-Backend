package com.one211.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.application.model.LoginRequest;
import com.one211.application.model.SignUp;
import com.one211.application.model.User;
import com.one211.application.service.OrganizationService;
import com.one211.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class UserControllerTest {
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
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void getUsersByOrgTest() throws Exception {
        Long orgId = signUpAndGetOrgId("praveen12311@gmail.com", "PVR1");
        String token = loginTestUser("praveen12311@gmail.com", orgId);

        mockMvc.perform(get("/api/org/{orgId}/user", orgId)
                        .header("Authorization", token)
                        .param("skip", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].email").value("praveen12311@gmail.com"));
    }

    @Test
    public void addUserTest() throws Exception {
        Long orgId = signUpAndGetOrgId("prathan@gmail.com", "PVR");
        String token = loginTestUser("prathan@gmail.com", orgId);
        User user = new User(null, "Raja Ram", "raja@gmail.com", "abcdefg", "USER", "HAPPY HAPPY", LocalDateTime.now(), LocalDateTime.now());

        mockMvc.perform(post("/api/org/{orgId}/user", orgId)
                        .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    public void getUsersByWrongOrgTest() throws Exception {
        Long orgId = signUpAndGetOrgId("rajarammm@gmail.com", "TVS");
        String token = loginTestUser("rajarammm@gmail.com", orgId);

        MockHttpServletResponse response = mockMvc.perform(get("/api/org/{orgId}/user", orgId - 1)
                        .header("Authorization", token)
                        .param("skip", "0")
                        .param("limit", "10"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse();
        assertEquals("Org ID mismatch", response.getErrorMessage());
    }

    @Test
    public void updateUserTest() throws Exception {
        String email = "prathamprasun@gmail.com";
        Long orgId = signUpAndGetOrgId(email, "One10");

        String token = loginTestUser(email, orgId);
        User existingUser = userService.getUserByEmail(email);
        User updatedUser = buildUpdatedUser(existingUser);

        mockMvc.perform(patch("/api/org/{orgId}/user/{userName}", orgId, existingUser.email())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("prathamprasun@gmail.com"))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.description").value("Updated desc"));
    }

    @Test
    public void updateUserByWrongOrgIdTest() throws Exception {
        String email = "prathampras@gmail.com";
        Long orgId = signUpAndGetOrgId(email, "One19");
        String token = loginTestUser(email, orgId);
        User existingUser = userService.getUserByEmail(email);
        User updatedUser = buildUpdatedUser(existingUser);

        MockHttpServletResponse response = mockMvc.perform(
                        patch("/api/org/{orgId}/user/{userName}", orgId + 1, existingUser.email())
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse();

        assertEquals("Org ID mismatch", response.getErrorMessage());
    }

    private Long signUpAndGetOrgId(String email, String orgName) throws Exception {
        SignUp signupRequest = new SignUp("Gagan Taneja", email, "tanejaGagan", orgName, "Big Data Com.", LocalDateTime.now(), LocalDateTime.now());
        MvcResult result = mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String org = objectMapper.readValue(result.getResponse().getContentAsString(), SignUp.class).orgName();
        return organizationService.getOrgByName(org).id();
    }

    private String loginTestUser(String email, Long orgId) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, "tanejaGagan");
        String json = mockMvc.perform(post("/api/login/org/{orgId}", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private User buildUpdatedUser(User original) {
        return new User(original.id(), "Updated Name", original.email(), "ABCDEFG", "ADMIN", "Updated desc", original.createdAt(), LocalDateTime.now());
    }
}
