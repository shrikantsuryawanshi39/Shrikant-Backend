package com.one211.application.service;

import com.one211.application.model.Cluster;
import com.one211.application.model.ClusterWithAction;
import com.one211.application.model.ClusterAssignmentRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class ClusterAssignmentService {

    private static final String INSERT_CLUSTER_ASSOCIATION =
            "INSERT INTO cluster_association (source_name, source_type, cluster_name, org_id, creation_time) " +
                    "VALUES (?, ?, ?, ?, ?)";

    private static final String DELETE_CLUSTER_ASSOCIATION =
            "DELETE FROM cluster_association WHERE source_name = ? AND source_type = ? AND cluster_name = ? AND org_id = ?";

    private static final String GET_ALL_CLUSTER_ASSOCIATIONS =
            "SELECT c.*, CASE WHEN ca.source_name IS NOT NULL THEN 'assign' ELSE 'unassign' END AS action " +
                    "FROM cluster c " +
                    "LEFT JOIN cluster_association ca " +
                    "ON c.name = ca.cluster_name AND ca.source_name = ? AND ca.source_type = ? " +
                    "WHERE c.org_id = ?";

    private final JdbcTemplate jdbc;

    public ClusterAssignmentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Assign or unassign a cluster for either USER or GROUP.
     */
    public boolean updateAssignment(Long orgId, ClusterAssignmentRequest request) {
        try {
            String action = request.action();
            if ("assign".equalsIgnoreCase(action)) {
                return jdbc.update(INSERT_CLUSTER_ASSOCIATION,
                        request.sourceName(), request.sourceType(), request.name(), orgId, currentTimestamp()) > 0;
            } else if ("unassign".equalsIgnoreCase(action)) {
                return jdbc.update(DELETE_CLUSTER_ASSOCIATION,
                        request.sourceName(), request.sourceType(), request.name(), orgId) > 0;
            } else {
                throw new IllegalArgumentException("Invalid action: " + action);
            }
        } catch (DataAccessException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all clusters with assignment status for a given USER or GROUP.
     */
    public List<ClusterWithAction> allClusters(Long orgId, ClusterAssignmentRequest request) {
        return jdbc.query(GET_ALL_CLUSTER_ASSOCIATIONS, ps -> {
            ps.setString(1, request.sourceName());
            ps.setString(2, request.sourceType());
            ps.setLong(3, orgId);
        }, this::mapRow);
    }

    private ClusterWithAction mapRow(ResultSet rs, int rowNum) throws SQLException {
        Cluster cluster = new Cluster(
                rs.getLong("id"),
                rs.getLong("org_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"),
                rs.getTimestamp("creation_time").toLocalDateTime()
        );
        return new ClusterWithAction(cluster, rs.getString("action"));
    }

    private Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}
