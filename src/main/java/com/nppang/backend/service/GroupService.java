package com.nppang.backend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.nppang.backend.entity.UserGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nppang.backend.entity.Receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final FirebaseDatabase firebaseDatabase;
    private final UserService userService;

    // 새로운 그룹을 생성하고 ID를 부여
    public CompletableFuture<UserGroup> createGroup(String name) {
        DatabaseReference counterRef = firebaseDatabase.getReference("counters/groups");
        CompletableFuture<UserGroup> future = new CompletableFuture<>();

        counterRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Long currentValue = mutableData.getValue(Long.class);
                if (currentValue == null) {
                    currentValue = 1L;
                } else {
                    currentValue++;
                }
                mutableData.setValue(currentValue);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed) {
                    Long newId = dataSnapshot.getValue(Long.class);
                    DatabaseReference newGroupRef = firebaseDatabase.getReference("groups").child(String.valueOf(newId));

                    UserGroup userGroup = new UserGroup();
                    userGroup.setId(String.valueOf(newId));
                    userGroup.setName(name);

                    newGroupRef.setValueAsync(userGroup);
                    future.complete(userGroup);
                } else {
                    future.completeExceptionally(databaseError.toException());
                }
            }
        });
        return future;
    }

    // 특정 그룹에 사용자를 멤버로 추가
    public CompletableFuture<Void> addMember(String groupId, String userName) {
        return userService.findOrCreateUser(userName).thenAccept(appUser -> {
            DatabaseReference membersRef = firebaseDatabase.getReference("groups").child(groupId).child("members");
            membersRef.child(appUser.getId()).setValueAsync(true);
        });
    }

    // 특정 그룹의 정보를 조회
    public CompletableFuture<UserGroup> getGroup(String groupId) {
        DatabaseReference groupRef = firebaseDatabase.getReference("groups").child(groupId);
        CompletableFuture<UserGroup> future = new CompletableFuture<>();
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserGroup group = dataSnapshot.getValue(UserGroup.class);
                future.complete(group);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    // 모든 그룹의 목록을 조회
    public CompletableFuture<List<UserGroup>> getAllGroups() {
        DatabaseReference groupsRef = firebaseDatabase.getReference("groups");
        CompletableFuture<List<UserGroup>> future = new CompletableFuture<>();
        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserGroup> groups = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    groups.add(snapshot.getValue(UserGroup.class));
                }
                future.complete(groups);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    // 특정 그룹 ID에 속한 모든 영수증 목록을 조회
    public CompletableFuture<List<Receipt>> getReceiptsByGroupId(String groupId) {
        DatabaseReference receiptsRef = firebaseDatabase.getReference("receipts");
        CompletableFuture<List<Receipt>> future = new CompletableFuture<>();
        receiptsRef.orderByChild("groupId").equalTo(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Receipt> receipts = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    receipts.add(snapshot.getValue(Receipt.class));
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
