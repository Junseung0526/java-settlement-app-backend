package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CalculationResultDto {
    private Map<String, Double> userBalances;
    private List<TransactionDto> transactions;
}
