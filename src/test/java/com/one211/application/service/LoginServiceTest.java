package com.one211.application.service;

import com.one211.application.model.LoginRequest;
import com.one211.application.model.Organization;
import com.one211.application.model.SignUp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class LoginServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private LoginService loginService;

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void login_withValidCredentials_shouldSucceed() {
        SignUp toSave = createTestUser("ekant.raj@gmail.com", "ZYZAB");
        var saved = signUpService.signUpUser(toSave);

        Organization org = organizationService.getOrgByName(saved.orgName());

        LoginRequest loginRequest = new LoginRequest(saved.email(), "tanejaGagan");

        var result = loginService.login(org.id(), loginRequest);
        assertEquals(result.email(), saved.email());
    }

    @Test
    public void login_withMissingOrgId_shouldFail() {
        SignUp toSave = createTestUser("verma@gmail.com", "APC");
        var savedUser = signUpService.signUpUser(toSave);

        LoginRequest loginRequest = new LoginRequest(savedUser.email(), "tanejaGagan");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            loginService.login(null, loginRequest);
        });

        assertTrue(exception.getMessage().contains("Missing login fields."));
    }

    private SignUp createTestUser(String email, String orgName) {
        return new SignUp("Gagan Taneja", email, "tanejaGagan", orgName, "Big Data Com.", LocalDateTime.now(), LocalDateTime.now());
    }
}
