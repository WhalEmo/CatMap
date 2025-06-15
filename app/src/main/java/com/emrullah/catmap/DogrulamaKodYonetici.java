package com.emrullah.catmap;

import com.google.firebase.auth.FirebaseAuth;

public class DogrulamaKodYonetici {
    private FirebaseAuth auth;

    public DogrulamaKodYonetici(){
        auth = FirebaseAuth.getInstance();
    }

    public interface DogrulamaCallback{
        void onBasarili(boolean basariliMi);
    }

    public void kaydetSifreEmail(String email, String sifre, DogrulamaCallback callback){
        auth.createUserWithEmailAndPassword(email,sifre)
                .addOnCompleteListener(gorev -> {
                    callback.onBasarili(gorev.isSuccessful());
                });
    }

    public void sifreSifirla(String email, DogrulamaCallback callback){
        if (email.isEmpty() || email == null) {
            callback.onBasarili(false);
        }
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    callback.onBasarili(task.isSuccessful());
                });
    }

    public void girisYap(String email, String sifre, DogrulamaCallback callback){
        auth.signInWithEmailAndPassword(email,sifre)
                .addOnCompleteListener(gorev -> {
                    callback.onBasarili(gorev.isSuccessful());
                });
    }

}
