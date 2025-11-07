package com.nppang.backend.service;

import com.nppang.backend.dto.CalculationResultDto;
import com.nppang.backend.dto.TransactionDto;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.ReceiptItem;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.entity.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class NppangService {

    private final GroupService groupService;
    private final SettlementService settlementService;

    @Autowired
    public NppangService(GroupService groupService, SettlementService settlementService) {
        this.groupService = groupService;
        this.settlementService = settlementService;
    }

    // 정산의 최종 결과를 계산
    public CompletableFuture<CalculationResultDto> calculateSettlement(Settlement settlement) {
        if (settlement.getGroupId() == null) {
            throw new IllegalStateException("Settlement is not associated with a group.");
        }

        CompletableFuture<UserGroup> groupFuture = groupService.getGroup(settlement.getGroupId());
        CompletableFuture<List<Receipt>> receiptsFuture = settlementService.getReceiptsForSettlement(settlement.getId());

        return groupFuture.thenCombine(receiptsFuture, (group, receipts) -> {
            Map<String, Double> balances = new HashMap<>();
            if (group.getMembers() != null) {
                for (String memberId : group.getMembers().keySet()) {
                    balances.put(memberId, 0.0);
                }
            }

            for (Receipt receipt : receipts) {
                String payerId = receipt.getPayerId();
                if (payerId == null || !balances.containsKey(payerId)) {
                    continue;
                }

                balances.put(payerId, balances.get(payerId) + receipt.getTotalAmount());

                if (receipt.getItems() != null) {
                    for (ReceiptItem item : receipt.getItems()) {
                        double price = item.getPrice();
                        List<String> participants = item.getParticipants();
                        if (participants == null || participants.isEmpty()) {
                            continue;
                        }

                        double costPerParticipant = price / participants.size();
                        for (String participantId : participants) {
                            if (balances.containsKey(participantId)) {
                                balances.put(participantId, balances.get(participantId) - costPerParticipant);
                            }
                        }
                    }
                }
            }

            List<TransactionDto> transactions = simplifyTransactions(balances);

            return new CalculationResultDto(balances, transactions);
        });
    }

    // 정산 결과를 바탕으로 최종 송금 목록을 계산
    private List<TransactionDto> simplifyTransactions(Map<String, Double> balances) {
        Map<String, Double> debtors = balances.entrySet().stream()
                .filter(entry -> entry.getValue() < -0.01)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Double> creditors = balances.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.01)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<TransactionDto> transactions = new ArrayList<>();

        var debtorIter = debtors.entrySet().iterator();
        var creditorIter = creditors.entrySet().iterator();

        Map.Entry<String, Double> debtorEntry = debtorIter.hasNext() ? debtorIter.next() : null;
        Map.Entry<String, Double> creditorEntry = creditorIter.hasNext() ? creditorIter.next() : null;

        while (debtorEntry != null && creditorEntry != null) {
            String debtorId = debtorEntry.getKey();
            double debtorAmount = debtorEntry.getValue();
            String creditorId = creditorEntry.getKey();
            double creditorAmount = creditorEntry.getValue();

            double transferAmount = Math.min(Math.abs(debtorAmount), creditorAmount);

            transactions.add(new TransactionDto(debtorId, creditorId, transferAmount));

            debtorEntry.setValue(debtorAmount + transferAmount);
            creditorEntry.setValue(creditorAmount - transferAmount);

            if (Math.abs(debtorEntry.getValue()) < 0.01) {
                debtorEntry = debtorIter.hasNext() ? debtorIter.next() : null;
            }

            if (creditorEntry.getValue() < 0.01) {
                creditorEntry = creditorIter.hasNext() ? creditorIter.next() : null;
            }
        }

        return transactions;
    }
}
