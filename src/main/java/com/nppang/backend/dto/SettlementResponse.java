package com.nppang.backend.dto;

import com.nppang.backend.entity.Settlement;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class SettlementResponse {
    private String settlementId;
    private String settlementName;
    private Long totalAmount;
    private Long alcoholAmount;
    private List<ReceiptDto> receipts;

    public static SettlementResponse from(Settlement settlement) {
        long totalAmount = settlement.getReceipts().values().stream().mapToLong(r -> r.getTotalAmount() != null ? r.getTotalAmount() : 0).sum();
        long alcoholAmount = settlement.getReceipts().values().stream().mapToLong(r -> r.getAlcoholAmount() != null ? r.getAlcoholAmount() : 0).sum();

        return SettlementResponse.builder()
                .settlementId(settlement.getId())
                .settlementName(settlement.getName())
                .totalAmount(totalAmount)
                .alcoholAmount(alcoholAmount)
                .receipts(settlement.getReceipts().values().stream().map(ReceiptDto::from).collect(Collectors.toList()))
                .build();
    }
}
