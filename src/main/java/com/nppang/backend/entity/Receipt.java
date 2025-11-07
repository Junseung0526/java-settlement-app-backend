package com.nppang.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // For mapping from ReceiptDto
public class Receipt {
    private String id;
    private String groupId; // Added for denormalization
    private String settlementId; // Added for denormalization
    private String payerId; // Who paid for this receipt
    private String storeName;
    private String transactionDate;
    private Long totalAmount;
    private List<ReceiptItem> items;
}
