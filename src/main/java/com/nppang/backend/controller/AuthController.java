package com.nppang.backend.controller;

import com.nppang.backend.dto.JwtResponse;
import com.nppang.backend.dto.LoginRequest;
import com.nppang.backend.dto.SignUpRequest;
import com.nppang.backend.entity.AppUser;
import com.nppang.backend.service.AuthService;
import com.nppang.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService; // (5단계에서 생성)

    // 회원가입 API
    @PostMapping("/signup")
    public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody SignUpRequest signUpRequest) {
        return userService.registerUser(signUpRequest)
                .thenApply(user -> ResponseEntity.ok("User registered successfully! User ID: " + user.getId()))
                .exceptionally(ex -> ResponseEntity.badRequest().body(ex.getMessage()));
    }

    // 로그인 API
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<JwtResponse>> authenticateUser(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(401).body(null)); // 401 Unauthorized
    }
}