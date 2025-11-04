package com.nppang.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private final String FIREBASE_CONFIG_PATH = "serviceAccountKey.json";

    @PostConstruct
    public void initialize() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(FIREBASE_CONFIG_PATH);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .setDatabaseUrl("https://bookcycle-e8d43-default-rtdb.firebaseio.com/")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Application initialized");
            }

        } catch (IOException e) {
            System.err.println("Firebase 초기화 실패: " + e.getMessage());
            throw new IOException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
