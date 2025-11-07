package com.nppang.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // Railway Variables에 설정할 환경 변수 이름 정의
    private static final String CREDENTIALS_VAR = "GOOGLE_APPLICATION_CREDENTIALS_JSON";
    private static final String DATABASE_URL_VAR = "FIREBASE_DATABASE_URL";

    @Bean
    public FirebaseApp firebaseApp() {
        String rawJson = System.getenv(CREDENTIALS_VAR);
        String databaseUrl = System.getenv(DATABASE_URL_VAR);

        if (rawJson == null || rawJson.trim().isEmpty()) {
            System.err.println("FATAL ERROR: Required environment variable '" + CREDENTIALS_VAR + "' is MISSING or EMPTY.");
            throw new RuntimeException("Firebase initialization failed: Authentication JSON is missing in the environment. Please ensure " + CREDENTIALS_VAR + " is set correctly in Railway Variables.");
        }

        System.out.println("FirebaseConfig: Successfully read raw JSON data from " + CREDENTIALS_VAR + ".");

        try (InputStream serviceAccount = new ByteArrayInputStream(rawJson.getBytes())) {

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
                optionsBuilder.setDatabaseUrl(databaseUrl);
                System.out.println("FirebaseConfig: Database URL set to " + databaseUrl);
            }

            FirebaseOptions options = optionsBuilder.build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp app = FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK initialized successfully.");
                return app;
            } else {
                return FirebaseApp.getInstance();
            }

        } catch (IOException e) {
            System.err.println("FATAL ERROR: Failed to read Raw JSON string. Check if the value of " + CREDENTIALS_VAR + " is valid JSON.");
            e.printStackTrace();
            throw new RuntimeException("Firebase initialization failed due to JSON parsing error.", e);
        } catch (Exception e) {
             System.err.println("FATAL ERROR: An unexpected error occurred during Firebase initialization.");
             e.printStackTrace();
             throw new RuntimeException("Firebase initialization failed.", e);
        }
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
