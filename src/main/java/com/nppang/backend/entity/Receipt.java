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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Receipt {
    private String id;
    private String groupId;
    private String settlementId;
    private String payerId;
    private String storeName;
    private String transactionDate;
    private Long totalAmount;
    private List<ReceiptItem> items;
}
