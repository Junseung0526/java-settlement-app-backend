package com.nppang.backend.service;

import com.google.firebase.database.*;
import com.nppang.backend.dto.CreateReceiptRequest;
import com.nppang.backend.entity.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final FirebaseDatabase firebaseDatabase;
    private static final String RECEIPTS_PATH = "receipts";

    public CompletableFuture<Receipt> createReceipt(CreateReceiptRequest request) {
        DatabaseReference receiptsRef = firebaseDatabase.getReference(RECEIPTS_PATH);
        DatabaseReference newReceiptRef = receiptsRef.push();
        String receiptId = newReceiptRef.getKey();

        Receipt receipt = Receipt.builder()
                .id(receiptId)
                .groupId(request.getGroupId())
                .settlementId(null) // Initially, settlementId is null
                .payerId(request.getPayerId())
                .storeName(request.getStoreName())
                .transactionDate(request.getTransactionDate())
                .totalAmount(request.getTotalAmount())
                .items(request.getItems())
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
    }

    public CompletableFuture<Receipt> getReceiptById(String receiptId) {
        DatabaseReference receiptRef = firebaseDatabase.getReference(RECEIPTS_PATH).child(receiptId);
        CompletableFuture<Receipt> future = new CompletableFuture<>();
        receiptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Receipt receipt = dataSnapshot.getValue(Receipt.class);
                future.complete(receipt);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<List<Receipt>> getReceiptsByIds(List<String> receiptIds) {
        List<CompletableFuture<Receipt>> futures = receiptIds.stream()
                .map(this::getReceiptById)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<Void> updateSettlementIdForReceipts(List<String> receiptIds, String settlementId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference receiptsRef = firebaseDatabase.getReference(RECEIPTS_PATH);
        Map<String, Object> updates = new HashMap<>();
        for (String receiptId : receiptIds) {
            updates.put(receiptId + "/settlementId", settlementId);
        }

        receiptsRef.updateChildren(updates, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                future.completeExceptionally(databaseError.toException());
            } else {
                future.complete(null);
            }
        });
        return future;
    }


    public CompletableFuture<List<Receipt>> getReceiptsBySettlementId(String settlementId) {
        DatabaseReference receiptsRef = firebaseDatabase.getReference(RECEIPTS_PATH);
        CompletableFuture<List<Receipt>> future = new CompletableFuture<>();
        receiptsRef.orderByChild("settlementId").equalTo(settlementId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Receipt> receipts = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        receipts.add(childSnapshot.getValue(Receipt.class));
                    }
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

    public CompletableFuture<List<Receipt>> getReceiptsByGroupId(String groupId) {
        DatabaseReference receiptsRef = firebaseDatabase.getReference(RECEIPTS_PATH);
        CompletableFuture<List<Receipt>> future = new CompletableFuture<>();
        receiptsRef.orderByChild("groupId").equalTo(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Receipt> receipts = new ArrayList<>();
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
