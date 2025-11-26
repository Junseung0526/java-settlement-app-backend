package com.nppang.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementTransaction {
    private String id;
    private String settlementId;
    private String fromUserId;
    private String toUserId;
    private Double amount;
    private boolean isPaid;
}
