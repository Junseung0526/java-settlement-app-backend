package com.nppang.backend.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseTestService {

    public String saveTestData(String path, String message) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("tests").child(path);

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", new java.util.Date().toString());
        data.put("message", message);

        ref.setValueAsync(data);

        return "Data saved to Realtime Database successfully at path: tests/" + path;
    }
}
