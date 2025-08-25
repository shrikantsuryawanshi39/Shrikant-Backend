package com.one211.application.service;

import com.one211.application.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private static final String INSERT_USER_QUERY = "INSERT INTO \"user\" (name, email, password, role, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id, name, email, password, role, description, created_at, updated_at";
    private static final String UPDATE_USER_QUERY = "UPDATE \"user\" SET name = ?, email = ?, password = ?, role = ?, description = ?, updated_at = ? WHERE email = ?";
    private static final String DELETE_USER_QUERY = "DELETE FROM \"user\" WHERE email = ?";
    private static final String GET_USER_BY_EMAIL_QUERY = "SELECT * FROM \"user\" WHERE LOWER(email) = LOWER(?)";
    private static final String GET_USERS_BY_ORG_ID_QUERY = "SELECT u.* FROM user_org uo JOIN \"user\" u ON uo.user_name = u.email WHERE uo.org_id = ? LIMIT ? OFFSET ?";
    private static final String INSERT_ORG_USER_QUERY = "INSERT INTO user_org (user_name, org_id, role, active, creation_time) VALUES (?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public UserService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User addUser(User user, Long orgId) {
        User newUser = insertUser(user);
        linkUserToOrganization(newUser, orgId);
        return newUser;
    }

    private User insertUser(User user) {
        try {
            LocalDateTime now = LocalDateTime.now();
            return jdbc.queryForObject(INSERT_USER_QUERY, (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("password"), rs.getString("role"), rs.getString("description"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime()), user.name(), user.email(), user.password(), user.role(), user.description(), Timestamp.valueOf(now), Timestamp.valueOf(now));
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Email already exists: " + user.email(), e);
        }
    }

    private void linkUserToOrganization(User user, Long orgId) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT_ORG_USER_QUERY);
            ps.setString(1, user.email());
            ps.setLong(2, orgId);
            ps.setString(3, user.role());
            ps.setBoolean(4, true);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            return ps;
        });
    }

    public boolean removeUser(String email) {
        return jdbc.update(DELETE_USER_QUERY, email) > 0;
    }

    public List<User> getUserByOrgId(Long orgId, int limit, int skip) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        int effectiveSkip = Math.max(skip, 0);

        return jdbc.query(GET_USERS_BY_ORG_ID_QUERY, new Object[]{orgId, effectiveLimit, effectiveSkip}, this::mapUserFromResultSet);
    }

    public User updateUser(String email, User user) {
        User existing = getUserByEmail(email);
        if (existing == null) {
            throw new IllegalStateException("User not found for update");
        }

        String name = isBlank(user.name()) ? existing.name() : user.name().trim();
        String newEmail = isBlank(user.email()) ? existing.email() : user.email().trim();
        String description = user.description() != null ? user.description() : existing.description();

        String password = user.password();
        if (isBlank(password)) {
            password = existing.password();
        } else if (!passwordEncoder.matches(password, existing.password())) {
            password = passwordEncoder.encode(password);
        } else {
            password = existing.password();
        }

        User updated = new User(existing.id(), name, email, password, existing.role(), description, existing.createdAt(), LocalDateTime.now());

        int rows = jdbc.update(UPDATE_USER_QUERY, updated.name(), updated.email(), updated.password(), updated.role(), updated.description(), Timestamp.valueOf(updated.updatedAt()), existing.email());

        return rows > 0 ? getUserByEmail(updated.email()) : null;
    }

    public User getUserByEmail(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email must not be empty");
        }

        return jdbc.query(GET_USER_BY_EMAIL_QUERY, new Object[]{email}, this::mapUserFromResultSet).stream().findFirst().orElse(null);
    }

    private User mapUserFromResultSet(ResultSet rs, int rowNum) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("password"), rs.getString("role"), rs.getString("description"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
