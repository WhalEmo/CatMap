package com.beem.catmap.userAuth;

import com.google.firebase.auth.FirebaseAuth;

public class VerifyAuth {
    private FirebaseAuth auth;

    public VerifyAuth(){
        auth = FirebaseAuth.getInstance();
    }

    public interface VerifyCallback {
        void onSuccess(boolean isSuccess);
    }

    public void savePasswordEmail(String email, String password, VerifyCallback callback){
        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(gorev -> {
                    callback.onSuccess(gorev.isSuccessful());
                });
    }

    public void resetPassword(String email, VerifyCallback callback){
        if (email.isEmpty() || email == null) {
            callback.onSuccess(false);
        }
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    callback.onSuccess(task.isSuccessful());
                });
    }

    public void login(String email, String sifre, VerifyCallback callback){
        auth.signInWithEmailAndPassword(email,sifre)
                .addOnCompleteListener(gorev -> {
                    callback.onSuccess(gorev.isSuccessful());
                });
    }

}
