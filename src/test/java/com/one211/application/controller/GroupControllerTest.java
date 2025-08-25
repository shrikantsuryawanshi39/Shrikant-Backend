package com.one211.application.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.application.model.Group;
import com.one211.application.model.GroupAssignmentRequest;
import com.one211.application.model.LoginRequest;
import com.one211.application.model.SignUp;
import com.one211.application.service.OrganizationService;
import com.one211.application.service.UserService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class GroupControllerTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;

    @Test
    public void createGroupTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan@gmail.com", "shyam");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        assertNotNull(groupName);
    }

    @Test
    public void createGroupWithNullFieldTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan1@gmail.com", "shyam1");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, null, "xyz test group", null);

        mockMvc.perform(post("/api/orgs/{orgId}/groups", orgEmail.getFirst())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteGroupTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan2@gmail.com", "shyam2");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz1", "xyz test group", null);

        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        assertNotNull(groupName);
        Boolean response = removeGroup((Long) orgEmail.getFirst(), groupName, token);
        assertTrue(response);
    }

    @Test
    public void deleteGroupWithWrongInputTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan3@gmail.com", "shyam3");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz1", "xyz test group", null);

        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        assertNotNull(groupName);
        Boolean response = removeGroup((Long) orgEmail.getFirst(), groupName + 10, token);
        assertFalse(response);
    }

    @Test
    public void addUserToGroupTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan4@gmail.com", "shyam4");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz4", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        assertNotNull(groupName);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        Boolean add = handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);
        assertTrue(add);
    }

    @Test
    public void addUserToGroupWithWrongInputTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan5@gmail.com", "shyam5");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz5", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);

        List<?> orgEmail2 = signUp("shyammohan44@gmail.com", "shyam44");
        String userName2 = userService.getUserByEmail((String) orgEmail2.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        Boolean add = handleUserInGroup((Long) orgEmail.getFirst(), userName2, token, request);
        assertFalse(add);
    }

    @Test
    public void removeUserFromGroupTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan6@gmail.com", "shyam6");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz6", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        Boolean add = handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);
        assertTrue(add);
        GroupAssignmentRequest request2 = new GroupAssignmentRequest(groupName, "remove");
        Boolean remove = handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request2);
        assertTrue(remove);
    }

    @Test
    public void removeUserFromGroupWithWrongInputTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan7@gmail.com", "shyam7");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz7", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        Boolean add = handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);
        assertTrue(add);
        List<?> orgEmail2 = signUp("shyammohan77@gmail.com", "shyam77");
        String userName2 = userService.getUserByEmail((String) orgEmail2.getLast()).email();

        GroupAssignmentRequest request2 = new GroupAssignmentRequest(groupName, "remove");
        Boolean remove = handleUserInGroup((Long) orgEmail2.getFirst(), userName2, token, request2);
        assertFalse(remove);
    }

    @Test
    public void getAllOrgGroupTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan80@gmail.com", "shyam08");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz8", "xyz test group", null);
        createGroup((Long) orgEmail.getFirst(), token, group);
        Group group2 = new Group(null, "xyz", "xyz test group", null);
        createGroup((Long) orgEmail.getFirst(), token, group2);

        mockMvc.perform(get("/api/orgs/{orgId}/groups", orgEmail.getFirst())
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void getGroupUsersTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan8@gmail.com", "shyam8");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz8", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        Boolean add = handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);
        assertTrue(add);
        Long existing = getUsersCountInGroup((Long) orgEmail.getFirst(), groupName, token, "existing");
        Long nonExisting = getUsersCountInGroup((Long) orgEmail.getFirst(), groupName, token, "nonexisting");
        assertEquals(1, existing + nonExisting);
    }

    @Test
    public void getGroupExistingUsersTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan9@gmail.com", "shyam9");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz9", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);

        Long existing = getUsersCountInGroup((Long) orgEmail.getFirst(), groupName, token, "existing");
        assertEquals(1, existing);
    }

    @Test
    public void getGroupNonExistingUsersTest() throws Exception {
        List<?> orgEmail = signUp("shyammohan10@gmail.com", "shyam10");
        String token = loginTestUser((String) orgEmail.getLast(), (Long) orgEmail.getFirst());
        Group group = new Group(null, "xyz10", "xyz test group", null);
        String groupName = createGroup((Long) orgEmail.getFirst(), token, group);
        String userName = userService.getUserByEmail((String) orgEmail.getLast()).email();

        GroupAssignmentRequest request = new GroupAssignmentRequest(groupName, "add");
        handleUserInGroup((Long) orgEmail.getFirst(), userName, token, request);

        Long nonExisting = getUsersCountInGroup((Long) orgEmail.getFirst(), groupName, token, "nonexisting");
        assertEquals(0, nonExisting);
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

    private String createGroup(Long orgId, String token, Group group) throws Exception {
        String result = mockMvc.perform(post("/api/orgs/{orgId}/groups", orgId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(result);
        return jsonNode.get("name").asText();
    }

    private Boolean removeGroup(Long orgId, String groupName, String token) throws Exception {
        String result = mockMvc.perform(delete("/api/orgs/{orgId}/groups/{groupName}", orgId, groupName)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Boolean.parseBoolean(result);
    }

    private Boolean handleUserInGroup(Long orgId, String userName, String token, GroupAssignmentRequest request) throws Exception {
        String result = mockMvc.perform(post("/api/orgs/{orgId}/user/{userName}", orgId, userName)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Boolean.parseBoolean(result);
    }

    private Long getUsersCountInGroup(Long orgId, String  groupName, String token, String status) throws Exception {
        String res = mockMvc.perform(get("/api/orgs/{orgId}/groups/{groupName}", orgId, groupName)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list = mapper.readValue(res, new TypeReference<>() {});
        return list.stream().filter(item -> status.equals(item.get("status"))).count();
    }
}
