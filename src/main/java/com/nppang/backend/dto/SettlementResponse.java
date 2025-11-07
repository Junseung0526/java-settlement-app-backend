package com.nppang.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SettlementResponse {
    private String settlementId;
    private String settlementName;
    private String groupId;
    private Long totalAmount;
    private List<ReceiptDto> receipts;
}
