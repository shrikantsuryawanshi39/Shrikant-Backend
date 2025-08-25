package com.one211.application.service;

import com.one211.application.model.Cluster;
import com.one211.application.model.ClusterWithAction;
import com.one211.application.model.SignUp;
import com.one211.application.model.ClusterAssignmentRequest;
import com.one211.application.model.Group;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class ClusterAssignmentServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ClusterAssignmentService clusterAssignmentService;
    @Autowired
    private SignUpService signUpService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;

    // --------------------------
    // USER TESTS
    // --------------------------

    @Test
    public void assignClusterToUserTest() {
        var signUpData = signUp("user1@gmail.com", "TestOrg1");
        Long orgId = signUpData.orgId();
        String userName = signUpData.userName();

        Cluster cluster = createCluster("ClusterA", orgId);
        var request = new ClusterAssignmentRequest("USER", userName, cluster.name(), "assign");

        boolean result = clusterAssignmentService.updateAssignment(orgId, request);
        assertTrue(result);
    }

    @Test
    public void unAssignClusterFromUserTest() {
        var signUpData = signUp("user2@gmail.com", "TestOrg2");
        Long orgId = signUpData.orgId();
        String userName = signUpData.userName();

        Cluster cluster = createCluster("ClusterB", orgId);
        var assignRequest = new ClusterAssignmentRequest("USER", userName, cluster.name(), "assign");
        assertTrue(clusterAssignmentService.updateAssignment(orgId, assignRequest));

        var unassignRequest = new ClusterAssignmentRequest("USER", userName, cluster.name(), "unassign");
        boolean result = clusterAssignmentService.updateAssignment(orgId, unassignRequest);
        assertTrue(result);
    }

    @Test
    public void assignClusterWithInvalidOrgIdTest() {
        var signUpData = signUp("user3@gmail.com", "TestOrg3");
        Long orgId = signUpData.orgId();
        String userName = signUpData.userName();

        Cluster cluster = createCluster("ClusterC", orgId);
        var request = new ClusterAssignmentRequest("USER", userName, cluster.name(), "assign");

        boolean result = clusterAssignmentService.updateAssignment(9999L, request);
        assertFalse(result);
    }

    @Test
    public void getAssignedClustersTest() {
        var signUpData = signUp("user4@gmail.com", "TestOrg4");
        Long orgId = signUpData.orgId();
        String userName = signUpData.userName();

        for (int i = 1; i <= 3; i++) {
            Cluster cluster = createCluster("Cluster_" + i, orgId);
            var request = new ClusterAssignmentRequest("USER", userName, cluster.name(), "assign");
            assertTrue(clusterAssignmentService.updateAssignment(orgId, request));
        }
        var getRequest = new ClusterAssignmentRequest("USER", userName);
        List<ClusterWithAction> clusters = clusterAssignmentService.allClusters(orgId, getRequest);
        assertEquals(3, clusters.stream().filter(c -> c.action().equals("assign")).count());
    }

    // --------------------------
    // GROUP TESTS
    // --------------------------

    @Test
    public void assignClusterToGroupTest() {
        var signUpData = signUp("groupuser1@gmail.com", "GroupOrg1");
        Long orgId = signUpData.orgId();
        String groupName = createGroup("GroupA", orgId).name();

        Cluster cluster = createCluster("ClusterG1", orgId);
        var request = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "assign");

        boolean result = clusterAssignmentService.updateAssignment(orgId, request);
        assertTrue(result);
    }

    @Test
    public void unAssignClusterFromGroupTest() {
        var signUpData = signUp("groupuser2@gmail.com", "GroupOrg2");
        Long orgId = signUpData.orgId();
        String groupName = createGroup("GroupB", orgId).name();

        Cluster cluster = createCluster("ClusterG2", orgId);
        var assignRequest = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "assign");
        assertTrue(clusterAssignmentService.updateAssignment(orgId, assignRequest));

        var unassignRequest = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "unassign");
        boolean result = clusterAssignmentService.updateAssignment(orgId, unassignRequest);
        assertTrue(result);
    }

    @Test
    public void getAssignedClustersForGroupTest() {
        var signUpData = signUp("groupuser3@gmail.com", "GroupOrg3");
        Long orgId = signUpData.orgId();
        String groupName = createGroup("GroupC", orgId).name();

        for (int i = 1; i <= 2; i++) {
            Cluster cluster = createCluster("ClusterG_" + i, orgId);
            var request = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "assign");
            assertTrue(clusterAssignmentService.updateAssignment(orgId, request));
        }
        var getRequest = new ClusterAssignmentRequest("GROUP", groupName);
        List<ClusterWithAction> clusters = clusterAssignmentService.allClusters(orgId, getRequest);
        assertEquals(2, clusters.stream().filter(c -> c.action().equals("assign")).count());
    }

    @Test
    public void unAssignClusterToGroup_WithWrongInputsTest() {
        var signUpData = signUp("groupuser4@gmail.com", "GroupOrg4");
        Long orgId = signUpData.orgId();
        String groupName = createGroup("GroupE", orgId).name();
        Cluster cluster = createCluster("ClusterG4", orgId);
        ClusterAssignmentRequest assignRequest = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "assign");
        boolean assign = clusterAssignmentService.updateAssignment(orgId, assignRequest);
        assertTrue(assign);
        ClusterAssignmentRequest unAssignRequest = new ClusterAssignmentRequest("GROUP", groupName + "invalid", assignRequest.name(), "unassign");
        boolean unAssign = clusterAssignmentService.updateAssignment(orgId, unAssignRequest);
        assertFalse(unAssign);
    }

    @Test
    public void assignUnAssignCluster_ToGroup_FrequentlyTest() {
        var signUpData = signUp("groupuser5@gmail.com", "GroupOrg5");
        Long orgId = signUpData.orgId();
        String groupName = createGroup("GroupF", orgId).name();
        Cluster cluster = createCluster("ClusterG5", orgId);
        ClusterAssignmentRequest assignRequest = new ClusterAssignmentRequest("GROUP", groupName, cluster.name(), "assign");
        ClusterAssignmentRequest unAssignRequest = new ClusterAssignmentRequest("GROUP", groupName, assignRequest.name(), "unassign");

        for (int i = 0; i < 50; i++) {
            boolean assign = clusterAssignmentService.updateAssignment(orgId, assignRequest);
            assertTrue(assign);
            boolean unAssign = clusterAssignmentService.updateAssignment(orgId, unAssignRequest);
            assertTrue(unAssign);
        }
    }

    // -----------------------
    // Helpers
    // -----------------------

    private record SignUpData(Long orgId, String userName) {}

    private SignUpData signUp(String email, String orgName) {
        var user = new SignUp(
                "Test User",
                email,
                "password123",
                orgName,
                "Big Data Com.",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        signUpService.signUpUser(user);
        Long orgId = organizationService.getOrgByName(user.orgName()).id();
        String userName = userService.getUserByEmail(user.email()).email();
        return new SignUpData(orgId, userName);
    }

    private Cluster createCluster(String name, Long orgId) {
        var cluster = new Cluster(null, orgId, name, "Test Description", true, LocalDateTime.now());
        return clusterService.addCluster(cluster.orgId(), cluster);
    }

    private Group createGroup(String name, Long orgId) {
        var group = new Group(null, name, "Test Group", LocalDateTime.now());
        return groupService.createGroup(orgId, group);
    }
}
