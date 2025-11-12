package com.nppang.backend.dto;

import com.nppang.backend.entity.ReceiptItem;
import lombok.Data;
import java.util.List;

@Data
public class CreateReceiptRequest {
    private String groupId;
    private String payerId;
    private String storeName;
    private String transactionDate;
    private Long totalAmount;
    private List<ReceiptItem> items;
}
