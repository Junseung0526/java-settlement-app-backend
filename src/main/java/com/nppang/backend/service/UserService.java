package com.nppang.backend.service;

import com.google.firebase.database.*;
import com.nppang.backend.entity.AppUser;
// import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.nppang.backend.dto.SignUpRequest;
import org.springframework.beans.factory.annotation.Autowired; // @Autowired 추가

@Service
// @RequiredArgsConstructor 대신 생성자 주입 사용
public class UserService {

    private final FirebaseDatabase firebaseDatabase;
    private final PasswordEncoder passwordEncoder;
    private static final String METADATA_PATH = "metadata";
    private static final String LAST_USER_ID_KEY = "lastUserId";
    private static final String USERS_PATH = "users";

    @Autowired // PasswordEncoder 주입을 위해 생성자 추가
    public UserService(FirebaseDatabase firebaseDatabase, PasswordEncoder passwordEncoder) {
        this.firebaseDatabase = firebaseDatabase;
        this.passwordEncoder = passwordEncoder;
    }

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

    // 회원가입 메서드
    public CompletableFuture<AppUser> registerUser(SignUpRequest request) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);

        // 1. 사용자 이름 중복 확인
        usersRef.orderByChild("username").equalTo(request.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // 이미 사용 중인 이름
                    future.completeExceptionally(new RuntimeException("Username is already taken."));
                    return;
                }

                // 2. 중복이 아니면 새 사용자 생성
                getLastUserIdAndIncrement().thenAccept(newId -> {
                    String userId = String.valueOf(newId);
                    AppUser newUser = new AppUser();
                    newUser.setId(userId);
                    newUser.setUsername(request.getUsername());
                    newUser.setNickname(request.getNickname());
                    // [중요] 비밀번호 암호화
                    newUser.setPassword(passwordEncoder.encode(request.getPassword()));

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

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    // 로그인 시 사용자 이름으로 조회
    public CompletableFuture<AppUser> findUserByUsername(String username) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);

        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Firebase 쿼리 결과는 여러 개일 수 있으므로 첫 번째 일치 항목을 사용
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            AppUser user = snapshot.getValue(AppUser.class);
                            if (user != null) {
                                user.setId(snapshot.getKey()); // [중요] 키(ID) 설정
                                future.complete(user);
                                return; // 사용자를 찾았으므로 반환
                            }
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                            return;
                        }
                    }
                }
                future.complete(null); // 사용자가 없음
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    // 사용자 이름으로 사용자를 찾거나, 없으면 새로 생성
    public CompletableFuture<AppUser> findOrCreateUser(String username) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);

        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            AppUser user = new AppUser();
                            user.setId(snapshot.getKey());
                            if (snapshot.hasChild("username")) {
                                user.setUsername(snapshot.child("username").getValue(String.class));
                            }
                            if (snapshot.hasChild("nickname")) {
                                user.setNickname(snapshot.child("nickname").getValue(String.class));
                            }
                            if (snapshot.hasChild("password")) {
                                user.setPassword(snapshot.child("password").getValue(String.class));
                            }
                            future.complete(user);
                            return; // Found the user, exit
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                            return; // Error, exit
                        }
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

    // 사용자 ID로 사용자를 찾음
    public CompletableFuture<AppUser> findUserById(String userId) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH).child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        AppUser user = new AppUser();
                        user.setId(dataSnapshot.getKey());
                        if (dataSnapshot.hasChild("username")) {
                            user.setUsername(dataSnapshot.child("username").getValue(String.class));
                        }
                        if (dataSnapshot.hasChild("nickname")) {
                            user.setNickname(dataSnapshot.child("nickname").getValue(String.class));
                        }
                        if (dataSnapshot.hasChild("password")) {
                            user.setPassword(dataSnapshot.child("password").getValue(String.class));
                        }
                        future.complete(user);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
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

    public CompletableFuture<List<AppUser>> findAllUsers() {
        CompletableFuture<List<AppUser>> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<AppUser> users = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        AppUser user = snapshot.getValue(AppUser.class);
                        if (user != null) { // Null check for user
                            user.setId(snapshot.getKey());
                            // Explicitly check and set nickname if snapshot.getValue misses it for some reason
                            if (snapshot.hasChild("nickname") && user.getNickname() == null) {
                                user.setNickname(snapshot.child("nickname").getValue(String.class));
                            }
                            users.add(user);
                        }
                    }
                }
                future.complete(users);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<AppUser> findUserByNickname(String nickname) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);

        usersRef.orderByChild("nickname").equalTo(nickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            AppUser user = snapshot.getValue(AppUser.class);
                            if (user != null) {
                                user.setId(snapshot.getKey());
                                future.complete(user);
                                return;
                            }
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                            return;
                        }
                    }
                }
                future.complete(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<Void> updateUser(String userId, String newUsername, String newNickname) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH).child(userId);
        Map<String, Object> updates = new HashMap<>();
        if (newUsername != null && !newUsername.isEmpty()) {
            updates.put("username", newUsername);
        }
        if (newNickname != null && !newNickname.isEmpty()) {
            updates.put("nickname", newNickname);
        }

        if (updates.isEmpty()) {
            future.complete(null); // Nothing to update
            return future;
        }

        userRef.updateChildren(updates, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                future.completeExceptionally(databaseError.toException());
            } else {
                future.complete(null);
            }
        });
        return future;
    }
}
