package com.nppang.backend.service;

import com.nppang.backend.dto.NppangRequest;
import com.nppang.backend.dto.NppangResponse;
import com.nppang.backend.dto.NppangGroupRequest;

import com.nppang.backend.entity.Settlement;
import com.nppang.backend.entity.Receipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NppangService {

    private final GroupService groupService;
    private final SettlementService settlementService;

    @Autowired
    public NppangService(GroupService groupService, SettlementService settlementService) {
        this.groupService = groupService;
        this.settlementService = settlementService;
    }

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

    public CompletableFuture<NppangResponse> calculateNppangForSettlement(Settlement settlement, int alcoholDrinkers) {
        if (settlement.getGroupId() == null) {
            throw new IllegalStateException("Settlement is not associated with a group.");
        }

        CompletableFuture<List<Receipt>> receiptsFuture = settlementService.getReceiptsForSettlement(settlement.getId());
        CompletableFuture<com.nppang.backend.entity.UserGroup> groupFuture = groupService.getGroup(settlement.getGroupId());

        return receiptsFuture.thenCombine(groupFuture, (receipts, group) -> {
            long totalAmount = receipts.stream()
                                .mapToLong(r -> r.getTotalAmount() != null ? r.getTotalAmount() : 0)
                                .sum();

            long alcoholAmount = receipts.stream()
                                 .mapToLong(r -> r.getAlcoholAmount() != null ? r.getAlcoholAmount() : 0)
                                 .sum();

            int totalPeople = (group.getMembers() != null) ? group.getMembers().size() : 0;

            NppangRequest request = new NppangRequest();
            request.setTotalAmount(totalAmount);
            request.setAlcoholAmount(alcoholAmount);
            request.setTotalPeople(totalPeople);
            request.setAlcoholDrinkers(alcoholDrinkers);

            return calculateNppang(request);
        });
    }

    public CompletableFuture<NppangResponse> calculateNppangForGroup(String groupId, NppangGroupRequest request) {
        return groupService.getGroup(groupId).thenApply(group -> {
            // Null 체크 추가: group.getMembers()가 null일 경우 빈 Map으로 처리
            int totalPeople = (group.getMembers() != null) ? group.getMembers().size() : 0;

            NppangRequest nppangRequest = new NppangRequest();
            nppangRequest.setTotalAmount(request.getTotalAmount());
            nppangRequest.setAlcoholAmount(request.getAlcoholAmount());
            nppangRequest.setTotalPeople(totalPeople);
            nppangRequest.setAlcoholDrinkers(request.getAlcoholDrinkers());

            return calculateNppang(nppangRequest);
        });
    }
}
