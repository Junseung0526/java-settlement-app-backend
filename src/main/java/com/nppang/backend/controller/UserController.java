package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateUserRequest;
import com.nppang.backend.dto.UpdateUserRequest;
import com.nppang.backend.entity.AppUser;
import com.nppang.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public CompletableFuture<ResponseEntity<AppUser>> createUser(@RequestBody CreateUserRequest request) {
        return userService.findOrCreateUser(request.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<AppUser>>> getAllUsers() {
        return userService.findAllUsers().thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<AppUser>> getUser(@PathVariable String userId) {
        return userService.findUserById(userId)
                .thenApply(user -> user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Void>> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
        return userService.updateUser(userId, request.getUsername())
                .thenApply(v -> ResponseEntity.ok().<Void>build())
                .exceptionally(ex -> ResponseEntity.status(500).build());
    }
}
