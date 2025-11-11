package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateUserRequest;
import com.nppang.backend.dto.UpdateUserRequest;
import com.nppang.backend.entity.AppUser;
import com.nppang.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<AppUser> createUser(@RequestBody CreateUserRequest request) {
        // [수정] .join()을 호출하여 비동기 작업이 끝날 때까지 "기다립니다".
        AppUser user = userService.findOrCreateUser(request.getUsername()).join();
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<AppUser>> getAllUsers() {
        // [수정] .join()을 호출하여 비동기 작업이 끝날 때까지 "기다립니다".
        // 이 스레드(스레드 A)는 SecurityContext를 잃어버리지 않습니다.
        List<AppUser> users = userService.findAllUsers().join();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AppUser> getUser(@PathVariable String userId) {
        // [수정] .join() 사용
        AppUser user = userService.findUserById(userId).join();
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
        // [수정] .join()으로 동기식 대기, 반환 타입 ResponseEntity<Void>
        userService.updateUser(userId, request.getUsername()).join();
        return ResponseEntity.ok().build();
    }
}
