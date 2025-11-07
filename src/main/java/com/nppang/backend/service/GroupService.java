package com.nppang.backend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nppang.backend.entity.UserGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final FirebaseDatabase firebaseDatabase;

    public UserGroup createGroup(String name) {
        DatabaseReference groupsRef = firebaseDatabase.getReference("groups");
        DatabaseReference newGroupRef = groupsRef.push();
        String groupId = newGroupRef.getKey();

        UserGroup userGroup = new UserGroup();
        userGroup.setId(groupId);
        userGroup.setName(name);

        newGroupRef.setValueAsync(userGroup);
        return userGroup;
    }

    public void addMember(String groupId, String userId) {
        DatabaseReference membersRef = firebaseDatabase.getReference("groups").child(groupId).child("members");
        membersRef.child(userId).setValueAsync(true);
    }

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
}
