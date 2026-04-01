package com.medreport.ai.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.medreport.ai.models.UserModel;

public class AuthManager {
    private static AuthManager instance;
    private String cachedToken;
    private UserModel currentUser;
    private final FirebaseAuth firebaseAuth;

    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    public interface TokenCallback { void onToken(String token); void onError(Exception e); }

    public void refreshToken(TokenCallback cb) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) { cb.onError(new Exception("Not logged in")); return; }
        user.getIdToken(true).addOnSuccessListener(result -> {
            cachedToken = result.getToken();
            cb.onToken(cachedToken);
        }).addOnFailureListener(cb::onError);
    }

    public String getCachedToken() { return cachedToken; }
    public void setCachedToken(String t) { cachedToken = t; }
    public UserModel getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserModel u) { currentUser = u; }
    public FirebaseUser getFirebaseUser() { return firebaseAuth.getCurrentUser(); }
    public boolean isLoggedIn() { return firebaseAuth.getCurrentUser() != null && cachedToken != null; }

    public void signOut() {
        firebaseAuth.signOut();
        cachedToken = null;
        currentUser = null;
    }
}
