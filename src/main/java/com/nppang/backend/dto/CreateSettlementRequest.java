package com.nppang.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSettlementRequest {
    private String settlementName;
    private String groupId;
}
