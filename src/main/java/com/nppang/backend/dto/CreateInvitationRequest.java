package com.nppang.backend.dto;

import lombok.Data;

@Data
public class CreateInvitationRequest {
    private String groupId;
    private String inviterId;
    private String inviteeNickname;
}
