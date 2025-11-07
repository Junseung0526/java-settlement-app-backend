package com.nppang.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_BASE64}")
    private String serviceAccountSource;

    @Value("${FIREBASE_DATABASE_URL}")
    private String databaseUrl;

    @Bean
    public FirebaseApp firebaseApp() {

        InputStream serviceAccount;

        try {
            if (serviceAccountSource.startsWith("file:")) {
                String path = serviceAccountSource.substring("file:".length());

                if (path.equals("local-placeholder")) {
                    System.err.println("FATAL ERROR: FIREBASE_SERVICE_ACCOUNT_BASE64 environment variable is MISSING.");
                    throw new RuntimeException("Firebase initialization failed: Service account key is missing in Railway environment.");
                }

                serviceAccount = new FileInputStream(path);
                System.out.println("Firebase: Initialized using local file path: " + path);
            } else {
                byte[] decodedBytes = Base64.getDecoder().decode(serviceAccountSource);
                serviceAccount = new ByteArrayInputStream(decodedBytes);
                System.out.println("Firebase: Initialized using Base64 environment variable.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (IOException e) {
            System.err.println("FATAL ERROR: Failed to initialize Firebase Admin SDK due to I/O error or invalid JSON structure.");
            e.printStackTrace();
            throw new RuntimeException("Firebase initialization failed.", e);
        } catch (IllegalArgumentException e) {
             System.err.println("FATAL ERROR: Invalid Base64 string in FIREBASE_SERVICE_ACCOUNT_BASE64.");
             e.printStackTrace();
             throw new RuntimeException("Firebase initialization failed due to invalid Base64.", e);
        }
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
