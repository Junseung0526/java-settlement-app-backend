package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionDto {
    private String fromUser;
    private String toUser;
    private Double amount;
}
