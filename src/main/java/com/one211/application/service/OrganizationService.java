package com.one211.application.service;

import com.one211.application.model.Organization;
import com.one211.application.model.UserOrg;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationService {
    private static final String INSERT_ORG_QUERY = "INSERT INTO organization (name, description, created_at, updated_at) VALUES (?, ?, ?, ?) RETURNING id";
    private static final String GET_ORG_BY_ID_QUERY = "SELECT * FROM organization WHERE id = ?";
    private static final String GET_ORG_BY_NAME_QUERY = "SELECT * FROM organization WHERE name = ?";
    private static final String DELETE_ORG_QUERY = "DELETE FROM organization WHERE id = ?";
    private static final String UPDATE_ORG_QUERY = "UPDATE organization SET name = ?, description = ?, updated_at = ? WHERE id = ?";

    private final JdbcTemplate jdbc;

    public OrganizationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Organization addOrg(Organization org) {
        if (isBlank(org.name())) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_ORG_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, org.name());
            ps.setString(2, org.description());
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setTimestamp(4, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated ID for organization");
        }

        return new Organization(key.longValue(), org.name(), org.description(), now, now);
    }

    public Organization getOrgById(Long id) {
        try {
            return jdbc.queryForObject(GET_ORG_BY_ID_QUERY, this::mapRow, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Organization getOrgByName(String name) {
        try {
            return jdbc.queryForObject(GET_ORG_BY_NAME_QUERY, this::mapRow, name);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean deleteOrg(Long orgId) {
        int rowsAffected = jdbc.update(DELETE_ORG_QUERY, orgId);
        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Organization with ID " + orgId + " not found.");
        }
        return true;
    }

    public Organization update(Organization org) {
        if (org.id() == null) {
            throw new IllegalArgumentException("Organization ID is required for update");
        }

        int rows = jdbc.update(
                UPDATE_ORG_QUERY,
                org.name(),
                org.description(),
                Timestamp.valueOf(org.updatedAt()),
                org.id()
        );

        return rows > 0 ? getOrgById(org.id()) : null;
    }

    private Organization mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Organization(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
