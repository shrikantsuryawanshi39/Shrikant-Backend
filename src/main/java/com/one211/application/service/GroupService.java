package com.one211.application.service;

import com.one211.application.model.Group;
import com.one211.application.model.GroupAssignmentRequest;
import com.one211.application.model.User;
import com.one211.application.model.UserWithStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GroupService {

    private static final String INSERT_GROUP_QUERY = "INSERT INTO \"group\" (name, description, org_id, creation_time) VALUES (?, ?, ?, ?)";
    private static final String DELETE_GROUP_QUERY = "DELETE FROM \"group\" WHERE org_id = ? AND name = ?";
    private static final String INSERT_USER_GROUP_QUERY =
            "INSERT INTO user_group (user_name, group_name, org_id) " +
                    "SELECT u.email, g.name, g.org_id " +
                    "FROM \"user\" u " +
                    "JOIN user_org uo ON u.email = uo.user_name " +
                    "JOIN \"group\" g ON g.name = ? AND g.org_id = ? " +
                    "WHERE u.email = ? AND uo.org_id = g.org_id";
    private static final String DELETE_USER_GROUP_QUERY = "DELETE FROM user_group WHERE user_name = ? AND group_name = ? AND org_id = ?";
    private static final String GET_ALL_EXISTING_NONEXISTING_USER = "SELECT u.*, CASE WHEN EXISTS ( SELECT 1 FROM user_group ug WHERE ug.user_name = u.email AND ug.group_name = ? AND ug.org_id = ? ) THEN 'existing' ELSE 'nonexisting' END AS status FROM \"user\" u JOIN user_org uo ON u.email = uo.user_name WHERE uo.org_id = ?";
    private static final String GET_ALL_ORG_GROUPS = "SELECT * FROM \"group\" WHERE org_id = ?";
    private final JdbcTemplate jdbc;

    public GroupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Group createGroup(Long orgId, Group group) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(INSERT_GROUP_QUERY, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, group.name());
                ps.setString(2, group.description());
                ps.setLong(3, orgId);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, keyHolder);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "Group with name '" + group.name() + "' may already exist in organization " + orgId, e);
        }

        Long generatedId = extractGeneratedId(keyHolder);
        return Group.withId(generatedId, group);
    }

    public Boolean deleteGroup(Long orgId, String groupName) {
        try {
            return jdbc.update(DELETE_GROUP_QUERY, orgId, groupName) > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting group", e);
        }
    }

    public List<Group> getAllOrgGroups(Long orgId) {
        return jdbc.query(GET_ALL_ORG_GROUPS, ps -> ps.setLong(1, orgId), this::mapRowForGroup);
    }

    public Boolean handleUserInGroup(Long orgId, String userEmail, GroupAssignmentRequest request) {
        if (orgId == null || request.name() == null || userEmail == null) {
            throw new IllegalArgumentException("orgId, groupName, and userEmail must not be null");
        }

        try {
            if ("add".equalsIgnoreCase(request.action())) {
                return jdbc.update(con -> {
                    PreparedStatement ps = con.prepareStatement(INSERT_USER_GROUP_QUERY);
                    ps.setString(1, request.name());   // g.name
                    ps.setLong(2, orgId);              // g.org_id
                    ps.setString(3, userEmail);        // u.email
                    return ps;
                }) > 0;
            } else if ("remove".equalsIgnoreCase(request.action())) {
                return jdbc.update(con -> {
                    PreparedStatement ps = con.prepareStatement(DELETE_USER_GROUP_QUERY);
                    ps.setString(1, userEmail);        // user_name
                    ps.setString(2, request.name());   // group_name
                    ps.setLong(3, orgId);
                    return ps;
                }) > 0;
            } else {
                throw new IllegalArgumentException(request.action() + " is not a valid action");
            }
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IllegalStateException("User is already in the group", e);
        }
    }

    public List<UserWithStatus> getGroupUsers(Long orgId, String groupName) {
        return jdbc.query(GET_ALL_EXISTING_NONEXISTING_USER, ps -> {
            ps.setString(1, groupName);
            ps.setLong(2, orgId);
            ps.setLong(3, orgId);
        }, this::mapRow);
    }

    private Long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            return ((Number) keys.get("id")).longValue();
        }
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new IllegalStateException("Failed to retrieve generated group id");
    }

    private UserWithStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
        return new UserWithStatus(user, rs.getString("status"));
    }

    private Group mapRowForGroup(ResultSet rs, int rowNum) throws SQLException {
        return new Group(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("creation_time").toLocalDateTime()
        );
    }
}
