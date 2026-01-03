package com.one211.application.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.application.model.Cluster;
import com.one211.application.model.ClusterAssignmentRequest;
import com.one211.application.model.LoginRequest;
import com.one211.application.model.SignUp;
import com.one211.application.service.ClusterService;
import com.one211.application.service.OrganizationService;
import com.one211.application.service.SignUpService;
import com.one211.application.service.UserService;
import org.junit.jupiter.api.Disabled;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class ClusterAssignmentControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ObjectMapper objectMapper;
    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationService organizationService;
    @Autowired private ClusterService clusterService;
    @Autowired private UserService userService;
    @Autowired private SignUpService signUpService;

    // --------------------------
    // USER TESTS
    // --------------------------

    @Test
    public void assignClusterToUserTest() throws Exception {
        var params = setUpUser("user1@gmail.com", "OrgU1");
        Long orgId = (Long) params.get(0);
        String username = (String) params.get(1);
        String token = (String) params.get(2);

        String clusterName = createCluster(orgId, "ClusterU1");
        var request = new ClusterAssignmentRequest("USER", username, clusterName, "assign");

        toggleAssignment(orgId, token, request);
    }

    @Test
    public void unAssignClusterFromUserTest() throws Exception {
        var params = setUpUser("user2@gmail.com", "OrgU2");
        Long orgId = (Long) params.get(0);
        String username = (String) params.get(1);
        String token = (String) params.get(2);

        String clusterName = createCluster(orgId, "ClusterU2");

        var assign = new ClusterAssignmentRequest("USER", username, clusterName, "assign");
        toggleAssignment(orgId, token, assign);

        var unassign = new ClusterAssignmentRequest("USER", username, clusterName, "unassign");
        toggleAssignment(orgId, token, unassign);
    }

    @Test
    public void getUnAssignClusters_ForUserTest() throws Exception {
        var params = setUpUser("user3@gmail.com", "OrgU3");
        Long orgId = (Long) params.get(0);
        String username = (String) params.get(1);
        String token = (String) params.get(2);

        for (int i = 0; i < 10; i++) {
            String clusterName = createCluster(orgId, "Cluster" + i);
            if (i == 3) {
                var assign = new ClusterAssignmentRequest("USER", username, clusterName, "assign");
                toggleAssignment(orgId, token, assign);
            }
        }

        // Another org’s cluster should not interfere
        var params2 = setUpUser("user564@gmail.com", "OrgU564");
        Long orgId2 = (Long) params2.get(0);
        createCluster(orgId2, "Cluster9"); // (Cluster9) the same cluster which already exists in another organization.

        var request = new ClusterAssignmentRequest("USER", username);
        MvcResult result = mockMvc.perform(get("/api/orgs/{orgId}/cluster-assignments", orgId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
        long unAssignCount = list.stream()
                .filter(item -> "unassign".equals(item.get("action")))
                .count();
        assertEquals(9, unAssignCount);
    }

    @Test
    @Disabled
    public void getAllClusters_ForUser_WithWrongTokenTest() throws Exception {
        var params = setUpUser("user5@gmail.com", "OrgU5");
        Long orgId = (Long) params.get(0);
        String username = (String) params.get(1);
        String token = (String) params.get(2);

        for (int i = 0; i < 5; i++) {
            String clusterName = createCluster(orgId, "Cluster" + i);
            if (i == 2) {
                var assign = new ClusterAssignmentRequest("USER", username, clusterName, "assign");
                toggleAssignment(orgId, token, assign);
            }
        }

        var params2 = setUpUser("mohanranga351@gmail.com", "MH351");
        String token2 = (String) params2.get(2);

        var request = new ClusterAssignmentRequest("USER", username);
        mockMvc.perform(get("/api/orgs/{orgId}/cluster-assignments", orgId)
                        .header("Authorization", token2) // wrong token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --------------------------
    // GROUP TESTS
    // --------------------------

    @Test
    public void assignClusterToGroupTest() throws Exception {
        var params = setUpGroup("group1", "OrgG1");
        Long orgId = (Long) params.get(0);
        String groupName = (String) params.get(1);
        String token = (String) params.get(2);

        String clusterName = createCluster(orgId, "ClusterG1");
        var request = new ClusterAssignmentRequest("GROUP", groupName, clusterName, "assign");

        toggleAssignment(orgId, token, request);
    }

    @Test
    public void unAssignClusterFromGroupTest() throws Exception {
        var params = setUpGroup("group2", "OrgG2");
        Long orgId = (Long) params.get(0);
        String groupName = (String) params.get(1);
        String token = (String) params.get(2);

        String clusterName = createCluster(orgId, "ClusterG2");
        var assign = new ClusterAssignmentRequest("GROUP", groupName, clusterName, "assign");
        toggleAssignment(orgId, token, assign);

        var unassign = new ClusterAssignmentRequest("GROUP", groupName, clusterName, "unassign");
        toggleAssignment(orgId, token, unassign);
    }

    // --------------------------
    // HELPERS
    // --------------------------

    private void toggleAssignment(Long orgId, String token, ClusterAssignmentRequest request) throws Exception {
        mockMvc.perform(post("/api/orgs/{orgId}/cluster-assignments", orgId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private List<?> setUpUser(String email, String orgName) throws Exception {
        SignUp signupRequest = new SignUp("Test User", email, "pass123", orgName, "Big Data", LocalDateTime.now(), LocalDateTime.now());
        signUpService.signUpUser(signupRequest);
        Long orgId = organizationService.getOrgByName(orgName).id();
        String username = userService.getUserByEmail(email).email();
        String token = login(orgId, email, "pass123");
        return List.of(orgId, username, token);
    }

    private List<?> setUpGroup(String groupName, String orgName) throws Exception {
        SignUp signupRequest = new SignUp("Group Admin", groupName + "@mail.com", "grpPass", orgName, "Big Data", LocalDateTime.now(), LocalDateTime.now());
        signUpService.signUpUser(signupRequest);
        Long orgId = organizationService.getOrgByName(orgName).id();
        String token = login(orgId, signupRequest.email(), "grpPass");
        return List.of(orgId, groupName, token);
    }

    private String login(Long orgId, String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        String json = mockMvc.perform(post("/api/login/org/{orgId}", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private String createCluster(Long orgId, String name) {
        Cluster cluster = new Cluster(null, orgId, name, "Test Cluster", true, LocalDateTime.now());
        return clusterService.addCluster(orgId, cluster).name();
    }
}
