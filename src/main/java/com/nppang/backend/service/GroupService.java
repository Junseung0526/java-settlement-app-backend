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

import com.nppang.backend.entity.AppUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    // 그룹 정보 업데이트
    public CompletableFuture<Void> updateGroup(String groupId, String name) {
        DatabaseReference groupRef = firebaseDatabase.getReference("groups").child(groupId).child("name");
        return CompletableFuture.runAsync(() -> groupRef.setValueAsync(name));
    }

    // 그룹 삭제
    public CompletableFuture<Void> deleteGroup(String groupId) {
        DatabaseReference groupRef = firebaseDatabase.getReference("groups").child(groupId);
        return CompletableFuture.runAsync(() -> groupRef.removeValueAsync());
    }

    // 특정 그룹에 사용자를 멤버로 추가
    public CompletableFuture<Void> addMember(String groupId, String userName) {
        return userService.findOrCreateUser(userName).thenAccept(appUser -> {
            DatabaseReference membersRef = firebaseDatabase.getReference("groups").child(groupId).child("members");
            membersRef.child(appUser.getId()).setValueAsync(true);
        });
    }

    // 특정 그룹에서 멤버를 삭제
    public CompletableFuture<Void> deleteMember(String groupId, String userId) {
        DatabaseReference memberRef = firebaseDatabase.getReference("groups").child(groupId).child("members").child(userId);
        return CompletableFuture.runAsync(() -> memberRef.removeValueAsync());
    }

    // 특정 그룹의 멤버 목록을 조회
    public CompletableFuture<List<AppUser>> getGroupMembers(String groupId) {
        return getGroup(groupId).thenCompose(userGroup -> {
            if (userGroup == null || userGroup.getMembers() == null) {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }
            List<CompletableFuture<AppUser>> userFutures = userGroup.getMembers().keySet().stream()
                    .map(userService::findUserById)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(userFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> userFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
        });
    }

    // 특정 그룹의 정보를 조회
    public CompletableFuture<UserGroup> getGroup(String groupId) {
        DatabaseReference groupRef = firebaseDatabase.getReference("groups").child(groupId);
        CompletableFuture<UserGroup> future = new CompletableFuture<>();
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    future.complete(null);
                    return;
                }
                try {
                    UserGroup group = new UserGroup();
                    group.setId(dataSnapshot.getKey());
                    if (dataSnapshot.hasChild("name")) {
                        group.setName(dataSnapshot.child("name").getValue(String.class));
                    }

                    if (dataSnapshot.hasChild("members")) {
                        DataSnapshot membersSnapshot = dataSnapshot.child("members");
                        Map<String, Boolean> members = new HashMap<>();
                        for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                            if (memberSnapshot.getValue(Boolean.class) != null) {
                                members.put(memberSnapshot.getKey(), memberSnapshot.getValue(Boolean.class));
                            }
                        }
                        group.setMembers(members);
                    }
                    future.complete(group);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
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
        System.out.println("Attempting to fetch all groups from Firebase...");
        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserGroup> groups = new ArrayList<>();
                System.out.println("Successfully received data snapshot. Processing " + dataSnapshot.getChildrenCount() + " children.");
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        System.out.println("Processing child with key: " + snapshot.getKey());
                        UserGroup group = new UserGroup();
                        group.setId(snapshot.getKey());
                        if (snapshot.hasChild("name")) {
                            group.setName(snapshot.child("name").getValue(String.class));
                        }

                        if (snapshot.hasChild("members")) {
                            DataSnapshot membersSnapshot = snapshot.child("members");
                            Map<String, Boolean> members = new HashMap<>();
                            for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                                if (memberSnapshot.getValue(Boolean.class) != null) {
                                    members.put(memberSnapshot.getKey(), memberSnapshot.getValue(Boolean.class));
                                }
                            }
                            group.setMembers(members);
                        }
                        groups.add(group);
                    } catch (Exception e) {
                        System.err.println("!!! FAILED to deserialize snapshot with key: " + snapshot.getKey() + " !!!");
                        e.printStackTrace();
                        // Do not add to the list, just log the error and continue
                    }
                }
                System.out.println("Finished processing. Completing future with " + groups.size() + " groups.");
                future.complete(groups);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Firebase data read was cancelled: " + databaseError.getMessage());
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

    // 특정 유저가 속한 모든 그룹 목록을 조회
    public CompletableFuture<List<UserGroup>> getGroupsByUserId(String userId) {
        return getAllGroups().thenApply(allGroups ->
                allGroups.stream()
                        .filter(group -> group.getMembers() != null && group.getMembers().containsKey(userId))
                        .collect(Collectors.toList())
        );
    }
}


