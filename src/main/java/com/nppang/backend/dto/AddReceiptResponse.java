package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddReceiptResponse {
    private Long settlementId;
    private Long receiptId;
    private ReceiptDto receipt;
}
