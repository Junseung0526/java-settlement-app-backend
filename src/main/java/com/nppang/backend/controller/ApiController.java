package com.nppang.backend.controller;

import com.nppang.backend.dto.NppangRequest;
import com.nppang.backend.dto.NppangResponse;

import com.nppang.backend.service.NppangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nppang.backend.dto.AddMemberRequest;
import com.nppang.backend.dto.CreateGroupRequest;
import com.nppang.backend.dto.NppangGroupRequest;
import com.nppang.backend.entity.GroupMember;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final NppangService nppangService;
    private final GroupService groupService;

    public ApiController(NppangService nppangService, GroupService groupService) {

        this.nppangService = nppangService;
        this.groupService = groupService;
    }

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
    public ResponseEntity<List<UserGroup>> getAllGroups() {
        List<UserGroup> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<GroupMember> addMemberToGroup(@PathVariable Long groupId, @RequestBody AddMemberRequest request) {
        GroupMember groupMember = groupService.addMember(groupId, request.getUserId());
        return ResponseEntity.ok(groupMember);
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/groups/{groupId}/calculate")
    public ResponseEntity<NppangResponse> calculateNppangForGroup(@PathVariable Long groupId, @RequestBody NppangGroupRequest request) {
        try {
            NppangResponse response = nppangService.calculateNppangForGroup(groupId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
