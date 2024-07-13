package com.noti.server.process.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseHelper {
    public static void init(String credentialPath) throws IOException {
        FileInputStream serviceAccount = new FileInputStream(credentialPath);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://notisender-41c1b.firebaseio.com")
                .build();
        FirebaseApp.initializeApp(options);
    }

    public static boolean verifyToken(String idToken, String uid) {
        try {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            UserRecord userRecord = firebaseAuth.getUser(decodedToken.getUid());
            return uid.equals(userRecord.getUid());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
