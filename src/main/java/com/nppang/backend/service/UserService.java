package com.nppang.backend.service;

import com.google.firebase.database.*;
import com.nppang.backend.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserService {

    private final FirebaseDatabase firebaseDatabase;
    private static final String METADATA_PATH = "metadata";
    private static final String LAST_USER_ID_KEY = "lastUserId";
    private static final String USERS_PATH = "users";

    // 원자적 연산을 통해 마지막 사용자 ID를 1 증가시키고 가져옵니다.
    private CompletableFuture<Long> getLastUserIdAndIncrement() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        DatabaseReference lastUserIdRef = firebaseDatabase.getReference(METADATA_PATH).child(LAST_USER_ID_KEY);

        lastUserIdRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Long currentId = mutableData.getValue(Long.class);
                if (currentId == null) {
                    currentId = 0L;
                }
                long nextId = currentId + 1;
                mutableData.setValue(nextId);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    future.completeExceptionally(databaseError.toException());
                } else if (committed) {
                    future.complete(dataSnapshot.getValue(Long.class));
                } else {

                }
            }
        });

        return future;
    }

    // 사용자 이름으로 사용자를 찾거나, 없으면 새로 생성합니다.
    public CompletableFuture<AppUser> findOrCreateUser(String username) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);

        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        future.complete(snapshot.getValue(AppUser.class));
                        return;
                    }
                } else {
                    getLastUserIdAndIncrement().thenAccept(newId -> {
                        String userId = String.valueOf(newId);
                        AppUser newUser = new AppUser();
                        newUser.setId(userId);
                        newUser.setUsername(username);
                        newUser.setPassword(null);

                        usersRef.child(userId).setValue(newUser, (databaseError, databaseReference) -> {
                            if (databaseError != null) {
                                future.completeExceptionally(databaseError.toException());
                            } else {
                                future.complete(newUser);
                            }
                        });
                    }).exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    // 사용자 ID로 사용자를 찾습니다.
    public CompletableFuture<AppUser> findUserById(String userId) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH).child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    future.complete(dataSnapshot.getValue(AppUser.class));
                } else {
                    future.complete(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }
}
