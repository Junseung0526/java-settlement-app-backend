package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingInvitationDto {
    private String invitationId;
    private String groupId;
    private String groupName;
    private String inviterId;
    private String inviterNickname;
    private String createdAt;
}
