package com.one211.application.service;

import com.one211.application.model.LoginRequest;
import com.one211.application.model.UserOrg;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class LoginService {
    private static final String GET_USER_ORG_BY_EMAIL_AND_ORG_QUERY = "SELECT u.id AS user_id, u.name AS user_name, u.email AS user_email, u.password, uo.role, o.id AS org_id, o.name AS org_name FROM \"user\" u JOIN user_org uo ON u.email = uo.user_name JOIN organization o ON uo.org_id = o.id WHERE LOWER(u.email) = LOWER(?) AND o.id = ?";
    private static final String GET_ORG_BY_USER_QUERY = "SELECT o.id AS org_id, o.name AS org_name, o.description AS org_description, u.email AS user_email, u.password, u.name AS user_name FROM user_org uo JOIN \"user\" u ON uo.user_name = u.email JOIN organization o ON uo.org_id = o.id WHERE u.email = ?";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public LoginService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserOrg> getOrgsByUser(String email) {
        return jdbc.query(GET_ORG_BY_USER_QUERY, new Object[]{email}, (rs, rowNum) ->
                new UserOrg(rs.getString("user_name"), rs.getString("user_email"), rs.getString("password"), null, rs.getLong("org_id"), rs.getString("org_name"))
        );
    }

    public UserOrg login(Long orgId, LoginRequest loginRequest) {
        if (loginRequest.email() == null || loginRequest.password() == null || orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing login fields.");
        }

        UserOrg userOrg = findUserOrgByEmailAndOrgId(loginRequest.email(), orgId);
        if (userOrg == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with given email and organization ID.");
        }

        if (!passwordEncoder.matches(loginRequest.password(), userOrg.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password.");
        }

        return userOrg;
    }

    private UserOrg findUserOrgByEmailAndOrgId(String email, Long orgId) {
        try {
            return jdbc.queryForObject(
                    GET_USER_ORG_BY_EMAIL_AND_ORG_QUERY,
                    new Object[]{email.trim(), orgId},
                    this::mapUserOrgRow
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private UserOrg mapUserOrgRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserOrg(
                rs.getString("user_name"),
                rs.getString("user_email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getLong("org_id"),
                rs.getString("org_name")
        );
    }
}
