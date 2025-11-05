package com.nppang.backend.service;

import com.nppang.backend.dto.NppangRequest;
import com.nppang.backend.dto.NppangResponse;
import org.springframework.stereotype.Service;

@Service
public class NppangService {

    public NppangResponse calculateNppang(NppangRequest request) {
        Long totalAmount = request.getTotalAmount();
        Long alcoholAmount = request.getAlcoholAmount();
        int totalPeople = request.getTotalPeople();
        int alcoholDrinkers = request.getAlcoholDrinkers();

        if (totalPeople <= 0) {
            throw new IllegalArgumentException("Total people must be greater than 0.");
        }

        if (alcoholDrinkers > totalPeople) {
            throw new IllegalArgumentException("Alcohol drinkers cannot be more than total people.");
        }

        // 술을 제외한 공통 금액
        long commonAmount = totalAmount - alcoholAmount;

        // 1인당 공통 부담 금액
        long commonAmountPerPerson = commonAmount / totalPeople;

        // 술 마시는 사람들의 추가 부담 금액
        long alcoholAmountPerDrinker = 0;
        if (alcoholDrinkers > 0) {
            alcoholAmountPerDrinker = alcoholAmount / alcoholDrinkers;
        }

        // 최종 금액 계산
        long amountPerPerson = commonAmountPerPerson;
        long amountForAlcoholDrinker = commonAmountPerPerson + alcoholAmountPerDrinker;

        return new NppangResponse(amountPerPerson, amountForAlcoholDrinker);
    }
}
