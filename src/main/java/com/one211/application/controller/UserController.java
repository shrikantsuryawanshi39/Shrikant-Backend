package com.one211.application.controller;

import com.one211.application.model.User;
import com.one211.application.security.JwtHelper;
import com.one211.application.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {
    private final JwtHelper jwtHelper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, JwtHelper jwtHelper, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtHelper = jwtHelper;
    }

    @GetMapping("/org/{orgId}/user")
    public ResponseEntity<List<User>> getUsersByOrgId(@PathVariable("orgId") Long orgId, @RequestParam(defaultValue = "0") int skip, @RequestParam(defaultValue = "10") int limit) {
        if (orgId == null || orgId < 1) {
            return ResponseEntity.badRequest().build();
        }
        List<User> users = userService.getUserByOrgId(orgId, limit, skip);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/org/{orgId}/user")
    public ResponseEntity<User> addUser(@PathVariable Long orgId, @RequestBody User user) {
        if (orgId == null || orgId < 1) {
            return ResponseEntity.badRequest().build();
        }
        User encodedUser = new User(user.id(), user.name(), user.email(), passwordEncoder.encode(user.password()), user.role(), user.description(), user.createdAt(), user.updatedAt());
        return ResponseEntity.ok(userService.addUser(encodedUser, orgId));
    }

    @PatchMapping("/org/{orgId}/user/{userName}")
    public ResponseEntity<?> updateUser(@PathVariable Long orgId, @PathVariable String userName, @RequestBody User user) {
        try {
            User updated = userService.updateUser(userName, user);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update user: " + e.getMessage());
        }
    }
}
