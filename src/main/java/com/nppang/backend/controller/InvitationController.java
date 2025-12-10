package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateInvitationRequest;
import com.nppang.backend.dto.PendingInvitationDto;
import com.nppang.backend.entity.Invitation;
import com.nppang.backend.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<Invitation> inviteUser(
            @RequestBody CreateInvitationRequest request) throws ExecutionException, InterruptedException {
        Invitation invitation = invitationService.createInvitation(request).get();
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingInvitationDto>> getPendingInvitations(Authentication authentication) throws ExecutionException, InterruptedException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        String userId = (String) authentication.getPrincipal();
        List<PendingInvitationDto> invitations = invitationService.getPendingInvitations(userId).get();
        return ResponseEntity.ok(invitations);
    }

        @PostMapping("/{invitationId}/accept")

        public ResponseEntity<Void> acceptInvitation(@PathVariable String invitationId) throws ExecutionException, InterruptedException {

            invitationService.acceptInvitation(invitationId).get();

            return ResponseEntity.ok().build();

        }



        @PostMapping("/{invitationId}/reject")

        public ResponseEntity<Void> rejectInvitation(@PathVariable String invitationId) throws ExecutionException, InterruptedException {

            invitationService.rejectInvitation(invitationId).get();

            return ResponseEntity.ok().build();

        }

    }
