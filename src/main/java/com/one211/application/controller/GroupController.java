package com.one211.application.controller;

import com.one211.application.model.GroupAssignmentRequest;
import com.one211.application.model.Group;
import com.one211.application.model.UserWithStatus;
import com.one211.application.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/orgs/{orgId}/groups")
    public ResponseEntity<Group> createGroup(@PathVariable Long orgId, @RequestBody Group group) {
        if (orgId == null || group.name() == null || group.description() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(groupService.createGroup(orgId, group));
    }

    @DeleteMapping("/orgs/{orgId}/groups/{groupName}")
    public ResponseEntity<Boolean> deleteGroup(@PathVariable Long orgId, @PathVariable String groupName) {
        if (orgId == null || groupName == null) {
            return ResponseEntity.badRequest().body(false);
        }
        return ResponseEntity.ok(groupService.deleteGroup(orgId, groupName));
    }

    @GetMapping("/orgs/{orgId}/groups")
    public ResponseEntity<List<Group>> getOrgAllGroups(@PathVariable Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("Invalid org Id " + null);
        }
        return ResponseEntity.ok(groupService.getAllOrgGroups(orgId));
    }

    @PostMapping("/orgs/{orgId}/user/{userName}")
    public ResponseEntity<Boolean> handleUserInGroup(@PathVariable Long orgId, @PathVariable String userName, @RequestBody GroupAssignmentRequest request) {
        if (orgId == null || userName == null || request.name() == null || request.action() == null) {
            return ResponseEntity.badRequest().body(false);
        }
        return ResponseEntity.ok(groupService.handleUserInGroup(orgId, userName, request));
    }

    @GetMapping("/orgs/{orgId}/groups/{groupName}")
    public ResponseEntity<List<UserWithStatus>> getUsers(@PathVariable Long orgId, @PathVariable String groupName) {
        if (orgId == null || groupName == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(groupService.getGroupUsers(orgId, groupName));
    }
}
