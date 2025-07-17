package com.beem.catmap;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.beem.catmap.ui.main.ProfilSayfasiFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GonderiKaydetmeYardimciSinif {
    public static void kullaniciyaGonderiKaydet(Activity activity, String kediID, ConstraintLayout main, UyariMesaji mesaji){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference kullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
        Map<String, Object> yeniKedi = new HashMap<>();
        yeniKedi.put("kediID", kediID);
        yeniKedi.put("tarih", Timestamp.now());
        kullaniciRef.update("GonderilenKediler", FieldValue.arrayUnion(yeniKedi))
                .addOnSuccessListener(aVoid -> {
                    if (main != null) main.setVisibility(View.GONE);
                    ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(MainActivity.kullanici.getID());
                    ((AppCompatActivity) activity).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, fragment)
                            .addToBackStack(null)
                            .commit();
                    mesaji.BasariliDurum("Eklendi",1000);
                })
                .addOnFailureListener(e -> {
                    mesaji.BasarisizDurum("Eklenemedi",1000);
                    Log.e("Yukle", "yukleme başarısız: " + e.getMessage());
                });
    }

}
