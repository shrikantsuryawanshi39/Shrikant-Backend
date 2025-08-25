package com.one211.application.service;

import com.one211.application.model.Cluster;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClusterService {

    private static final String GET_CLUSTER_QUERY = "SELECT * FROM cluster WHERE org_id = ? AND name = ?";
    private static final String INSERT_CLUSTER_QUERY = "INSERT INTO cluster (org_id, name, description, active, creation_time) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_CLUSTER_QUERY = "UPDATE cluster SET name = ?, description = ?, active = ? WHERE name = ? AND org_id = ?";
    private static final String DELETE_CLUSTER_BY_NAME = "DELETE FROM cluster WHERE name = ? AND org_id = ?";
    private static final String GET_ALL_CLUSTERS_QUERY = "SELECT * FROM cluster WHERE org_id = ?";
    private final JdbcTemplate jdbc;

    public ClusterService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Cluster addCluster(Long orgId, Cluster cluster) {
        LocalDateTime now = LocalDateTime.now();
        Timestamp creationTimestamp = Timestamp.valueOf(now);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        INSERT_CLUSTER_QUERY,
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, orgId);
                ps.setString(2, cluster.name());
                ps.setString(3, cluster.description());
                ps.setBoolean(4, cluster.status());
                ps.setTimestamp(5, creationTimestamp);
                return ps;
            }, keyHolder);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "Cluster with name '" + cluster.name() + "' already exists for this organization."
            );
        }

        Long generatedId = extractGeneratedId(keyHolder);
        return new Cluster(generatedId, orgId, cluster.name(), cluster.description(), cluster.status(), now);
    }

    private Long extractGeneratedId(KeyHolder keyHolder) {
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            Object id = keyHolder.getKeys().get("id");
            if (id instanceof Number number) {
                return number.longValue();
            }
        }

        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new IllegalStateException("Failed to retrieve generated cluster ID from database");
    }

    public Cluster getClusterByName(Long orgId, String name) {
        try {
            return jdbc.queryForObject(GET_CLUSTER_QUERY, new Object[]{orgId, name}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Cluster> getAllClusters(Long orgId) {
        return jdbc.query(GET_ALL_CLUSTERS_QUERY, new Object[]{orgId}, (rs, rowNum) -> mapRow(rs));
    }

    @Transactional
    public Cluster updateCluster(Long orgId, String clusterName, Cluster updatedInput) {
        Cluster existing = getClusterByName(orgId, clusterName);
        if (existing == null) {
            throw new IllegalArgumentException("Cluster does not exist");
        }

        Cluster updated = new Cluster(
                updatedInput.id(), orgId,
                updatedInput.name() != null ? updatedInput.name() : existing.name(),
                updatedInput.description() != null ? updatedInput.description() : existing.description(),
                updatedInput.status() != null ? updatedInput.status() : existing.status(),
                existing.createdAt()
        );

        int rows = jdbc.update(UPDATE_CLUSTER_QUERY, updated.name(), updated.description(), updated.status(), clusterName, orgId);
        return rows > 0 ? getClusterByName(updated.orgId(), updated.name()) : null;
    }

    public boolean deleteCluster(Long orgId, String clusterName) {
        int result = jdbc.update(DELETE_CLUSTER_BY_NAME, clusterName, orgId);
        return result != 0;
    }

    private Cluster mapRow(ResultSet rs) throws SQLException {
        return new Cluster(
                rs.getLong("id"),
                rs.getLong("org_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"),
                rs.getTimestamp("creation_time").toLocalDateTime()
        );
    }
}
