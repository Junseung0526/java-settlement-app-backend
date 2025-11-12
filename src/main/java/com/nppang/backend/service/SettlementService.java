package com.nppang.backend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nppang.backend.dto.CalculationResultDto;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            // Call the calculation logic
            return nppangService.calculateSettlement(settlement, receipts)
                    .thenCompose(result ->
                            // After calculation, update the settlementId on all used receipts
                            receiptService.updateSettlementIdForReceipts(receiptIds, settlementId)
                                    .thenApply(v -> result) // Return the original calculation result
                    );
        }).thenCompose(future -> future);
    }
}
