package com.one211.application.controller;

import com.one211.application.model.LoginRequest;
import com.one211.application.model.UserOrg;
import com.one211.application.security.CustomUserDetails;
import com.one211.application.security.JwtHelper;
import com.one211.application.service.LoginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class LoginController {
    private final LoginService loginService;
    private final JwtHelper helper;
    private final PasswordEncoder passwordEncoder;

    public LoginController(LoginService loginService, JwtHelper helper, PasswordEncoder passwordEncoder) {
        this.loginService = loginService;
        this.helper = helper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<List<UserOrg>> getUserOrgs(@RequestBody LoginRequest credential) {
        List<UserOrg> user = loginService.getOrgsByUser(credential.email());
        if (user.isEmpty() || !passwordEncoder.matches(credential.password(), user.getFirst().password())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login/org/{orgId}")
    public ResponseEntity<Map<String, String>> login(@PathVariable Long orgId, @RequestBody LoginRequest loginRequest, HttpServletResponse httpResponse) {
        if (orgId == null || loginRequest.email() == null || loginRequest.password() == null) {
            throw new IllegalArgumentException("Invalid credentials provided.");
        }

        // Step 1: Authenticate user
        UserOrg authenticatedUser = loginService.login(orgId, loginRequest);

        // Step 2: Create UserDetails and generate JWT
        UserDetails userDetails = new CustomUserDetails(authenticatedUser);
        String token = this.helper.generateToken(userDetails);

        // Step 3: Set secure cookie (with SameSite workaround)
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        int jwtExpiry = (int) JwtHelper.JWT_TOKEN_VALIDITY;
        cookie.setMaxAge(jwtExpiry);

        httpResponse.addCookie(cookie);

        String cookieHeader = String.format("token=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=%d", token, 10 * 60 * 60);
        httpResponse.setHeader("Set-Cookie", cookieHeader);

        return ResponseEntity.ok(Map.of("token", "Bearer " + token));
    }
}
