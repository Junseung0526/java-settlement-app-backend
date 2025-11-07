package com.nppang.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NppangResponse {
    private Long amountPerPerson; // 술 안먹는 사람
    private Long amountForAlcoholDrinker; // 술 먹는 사람
}
