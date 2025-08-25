package com.one211.application.service;

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
public class SignUpServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SignUpService signUpService;

    @Test
    public void signUp_withValidData_shouldSucceed() {
        SignUp newUser = createTestUser("ekantEyadav@gmail.com", "AWS");
        SignUp savedUser = signUpService.signUpUser(newUser);

        assertNotNull(savedUser);
        assertEquals("AWS", savedUser.orgName());
    }

    @Test
    public void signUp_withInvalidEmail_shouldFail() {
        SignUp invalidUser = createTestUser("@gmail.com", "ZYZ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            signUpService.signUpUser(invalidUser);
        });

        assertTrue(exception.getMessage().contains("Invalid email: "));
    }

    private SignUp createTestUser(String email, String orgName) {
        return new SignUp(
                "Gagan Taneja",
                email,
                "tanejaGagan",
                orgName,
                "Big Data Com.",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
