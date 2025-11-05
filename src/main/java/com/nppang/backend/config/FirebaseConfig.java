package com.nppang.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_KEY}")
    private String firebaseServiceAccountKey;

    @PostConstruct
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = new ByteArrayInputStream(
                        firebaseServiceAccountKey.getBytes(StandardCharsets.UTF_8));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://bookcycle-e8d43-default-rtdb.firebaseio.com/")
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Application initialized via Environment Variable");

            } catch (Exception e) {
                // IOException 대신 Exception으로 변경하여 모든 예외 처리
                System.err.println("Firebase 초기화 실패: " + e.getMessage());
                throw new IOException("Failed to initialize Firebase Admin SDK", e);
            }
        }
    }
}
