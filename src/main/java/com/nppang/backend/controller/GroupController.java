package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateGroupRequest;
import com.nppang.backend.dto.AddMemberRequest;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public CompletableFuture<ResponseEntity<UserGroup>> createGroup(@RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request.getName())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<UserGroup>>> getAllGroups() {
        return groupService.getAllGroups()
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{groupId}")
    public CompletableFuture<ResponseEntity<UserGroup>> getGroup(@PathVariable String groupId) {
        return groupService.getGroup(groupId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{groupId}/members")
    public CompletableFuture<ResponseEntity<Void>> addMember(@PathVariable String groupId, @RequestBody AddMemberRequest request) {
        return groupService.addMember(groupId, request.getUserName())
                .thenApply(ResponseEntity::ok);
    }
}
