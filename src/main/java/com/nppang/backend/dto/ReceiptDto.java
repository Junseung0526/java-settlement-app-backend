package com.nppang.backend.dto;

import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.ReceiptItem;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class ReceiptDto {
    private String payerId;
    private String storeName;
    private String transactionDate;
    private Long totalAmount;
    private List<ReceiptItem> items;


    public static ReceiptDto from(Receipt entity) {
        return ReceiptDto.builder()
                .payerId(entity.getPayerId())
                .storeName(entity.getStoreName())
                .transactionDate(entity.getTransactionDate())
                .totalAmount(entity.getTotalAmount())
                .items(entity.getItems())
                .build();
    }
}
