package com.nppang.backend.service;

import com.google.firebase.database.*;
import com.nppang.backend.dto.CreateInvitationRequest;
import com.nppang.backend.dto.PendingInvitationDto;
import com.nppang.backend.entity.AppUser;
import com.nppang.backend.entity.Invitation;
import com.nppang.backend.entity.InvitationStatus;
import com.nppang.backend.entity.UserGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final FirebaseDatabase firebaseDatabase;
    private final GroupService groupService;
    private final UserService userService;

    public CompletableFuture<Invitation> createInvitation(CreateInvitationRequest request) {
        CompletableFuture<Invitation> future = new CompletableFuture<>();

        userService.findUserByNickname(request.getInviteeNickname()).thenAccept(invitee -> {
            if (invitee == null) {
                future.completeExceptionally(new RuntimeException("User with nickname '" + request.getInviteeNickname() + "' not found."));
                return;
            }

            String inviteeId = invitee.getId();
            Invitation invitation = new Invitation(request.getGroupId(), request.getInviterId(), inviteeId);
            invitation.setId(UUID.randomUUID().toString());

            DatabaseReference invitationsRef = firebaseDatabase.getReference("invitations");
            invitationsRef.child(invitation.getId()).setValue(invitation, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    future.completeExceptionally(databaseError.toException());
                } else {
                    future.complete(invitation);
                }
            });
        }).exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }


    public CompletableFuture<List<PendingInvitationDto>> getPendingInvitations(String userId) {
        return getRawPendingInvitations(userId).thenCompose(invitations -> {
            if (invitations.isEmpty()) {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }

            List<CompletableFuture<PendingInvitationDto>> dtoFutures = invitations.stream().map(invitation -> {
                CompletableFuture<UserGroup> groupFuture = groupService.getGroup(invitation.getGroupId());
                CompletableFuture<AppUser> inviterFuture = userService.findUserById(invitation.getInviterId());

                return CompletableFuture.allOf(groupFuture, inviterFuture).thenApply(v -> {
                    UserGroup group = groupFuture.join();
                    AppUser inviter = inviterFuture.join();
                    return new PendingInvitationDto(
                            invitation.getId(),
                            invitation.getGroupId(),
                            group != null ? group.getName() : "Unknown Group",
                            invitation.getInviterId(),
                            inviter != null ? inviter.getNickname() : "Unknown User",
                            invitation.getCreatedAt()
                    );
                });
            }).collect(Collectors.toList());

            return CompletableFuture.allOf(dtoFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> dtoFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
        });
    }

    private CompletableFuture<List<Invitation>> getRawPendingInvitations(String userId) {
        DatabaseReference invitationsRef = firebaseDatabase.getReference("invitations");
        CompletableFuture<List<Invitation>> future = new CompletableFuture<>();

        invitationsRef.orderByChild("inviteeId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Invitation> pendingInvitations = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Invitation invitation = snapshot.getValue(Invitation.class);
                    if (invitation != null && invitation.getStatus() == InvitationStatus.PENDING) {
                        // Manually set ID as it might be missed by getValue
                        invitation.setId(snapshot.getKey());
                        pendingInvitations.add(invitation);
                    }
                }
                future.complete(pendingInvitations);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }
// ... other methods, but replace the old getPendingInvitations


    public CompletableFuture<Invitation> getInvitationById(String invitationId) {
        DatabaseReference invitationRef = firebaseDatabase.getReference("invitations").child(invitationId);
        CompletableFuture<Invitation> future = new CompletableFuture<>();
        invitationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                future.complete(dataSnapshot.getValue(Invitation.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<Void> acceptInvitation(String invitationId) {
        return getInvitationById(invitationId).thenCompose(invitation -> {
            if (invitation == null || invitation.getStatus() != InvitationStatus.PENDING) {
                throw new IllegalStateException("Invitation not found or not pending.");
            }

            DatabaseReference invitationRef = firebaseDatabase.getReference("invitations").child(invitationId);
            invitation.setStatus(InvitationStatus.ACCEPTED);

            return groupService.addMemberById(invitation.getGroupId(), invitation.getInviteeId())
                    .thenRun(() -> invitationRef.child("status").setValueAsync(InvitationStatus.ACCEPTED));
        });
    }

    public CompletableFuture<Void> rejectInvitation(String invitationId) {
        return getInvitationById(invitationId).thenCompose(invitation -> {
            if (invitation == null || invitation.getStatus() != InvitationStatus.PENDING) {
                throw new IllegalStateException("Invitation not found or not pending.");
            }

            DatabaseReference invitationRef = firebaseDatabase.getReference("invitations").child(invitationId);
            return CompletableFuture.runAsync(() -> invitationRef.child("status").setValueAsync(InvitationStatus.REJECTED));
        });
    }
}
