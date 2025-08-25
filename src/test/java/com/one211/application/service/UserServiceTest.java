package com.one211.application.service;

import com.one211.application.model.SignUp;
import com.one211.application.model.User;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Sql(scripts = {"/schema.sql"})
public class UserServiceTest {

    private static final AtomicInteger counter = new AtomicInteger();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void addAndRemoveUser_shouldSucceed() {
        String email = generateUniqueEmail();
        SignUp newUser = createTestUser(email, "Dance D.");
        SignUp savedUser = signUpService.signUpUser(newUser);

        assertEquals(email, savedUser.email());
        boolean deleted = userService.removeUser(savedUser.email());
        assertTrue(deleted);
    }

    @Test
    public void getUserByEmailTest() {
        String email = generateUniqueEmail();
        SignUp newUser = createTestUser(email, "Dairy");
        SignUp savedUser = signUpService.signUpUser(newUser);

        User fetchedByEmail = userService.getUserByEmail(savedUser.email());

        assertNotNull(fetchedByEmail);
        assertEquals(savedUser.email(), fetchedByEmail.email());
    }

    @Test void addUser() {
        String email = generateUniqueEmail();
        SignUp newUser = createTestUser(email, "TCS");
        SignUp savedUser = signUpService.signUpUser(newUser);

        User newUserForOrg = userService.addUser(new User(null, "Raja Ram", "raja@gmail.com", "abcdefg", "USER", "HAPPY HAPPY", LocalDateTime.now(), LocalDateTime.now()), organizationService.getOrgByName(savedUser.orgName()).id());
        assertNotNull(newUserForOrg.email());
    }

    @Test
    public void updateUser_shouldNotUpdateEmail() {
        String originalEmail = generateUniqueEmail();
        SignUp newUser = createTestUser(originalEmail, "PKJ");

        SignUp result = signUpService.signUpUser(newUser);
        Long userId = userService.getUserByEmail(result.email()).id();
        String registeredEmail = userService.getUserByEmail(result.email()).email();

        String attemptedNewEmail = generateUniqueEmail(); // This should be ignored in update

        User updateData = new User(
                userId,
                "Gagan Taneja",
                attemptedNewEmail, // Should not be updated
                "tanejaGagan",
                "ADMIN",
                "Big Data Com.",
                newUser.createdAt(),
                LocalDateTime.now()
        );

        User updatedUser = userService.updateUser(registeredEmail, updateData);

        assertNotNull(updatedUser);
        assertEquals(registeredEmail, updatedUser.email()); // Email should remain unchanged
        assertEquals(userId, updatedUser.id());
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

    private String generateUniqueEmail() {
        return "user" + counter.incrementAndGet() + "@example.com";
    }
}
