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

    public CompletableFuture<AppUser> findOrCreateUser(String username) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference("users");

        // Try to find user by username first
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User found, return the existing user
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        future.complete(snapshot.getValue(AppUser.class));
                        return;
                    }
                } else {
                    // User not found, create a new one
                    DatabaseReference newUserRef = usersRef.push();
                    String userId = newUserRef.getKey();
                    AppUser newUser = new AppUser();
                    newUser.setId(userId);
                    newUser.setUsername(username);
                    // Password can be set to a default or left null if not used for login
                    newUser.setPassword(null); 

                    newUserRef.setValue(newUser, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            future.completeExceptionally(databaseError.toException());
                        } else {
                            future.complete(newUser);
                        }
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

    public CompletableFuture<AppUser> findUserById(String userId) {
        CompletableFuture<AppUser> future = new CompletableFuture<>();
        DatabaseReference userRef = firebaseDatabase.getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    future.complete(dataSnapshot.getValue(AppUser.class));
                } else {
                    future.complete(null); // User not found
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
