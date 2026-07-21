package com.beem.catmap.Profil.Gonderiler;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.beem.catmap.MainActivity;
import com.beem.catmap.Profil.ProfilSayfasiFragment;
import com.beem.catmap.UyariMesaji;
import com.beem.catmap.data.repository.UserRepository;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GonderiKaydetmeYardimciSinif {
    public static void kullaniciyaGonderiKaydet(Context context, String kediID, ConstraintLayout main, UyariMesaji mesaji){
        UserRepository userRepository = UserRepository.Companion.getInstance(context);

        if (!userRepository.isUserLoggedIn()) {
            if (mesaji != null) mesaji.BasarisizDurum("Oturum bulunamadı!", 1000);
            return;
        }

        String currentUserId = userRepository.getCurrentUser().getID();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference kullaniciRef = db.collection("users").document(currentUserId);
        Map<String, Object> yeniKedi = new HashMap<>();
        yeniKedi.put("kediID", kediID);
        yeniKedi.put("tarih", Timestamp.now());
        kullaniciRef.update("GonderilenKediler", FieldValue.arrayUnion(yeniKedi))
                .addOnSuccessListener(aVoid -> {
                    Bundle args = ProfilSayfasiFragment.newArgs(currentUserId);
                    SmartNavigationEngine.navigateTo(
                            Screen.PROFILE,
                            args
                    );
                    mesaji.BasariliDurum("Eklendi",1000);
                })
                .addOnFailureListener(e -> {
                    mesaji.BasarisizDurum("Eklenemedi",1000);
                    Log.e("Yukle", "yukleme başarısız: " + e.getMessage());
                });
    }

}
