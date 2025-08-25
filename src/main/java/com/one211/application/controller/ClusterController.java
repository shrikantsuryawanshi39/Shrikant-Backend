package com.one211.application.controller;

import com.one211.application.model.Cluster;
import com.one211.application.security.JwtHelper;
import com.one211.application.service.ClusterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ClusterController {

    private final ClusterService clusterService;

    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @PostMapping("/org/{orgId}/cluster")
    public ResponseEntity<Cluster> createCluster(@PathVariable Long orgId, @RequestBody Cluster cluster) {
        Cluster created = clusterService.addCluster(orgId, cluster);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/org/{orgId}/cluster/{clusterName}")
    public ResponseEntity<Cluster> getCluster(@PathVariable Long orgId, @PathVariable String clusterName) {
        Cluster cluster = clusterService.getClusterByName(orgId, clusterName);
        return ResponseEntity.ok(cluster);
    }

    @GetMapping("/org/{orgId}/cluster")
    public ResponseEntity<List<Cluster>> getAllClusters(@PathVariable Long orgId) {
        List<Cluster> clusters = clusterService.getAllClusters(orgId);
        return ResponseEntity.ok(clusters);
    }

    @PatchMapping("/org/{orgId}/cluster/{clusterName}")
    public ResponseEntity<Cluster> updateCluster(
            @PathVariable Long orgId,
            @PathVariable String clusterName,
            @RequestBody Cluster updatedCluster
    ) {
        Cluster updated = clusterService.updateCluster(orgId, clusterName, updatedCluster);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/org/{orgId}/cluster/{clusterName}")
    public ResponseEntity<Void> deleteCluster(@PathVariable Long orgId, @PathVariable String clusterName) {
        boolean deleted = clusterService.deleteCluster(orgId, clusterName);
        if (deleted) {
            return  ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
