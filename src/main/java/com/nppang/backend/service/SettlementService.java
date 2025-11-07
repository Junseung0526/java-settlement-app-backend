package com.nppang.backend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nppang.backend.dto.ReceiptDto;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final FirebaseDatabase firebaseDatabase;
    private final OcrService ocrService;

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

    public CompletableFuture<Receipt> addReceiptToSettlement(String settlementId, MultipartFile file) {
        CompletableFuture<Settlement> settlementFuture = getSettlement(settlementId);

        return settlementFuture.thenCompose(settlement -> {
            if (settlement == null) {
                CompletableFuture<Receipt> exceptionalFuture = new CompletableFuture<>();
                exceptionalFuture.completeExceptionally(new RuntimeException("Settlement not found with id: " + settlementId));
                return exceptionalFuture;
            }

            try {
                String groupId = settlement.getGroupId();
                DatabaseReference receiptsRef = firebaseDatabase.getReference("receipts");
                DatabaseReference newReceiptRef = receiptsRef.push();
                String receiptId = newReceiptRef.getKey();

                ReceiptDto parsedInfo = ocrService.doOcrAndParse(file);

                Receipt receipt = Receipt.builder()
                        .id(receiptId)
                        .groupId(groupId)
                        .settlementId(settlementId)
                        .storeName(parsedInfo.getStoreName())
                        .transactionDate(parsedInfo.getTransactionDate())
                        .totalAmount(parsedInfo.getTotalAmount())
                        .alcoholAmount(parsedInfo.getAlcoholAmount())
                        .rawText(parsedInfo.getRawText())
                        .build();

                CompletableFuture<Receipt> saveFuture = new CompletableFuture<>();
                newReceiptRef.setValue(receipt, (databaseError, databaseReference) -> {
                    if (databaseError == null) {
                        saveFuture.complete(receipt);
                    } else {
                        saveFuture.completeExceptionally(databaseError.toException());
                    }
                });
                return saveFuture;
            } catch (TesseractException | IOException e) {
                CompletableFuture<Receipt> exceptionalFuture = new CompletableFuture<>();
                exceptionalFuture.completeExceptionally(e);
                return exceptionalFuture;
            }
        });
    }

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

    public CompletableFuture<java.util.List<Receipt>> getReceiptsForSettlement(String settlementId) {
        DatabaseReference receiptsRef = firebaseDatabase.getReference("receipts");
        CompletableFuture<java.util.List<Receipt>> future = new CompletableFuture<>();
        receiptsRef.orderByChild("settlementId").equalTo(settlementId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                java.util.List<Receipt> receipts = new java.util.ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    receipts.add(childSnapshot.getValue(Receipt.class));
                }
                future.complete(receipts);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }
}
