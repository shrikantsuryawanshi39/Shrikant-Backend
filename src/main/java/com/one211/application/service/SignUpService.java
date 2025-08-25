package com.one211.application.service;

import com.one211.application.model.SignUp;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class SignUpService {
    private static final String INSERT_USER_QUERY = "INSERT INTO \"user\" (name, email, password, role, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_ORG_QUERY = "INSERT INTO organization (name, description, created_at, updated_at) VALUES (?, ?, ?, ?)";
    private static final String INSERT_ORG_USER_QUERY = "INSERT INTO user_org (user_name, org_id, role, active, creation_time) VALUES (?, ?, ?, ?, ?)";

    private static final String DEFAULT_ROLE = "ADMIN";
    private static final boolean ORG_ACTIVE = true;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public SignUpService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignUp signUpUser(SignUp user) {
        validateInput(user);
        LocalDateTime now = LocalDateTime.now();

        SignUp encodedUser = new SignUp(
                user.name(),
                user.email(),
                passwordEncoder.encode(user.password()),
                user.orgName(),
                user.orgDescription(),
                now,
                now
        );

        // Insert user (returns email as natural key)
        String userEmail = insertUser(encodedUser, now);

        // Insert org (returns generated org_id)
        Long orgId = insertOrganization(encodedUser, now);

        // Link user to org
        linkUserToOrganization(userEmail, orgId, now);

        return encodedUser;
    }

    private String insertUser(SignUp user, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(INSERT_USER_QUERY);
                ps.setString(1, user.name());
                ps.setString(2, user.email());
                ps.setString(3, user.password());
                ps.setString(4, DEFAULT_ROLE);
                ps.setString(5, user.orgDescription());
                ps.setTimestamp(6, Timestamp.valueOf(now));
                ps.setTimestamp(7, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Email already exists: " + user.email(), e);
        }
        return user.email();
    }

    private Long insertOrganization(SignUp user, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(INSERT_ORG_QUERY, new String[]{"id"});
                ps.setString(1, user.orgName());
                ps.setString(2, user.orgDescription());
                ps.setTimestamp(3, Timestamp.valueOf(now));
                ps.setTimestamp(4, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Organization already exists: " + user.orgName(), e);
        }
        return extractGeneratedId(keyHolder, "organization");
    }

    private void linkUserToOrganization(String userEmail, Long orgId, LocalDateTime now) {
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT_ORG_USER_QUERY);
            ps.setString(1, userEmail);
            ps.setLong(2, orgId);
            ps.setString(3, DEFAULT_ROLE);
            ps.setBoolean(4, ORG_ACTIVE);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            return ps;
        });
    }

    private Long extractGeneratedId(KeyHolder keyHolder, String source) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to generate ID for: " + source);
        }
        return key.longValue();
    }

    private void validateInput(SignUp user) {
        if (isBlank(user.name())) throw new IllegalArgumentException("Name is required.");
        if (isBlank(user.email())) throw new IllegalArgumentException("Email is required.");
        if (!isValidEmail(user.email())) throw new IllegalArgumentException("Invalid email: " + user.email());
        if (isBlank(user.password())) throw new IllegalArgumentException("Password is required.");
        if (isBlank(user.orgName())) throw new IllegalArgumentException("Organization name is required.");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
