package com.nppang.backend.controller;

import com.nppang.backend.dto.*;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.service.GroupService;
import com.nppang.backend.service.NppangService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nppang.backend.entity.Receipt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            logger.warn("Invalid nppang calculation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/groups")
    public CompletableFuture<ResponseEntity<UserGroup>> createGroup(@RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request.getName())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to create group: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body(null);
                });
    }

    @GetMapping("/groups")
    public CompletableFuture<ResponseEntity<List<UserGroup>>> getAllGroups() {
        return groupService.getAllGroups()
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to fetch all groups: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body(null);
                });
    }

    @PostMapping("/groups/{groupId}/members")
    public CompletableFuture<ResponseEntity<Object>> addMemberToGroup(@PathVariable String groupId, @RequestBody AddMemberRequest request) {
        return groupService.addMember(groupId, request.getUserName())
                .thenApply(aVoid -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    logger.error("Failed to add member to group {}: {}", groupId, ex.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/groups/{groupId}")
    public CompletableFuture<ResponseEntity<?>> getGroupMembers(@PathVariable String groupId) {
        return groupService.getGroup(groupId)
                .thenApply(group -> {
                    if (group == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(group);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to get group {}: {}", groupId, ex.getMessage());
                    return ResponseEntity.internalServerError().body(null);
                });
    }

    @GetMapping("/groups/{groupId}/receipts")
    public CompletableFuture<ResponseEntity<List<Receipt>>> getReceiptsForGroup(@PathVariable String groupId) {
        return groupService.getReceiptsByGroupId(groupId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to get receipts for group {}: {}", groupId, ex.getMessage());
                    return ResponseEntity.internalServerError().body(null);
                });
    }

    @PostMapping("/groups/{groupId}/calculate")
    public CompletableFuture<ResponseEntity<NppangResponse>> calculateNppangForGroup(@PathVariable String groupId, @RequestBody NppangGroupRequest request) {
        return nppangService.calculateNppangForGroup(groupId, request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Nppang calculation failed for group {}: {}", groupId, ex.getMessage());
                    return ResponseEntity.badRequest().body(null);
                });
    }
}
