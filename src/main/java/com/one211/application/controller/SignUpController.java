package com.one211.application.controller;

import com.one211.application.model.SignUp;
import com.one211.application.service.SignUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class SignUpController {
    private final SignUpService signUpService;

    public SignUpController(SignUpService signUpService) {
        this.signUpService = signUpService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUp signUpUser) {
        try {
            return ResponseEntity.ok(signUpService.signUpUser(signUpUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Signup failed: " + e.getMessage());
        }
    }
}
