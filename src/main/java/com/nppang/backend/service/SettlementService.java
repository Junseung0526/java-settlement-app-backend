package com.nppang.backend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nppang.backend.dto.CalculationResultDto;
import com.nppang.backend.dto.TransactionDto;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.entity.SettlementTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final FirebaseDatabase firebaseDatabase;
    private final ReceiptService receiptService;
    private final NppangService nppangService;

    // 새로운 정산을 생성
    public Settlement createSettlement(String settlementName, String groupId) {
        DatabaseReference settlementsRef = firebaseDatabase.getReference("settlements");
        DatabaseReference newSettlementRef = settlementsRef.push();
        String settlementId = newSettlementRef.getKey();

        Settlement settlement = new Settlement();
        settlement.setId(settlementId);
        settlement.setName(settlementName);
        settlement.setGroupId(groupId);

        newSettlementRef.setValueAsync(settlement);
        return settlement;
    }

    // 특정 정산 정보를 조회
    public CompletableFuture<Settlement> getSettlement(String settlementId) {
        DatabaseReference settlementRef = firebaseDatabase.getReference("settlements").child(settlementId);
        CompletableFuture<Settlement> future = new CompletableFuture<>();
        settlementRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Settlement settlement = dataSnapshot.getValue(Settlement.class);
                future.complete(settlement);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<CalculationResultDto> calculateAndFinalizeSettlement(String settlementId, List<String> receiptIds) {
        CompletableFuture<Settlement> settlementFuture = getSettlement(settlementId);
        CompletableFuture<List<Receipt>> receiptsFuture = receiptService.getReceiptsByIds(receiptIds);

        return settlementFuture.thenCombine(receiptsFuture, (settlement, receipts) -> {
            if (settlement == null) {
                throw new IllegalStateException("Settlement not found");
            }

            return nppangService.calculateSettlement(settlement, receipts)
                    .thenCompose(result -> {
                        // 2. 영수증에 정산 ID 업데이트 (기존 로직)
                        CompletableFuture<Void> updateReceiptsFuture = receiptService.updateSettlementIdForReceipts(receiptIds, settlementId);

                        // 3. [추가됨] 계산된 송금 내역(Transaction)을 DB에 저장
                        CompletableFuture<Void> saveTransactionsFuture = saveTransactions(settlementId, result.getTransactions());

                        // 두 작업(영수증 업데이트, 트랜잭션 저장)이 모두 끝나면 결과 반환
                        return CompletableFuture.allOf(updateReceiptsFuture, saveTransactionsFuture)
                                .thenApply(v -> result);
                    });
        }).thenCompose(future -> future);
    }

    private CompletableFuture<Void> saveTransactions(String settlementId, List<TransactionDto> transactionDtos) {
        DatabaseReference transactionsRef = firebaseDatabase.getReference("settlement_transactions").child(settlementId);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (TransactionDto dto : transactionDtos) {
            DatabaseReference newTransRef = transactionsRef.push(); // 고유 ID 생성

            SettlementTransaction transaction = SettlementTransaction.builder()
                    .id(newTransRef.getKey())
                    .settlementId(settlementId)
                    .fromUserId(dto.getFromUser())
                    .toUserId(dto.getToUser())
                    .amount(dto.getAmount())
                    .isPaid(false) // 초기 상태는 미입금
                    .build();

            // 비동기로 저장
            futures.add(CompletableFuture.runAsync(() -> {
                newTransRef.setValueAsync(transaction);
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // 저장된 송금 내역 조회
    public CompletableFuture<List<SettlementTransaction>> getTransactions(String settlementId) {
        DatabaseReference ref = firebaseDatabase.getReference("settlement_transactions").child(settlementId);
        CompletableFuture<List<SettlementTransaction>> future = new CompletableFuture<>();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<SettlementTransaction> list = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    list.add(snapshot.getValue(SettlementTransaction.class));
                }
                future.complete(list);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });
        return future;
    }
}
