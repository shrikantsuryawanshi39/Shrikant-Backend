package com.one211.application.service;

import com.one211.application.model.Cluster;
import com.one211.application.model.SignUp;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class ClusterServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private UserService userService;

    @Test
    public void addAndDeleteClusterTest() throws Exception {
        Long orgId = signUp("praveen@gmail.com", "TestOrg421");
        Cluster cluster = createTestCluster("Test Cluster 1", orgId);
        Cluster savedCluster = clusterService.addCluster(cluster.orgId(), cluster);
        assertNotNull(savedCluster);
        assertNotNull(savedCluster.id());
        assertEquals("Test Cluster 1", savedCluster.name());
        assertEquals("Test Description", savedCluster.description());
        assertTrue(savedCluster.status());

        boolean deleted = clusterService.deleteCluster(savedCluster.orgId(), savedCluster.name());
        assertTrue(deleted);
    }

    @Test
    public void getClusterTest() throws Exception {
        Long orgId = signUp("praveen9@gmail.com", "TestOrg4");
        Cluster cluster = createTestCluster("Test Cluster 2", orgId);
        Cluster savedCluster = clusterService.addCluster(cluster.orgId(), cluster);

        //By Name
        Cluster resultClusterByName = clusterService.getClusterByName(savedCluster.orgId(), savedCluster.name());
        assertNotNull(resultClusterByName);
        assertEquals(savedCluster.id(), resultClusterByName.id());

        //By ID
        Cluster resultClusterById = clusterService.getClusterByName(savedCluster.orgId(), savedCluster.name());
        assertNotNull(resultClusterById);
        assertEquals(savedCluster.id(), resultClusterById.id());
    }

    @Test
    public void updateClusterTest() throws Exception {
        Long orgId = signUp("praveen99812@gmail.com", "TestOrg5");
        Cluster cluster = createTestCluster("Test Cluster 3", orgId);
        Cluster savedCluster = clusterService.addCluster(cluster.orgId(), cluster);

        Cluster updatedValue = new Cluster(
                savedCluster.id(),
                savedCluster.orgId(),
                "Updated Cluster",
                "Test Updated Description",
                false,
                savedCluster.createdAt()
        );
        Cluster updated = clusterService.updateCluster(savedCluster.orgId(), savedCluster.name(), updatedValue);

        assertEquals("Updated Cluster", updated.name());
        assertEquals("Test Updated Description", updated.description());
        assertFalse(updated.status());
    }

    @Test
    public void clusterComboExistTest() throws Exception {
        Long orgId = signUp("praveen9971@gmail.com", "TestOrg4212");

        LocalDateTime now = LocalDateTime.now();
        Cluster cluster1 = new Cluster(
                null,
                orgId,
                "DuplicateCluster",
                "Duplicate Desc 1",
                true,
                now
        );
        Cluster savedCluster = clusterService.addCluster(cluster1.orgId(), cluster1);
        assertNotNull(savedCluster);

        Cluster cluster2 = new Cluster(
                null,
                orgId,
                "DuplicateCluster",
                "Duplicate Desc 2",
                false,
                now
        );
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clusterService.addCluster(cluster2.orgId(), cluster2);
        });
        assertTrue(exception.getMessage().contains("already exists"));
    }

    private Long signUp(String email, String orgName) {
        var user = new SignUp(
                "Gagan Taneja",
                email,
                "tanejaGagan",
                orgName,
                "Big Data Com.",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        signUpService.signUpUser(user);
        return organizationService.getOrgByName(user.orgName()).id();
    }

    private Cluster createTestCluster(String name, Long orgId) {
        return new Cluster(
                null,
                orgId,
                name,
                "Test Description",
                true,
                LocalDateTime.now()
        );
    }
}
