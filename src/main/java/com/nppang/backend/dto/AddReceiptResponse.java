package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddReceiptResponse {
    private String settlementId;
    private String receiptId;
    private ReceiptDto receipt;
}
