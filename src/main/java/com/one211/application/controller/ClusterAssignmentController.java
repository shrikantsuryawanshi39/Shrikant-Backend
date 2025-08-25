package com.one211.application.controller;

import com.one211.application.model.ClusterWithAction;
import com.one211.application.model.ClusterAssignmentRequest;
import com.one211.application.service.ClusterAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ClusterAssignmentController {

    private final ClusterAssignmentService clusterAssignmentService;

    public ClusterAssignmentController(ClusterAssignmentService clusterAssignmentService) {
        this.clusterAssignmentService = clusterAssignmentService;
    }

    /**
     * Toggle assignment for USER or GROUP
     */
    @PostMapping("/orgs/{orgId}/cluster-assignments")
    public ResponseEntity<Boolean> toggleAssignment(
            @PathVariable Long orgId,
            @RequestBody ClusterAssignmentRequest request) {

        if (orgId == null || request.sourceType() == null || request.sourceName() == null || request.name() == null || request.action() == null) {
            throw new IllegalArgumentException("orgId, sourceType, sourceName, name, and action cannot be null for Assignment/Unassignment");
        }

        return ResponseEntity.ok(
                clusterAssignmentService.updateAssignment(orgId, request)
        );
    }

    /**
     * Get all clusters for USER or GROUP
     */
    @GetMapping("/orgs/{orgId}/cluster-assignments")
    public ResponseEntity<List<ClusterWithAction>> getClusters(
            @PathVariable Long orgId,
            @RequestBody ClusterAssignmentRequest request) {

        if (orgId == null || request.sourceType() == null || request.sourceName() == null) {
            throw new IllegalArgumentException("orgId, sourceType, and sourceName must be provided");
        }

        return ResponseEntity.ok(
                clusterAssignmentService.allClusters(orgId, request)
        );
    }
}
