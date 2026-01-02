package com.one211.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.application.model.Cluster;
import com.one211.application.model.LoginRequest;
import com.one211.application.model.SignUp;
import com.one211.application.service.ClusterService;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class ClusterControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClusterService clusterService;


    @Test
    public void createClusterTest() throws Exception {
        List<?> orgEmail = signUp("praveen9981@gmail.com", "TestOrg4");
        String token = loginTestUser("praveen9981@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 1", orgId);
        mockMvc.perform(post("/api/org/{orgId}/cluster", orgId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cluster)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Cluster 1"))
                .andExpect(jsonPath("$.description").value("For Test Description"))
                .andExpect(jsonPath("$.orgId").value(orgId));
    }

    @Test
    public void createClusterWithWrongOrgIdTest() throws Exception {
        List<?> orgEmail = signUp("praveen9@gmail.com", "TestOrg3");
        String token = loginTestUser("praveen9@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();
        Cluster cluster = createTestCluster("Test Cluster 10", orgId);

        mockMvc.perform(post("/api/org/{orgId}/cluster", orgId + 1)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cluster)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getClusterTest() throws Exception {
        List<?> orgEmail = signUp("praveen99@gmail.com", "TestOrg2");
        String token = loginTestUser("praveen99@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 2", orgId);
        Cluster savedCluster = clusterService.addCluster(orgId, cluster);
        String clusterName = savedCluster.name();

        mockMvc.perform(get("/api/org/{orgId}/cluster/{clusterName}", orgId, clusterName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cluster)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Cluster 2"))
                .andExpect(jsonPath("$.description").value("For Test Description"))
                .andExpect(jsonPath("$.orgId").value(orgId))
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    public void getClusterWithWrongOrgIdTest() throws Exception {
        List<?> orgEmail = signUp("praveen998@gmail.com", "TestOrg42");
        String token = loginTestUser("praveen998@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 20", orgId);
        Cluster savedCluster = clusterService.addCluster(orgId, cluster);
        String clusterName = savedCluster.name();

        mockMvc.perform(get("/api/org/{orgId}/cluster/{clusterName}", orgId - 1, clusterName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cluster)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateClusterTest() throws Exception {
        List<?> orgEmail = signUp("praveen99811@gmail.com", "TestOrg41");
        String token = loginTestUser("praveen99811@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 311", orgId);
        Cluster savedCluster = clusterService.addCluster(orgId, cluster);
        String clusterName = savedCluster.name();

        Cluster updatedValue = new Cluster(
                savedCluster.id(),
                savedCluster.orgId(),
                "Updated Test Cluster",
                "Updated Test Description",
                false,
                savedCluster.createdAt()
        );

        mockMvc.perform(patch("/api/org/{orgId}/cluster/{clusterName}", orgId, clusterName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Test Cluster"))
                .andExpect(jsonPath("$.description").value("Updated Test Description"))
                .andExpect(jsonPath("$.orgId").value(orgId));
    }

    @Test
    public void deleteClusterTest() throws Exception {
        List<?> orgEmail = signUp("praveen998111@gmail.com", "TestOrg432");
        String token = loginTestUser("praveen998111@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 432", orgId);
        Cluster savedCluster = clusterService.addCluster(orgId, cluster);
        String clusterName = savedCluster.name();

        mockMvc.perform(delete("/api/org/{orgId}/cluster/{clusterName}", orgId, clusterName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedCluster)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteClusterByWrongOrgIdTest() throws Exception {
        List<?> orgEmail = signUp("praveen998121@gmail.com", "TestOrg421");
        String token = loginTestUser("praveen998121@gmail.com", (Long) orgEmail.getFirst());
        Long orgId = (Long) orgEmail.getFirst();

        Cluster cluster = createTestCluster("Test Cluster 402", orgId);
        Cluster savedCluster = clusterService.addCluster(orgId, cluster);
        String clusterName = savedCluster.name();

        mockMvc.perform(delete("/api/org/{orgId}/cluster/{clusterName}", orgId - 3, clusterName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedCluster)))
                .andExpect(status().isForbidden());
    }

    private Cluster createTestCluster(String name, Long orgId) {
        return new Cluster(
                null,
                orgId,
                name,
                "For Test Description",
                true,
                LocalDateTime.now()
        );
    }

    private List<?> signUp(String email, String orgName) throws Exception {
        SignUp signupRequest = new SignUp("Gagan Taneja", email, "tanejaGagan", orgName, "Big Data Com.", LocalDateTime.now(), LocalDateTime.now());;
        MvcResult result = mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String org = objectMapper.readValue(result.getResponse().getContentAsString(), SignUp.class).orgName();
        return List.of(organizationService.getOrgByName(org).id(), objectMapper.readValue(result.getResponse().getContentAsString(), SignUp.class).email());
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
}
