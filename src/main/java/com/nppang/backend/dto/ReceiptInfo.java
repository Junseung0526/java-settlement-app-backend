package com.nppang.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ReceiptInfo {
    private String transactionDate;
    private Long totalAmount;
    private String storeName;
    private String rawText;
}
