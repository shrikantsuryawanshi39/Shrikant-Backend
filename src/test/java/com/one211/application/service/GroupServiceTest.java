package com.one211.application.service;

import com.one211.application.model.Group;
import com.one211.application.model.GroupAssignmentRequest;
import com.one211.application.model.SignUp;
import com.one211.application.model.UserWithStatus;
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
public class GroupServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GroupService groupService;
    @Autowired
    private SignUpService signUpService;
    @Autowired
    private OrganizationService organizationService;

    @Test
    public void addGroupTest() {
        var data = signUp("ajay@gmail.com", "AJ");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());
    }

    @Test
    public void addGroupWithWrongInputTest() {
        var data = signUp("ajayBB@gmail.com", "AJBB");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class, () -> {
            groupService.createGroup(data.orgId() + 2, group);
        });
        assertTrue(err.getMessage()
                .startsWith("Group with name '" + group.name() + "' may already exist in organization " + (data.orgId() + 2)));
    }

    @Test
    public void removeGroupTest() {
        var data = signUp("ajay1@gmail.com", "AJ1");
        Group group = new Group(null, "AJGroups1", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());
        Boolean res1 = groupService.deleteGroup(data.orgId(), res.name());
        assertTrue(res1);
    }

    @Test
    public void removeGroupWithWrongInputTest() {
        var data = signUp("ajay2@gmail.com", "AJ2");
        Group group = new Group(null, "AJGroups2", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());
        Boolean res1 = groupService.deleteGroup(data.orgId(), res.name() + "X");
        assertFalse(res1);
    }

    @Test
    public void getOrgGroupsTest() {
        var data = signUp("ajay20@gmail.com", "AJ20");
        Group group1 = new Group(null, "AJGroups", "AJ Groups", null);
        Group res1 = groupService.createGroup(data.orgId(), group1);
        assertNotNull(res1.id());

        Group group2 = new Group(null, "AJGroups2", "AJ Groups", null);
        Group res2 = groupService.createGroup(data.orgId(), group2);
        assertNotNull(res2.id());

        List<Group> response = groupService.getAllOrgGroups(data.orgId());
        assertEquals(2, response.size());
    }

    @Test
    public void addUserIntoGroupTest() {
        var data = signUp("ajay3@gmail.com", "AJ3");
        Group group = new Group(null, "AJGroups3", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest request = new GroupAssignmentRequest(res.name(), "add");
        Boolean added = groupService.handleUserInGroup(data.orgId(), data.email(), request);
        assertTrue(added);
    }

    @Test
    public void addUserIntoGroupWithWrongInputTest() {
        var data = signUp("ajay4@gmail.com", "AJ4");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest request = new GroupAssignmentRequest(res.name(), "add");
        Boolean added = groupService.handleUserInGroup(data.orgId(), "wrong-email@gmail.com", request);
        assertFalse(added);
    }

    @Test
    public void removeUserFromGroupTest() {
        var data = signUp("ajay5@gmail.com", "AJ5");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest addReq = new GroupAssignmentRequest(res.name(), "add");
        assertTrue(groupService.handleUserInGroup(data.orgId(), data.email(), addReq));

        GroupAssignmentRequest removeReq = new GroupAssignmentRequest(res.name(), "remove");
        assertTrue(groupService.handleUserInGroup(data.orgId(), data.email(), removeReq));
    }

    @Test
    public void removeUserFromGroupWithWrongInputsTest() {
        var data = signUp("ajay6@gmail.com", "AJ6");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest addReq = new GroupAssignmentRequest(res.name(), "add");
        assertTrue(groupService.handleUserInGroup(data.orgId(), data.email(), addReq));

        GroupAssignmentRequest invalidReq = new GroupAssignmentRequest(res.name(), "");
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class, () -> {
            groupService.handleUserInGroup(data.orgId(), data.email(), invalidReq);
        });
        assertTrue(err.getMessage().startsWith(invalidReq.action() + " is not a valid action"));
    }

    @Test
    public void getGroupExistingUsersTest() {
        var data = signUp("ajay7@gmail.com", "AJ7");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest addReq = new GroupAssignmentRequest(res.name(), "add");
        assertTrue(groupService.handleUserInGroup(data.orgId(), data.email(), addReq));

        List<UserWithStatus> users = groupService.getGroupUsers(data.orgId(), res.name());
        assertEquals(1, users.stream().filter(u -> u.status().equals("existing")).count());
    }

    @Test
    public void getGroupNonExistingUsersTest() {
        var data = signUp("ajay79@gmail.com", "AJ97");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        GroupAssignmentRequest addReq = new GroupAssignmentRequest(res.name(), "add");
        assertTrue(groupService.handleUserInGroup(data.orgId(), data.email(), addReq));

        List<UserWithStatus> users = groupService.getGroupUsers(data.orgId(), res.name());
        assertEquals(0, users.stream().filter(u -> u.status().equals("nonexisting")).count());
    }

    @Test
    public void getGroupUsersByWrongInputTest() {
        var data = signUp("ajay8@gmail.com", "AJ8");
        Group group = new Group(null, "AJGroups", "AJ Groups", null);
        Group res = groupService.createGroup(data.orgId(), group);
        assertNotNull(res.id());

        List<UserWithStatus> users = groupService.getGroupUsers(data.orgId() + 1, res.name());
        assertEquals(0, users.size());
    }

    // ----------------- Helpers -----------------
    private SignUpData signUp(String email, String orgName) {
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
        Long orgId = organizationService.getOrgByName(user.orgName()).id();
        return new SignUpData(orgId, email);
    }

    private record SignUpData(Long orgId, String email) {}
}
