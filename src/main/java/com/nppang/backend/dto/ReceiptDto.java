package com.nppang.backend.dto;

import com.nppang.backend.entity.Receipt;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ReceiptDto {
    private String transactionDate;
    private Long totalAmount;
    private Long alcoholAmount;
    private String storeName;
    private String rawText;

    public static ReceiptDto from(Receipt entity) {
        return ReceiptDto.builder()
                .storeName(entity.getStoreName())
                .transactionDate(entity.getTransactionDate())
                .totalAmount(entity.getTotalAmount())
                .alcoholAmount(entity.getAlcoholAmount())
                .rawText(entity.getRawText())
                .build();
    }
}
