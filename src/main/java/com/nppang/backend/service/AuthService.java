package com.nppang.backend.service;

import com.nppang.backend.config.JwtUtil;
import com.nppang.backend.dto.JwtResponse;
import com.nppang.backend.dto.LoginRequest;
import com.nppang.backend.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public CompletableFuture<JwtResponse> login(LoginRequest loginRequest) {
        return userService.findUserByUsername(loginRequest.getUsername())
                .thenApply(user -> {
                    if (user == null) {
                        throw new RuntimeException("User not found.");
                    }

                    // [중요] 암호화된 비밀번호 비교
                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        throw new RuntimeException("Invalid password.");
                    }

                    // 비밀번호 일치 -> JWT 생성
                    String jwt = jwtUtil.generateToken(user.getId(), user.getUsername());

                    return new JwtResponse(jwt, user.getId(), user.getUsername());
                });
    }
}