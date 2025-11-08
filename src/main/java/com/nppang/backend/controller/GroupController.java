package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateGroupRequest;
import com.nppang.backend.dto.AddMemberRequest;
import com.nppang.backend.dto.UpdateGroupRequest;
import com.nppang.backend.entity.AppUser;
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

    // 새로운 그룹을 생성하는 API
    @PostMapping
    public CompletableFuture<ResponseEntity<UserGroup>> createGroup(@RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request.getName())
                .thenApply(ResponseEntity::ok);
    }

    // 모든 그룹의 목록을 조회하는 API
    @GetMapping
    public CompletableFuture<ResponseEntity<List<UserGroup>>> getAllGroups() {
        return groupService.getAllGroups()
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹의 정보를 조회하는 API
    @GetMapping("/{groupId}")
    public CompletableFuture<ResponseEntity<UserGroup>> getGroup(@PathVariable String groupId) {
        return groupService.getGroup(groupId)
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹의 정보를 수정하는 API
    @PutMapping("/{groupId}")
    public CompletableFuture<ResponseEntity<Void>> updateGroup(@PathVariable String groupId, @RequestBody UpdateGroupRequest request) {
        return groupService.updateGroup(groupId, request.getName())
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹을 삭제하는 API
    @DeleteMapping("/{groupId}")
    public CompletableFuture<ResponseEntity<Void>> deleteGroup(@PathVariable String groupId) {
        return groupService.deleteGroup(groupId)
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹에 멤버를 추가하는 API
    @PostMapping("/{groupId}/members")
    public CompletableFuture<ResponseEntity<Void>> addMember(@PathVariable String groupId, @RequestBody AddMemberRequest request) {
        return groupService.addMember(groupId, request.getUserName())
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹의 모든 멤버를 조회하는 API
    @GetMapping("/{groupId}/members")
    public CompletableFuture<ResponseEntity<List<AppUser>>> getGroupMembers(@PathVariable String groupId) {
        return groupService.getGroupMembers(groupId)
                .thenApply(ResponseEntity::ok);
    }

    // 특정 그룹의 멤버를 삭제하는 API
    @DeleteMapping("/{groupId}/members/{userId}")
    public CompletableFuture<ResponseEntity<Void>> deleteMember(@PathVariable String groupId, @PathVariable String userId) {
        return groupService.deleteMember(groupId, userId)
                .thenApply(ResponseEntity::ok);
    }
}
