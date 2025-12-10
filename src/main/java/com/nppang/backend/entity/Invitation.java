package com.nppang.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Invitation {

    private String id;
    private String groupId;
    private String inviterId;
    private String inviteeId;
    private InvitationStatus status;
    private String createdAt;

    public Invitation(String groupId, String inviterId, String inviteeId) {
        this.groupId = groupId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.status = InvitationStatus.PENDING;
        this.createdAt = LocalDateTime.now().toString();
    }
}
