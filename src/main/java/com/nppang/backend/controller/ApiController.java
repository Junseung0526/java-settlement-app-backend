package com.nppang.backend.controller;

import com.nppang.backend.dto.AddMemberRequest;
import com.nppang.backend.dto.CreateGroupRequest;
import com.nppang.backend.dto.NppangGroupRequest;
import com.nppang.backend.dto.NppangRequest;
import com.nppang.backend.dto.NppangResponse;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.service.GroupService;
import com.nppang.backend.service.NppangService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final NppangService nppangService;
    private final GroupService groupService;

    @PostMapping("/nppang/calculate")
    public ResponseEntity<NppangResponse> calculateNppang(@RequestBody NppangRequest request) {
        try {
            NppangResponse response = nppangService.calculateNppang(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/groups")
    public ResponseEntity<UserGroup> createGroup(@RequestBody CreateGroupRequest request) {
        UserGroup group = groupService.createGroup(request.getName());
        return ResponseEntity.ok(group);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<UserGroup>> getAllGroups() throws ExecutionException, InterruptedException {
        List<UserGroup> groups = groupService.getAllGroups().get();
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<Void> addMemberToGroup(@PathVariable String groupId, @RequestBody AddMemberRequest request) {
        groupService.addMember(groupId, request.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<UserGroup> getGroupMembers(@PathVariable String groupId) throws ExecutionException, InterruptedException {
        UserGroup group = groupService.getGroup(groupId).get();
        return ResponseEntity.ok(group);
    }

    @PostMapping("/groups/{groupId}/calculate")
    public ResponseEntity<NppangResponse> calculateNppangForGroup(@PathVariable String groupId, @RequestBody NppangGroupRequest request) throws ExecutionException, InterruptedException {
        try {
            NppangResponse response = nppangService.calculateNppangForGroup(groupId, request).get();
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
