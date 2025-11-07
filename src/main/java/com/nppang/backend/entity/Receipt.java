package com.nppang.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // For mapping from ReceiptDto
public class Receipt {
    private String id;
    private String groupId; // Added for denormalization
    private String settlementId; // Added for denormalization
    private String storeName;
    private String transactionDate;
    private Long totalAmount;
    private Long alcoholAmount;
    private String rawText;
}
