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
    public ResponseEntity<UserGroup> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            UserGroup group = groupService.createGroup(request.getName()).join();
            return ResponseEntity.ok(group);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 모든 그룹의 목록을 조회하는 API
    @GetMapping
    public ResponseEntity<List<UserGroup>> getAllGroups() {
        try {
            List<UserGroup> groups = groupService.getAllGroups().join();
            return ResponseEntity.ok(groups);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 그룹의 정보를 조회하는 API
    @GetMapping("/{groupId}")
    public ResponseEntity<UserGroup> getGroup(@PathVariable String groupId) {
        try {
            UserGroup group = groupService.getGroup(groupId).join();
            return ResponseEntity.ok(group);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build(); // Or more specific error handling
        }
    }

    // 특정 그룹의 정보를 수정하는 API
    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(@PathVariable String groupId, @RequestBody UpdateGroupRequest request) {
        try {
            groupService.updateGroup(groupId, request.getName()).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 그룹을 삭제하는 API
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId) {
        try {
            groupService.deleteGroup(groupId).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 그룹에 멤버를 추가하는 API
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable String groupId, @RequestBody AddMemberRequest request) {
        try {
            groupService.addMember(groupId, request.getUserName()).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 그룹의 모든 멤버를 조회하는 API
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<AppUser>> getGroupMembers(@PathVariable String groupId) {
        try {
            List<AppUser> members = groupService.getGroupMembers(groupId).join();
            return ResponseEntity.ok(members);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 그룹의 멤버를 삭제하는 API
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> deleteMember(@PathVariable String groupId, @PathVariable String userId) {
        try {
            groupService.deleteMember(groupId, userId).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    // 특정 유저가 속한 모든 그룹 목록을 조회하는 API
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserGroup>> getGroupsByUserId(@PathVariable String userId) {
        try {
            List<UserGroup> groups = groupService.getGroupsByUserId(userId).join();
            return ResponseEntity.ok(groups);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }
}