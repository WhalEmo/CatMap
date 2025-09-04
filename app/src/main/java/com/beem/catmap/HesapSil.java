package com.beem.catmap;

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class HesapSil {
   private FirebaseFirestore db = FirebaseFirestore.getInstance();
   private DatabaseReference ref = FirebaseDatabase.getInstance().getReference("mesajlar");

    private ArrayList<String> takipEdilenler = new ArrayList<>();
    private ArrayList<String> takipciler = new ArrayList<>();
    private ArrayList<Map<String, Object>> yuklenenKediler;

    public void HesapSilmeBaslat(){
        db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipEdilenler")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            takipEdilenler.add(doc.getId());
                        }
                        TakipEdilenlerCikartma();
                    }
                });

        db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipciler")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots1 -> {
                    if (!queryDocumentSnapshots1.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots1) {
                            takipciler.add(doc.getId());
                        }
                        TakipcilerCikartma();
                    }
                });

        db.collection("users")
                .document(MainActivity.kullanici.getID())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    yuklenenKediler = documentSnapshot.contains("GonderilenKediler") ? (ArrayList<Map<String, Object>>) documentSnapshot.get("GonderilenKediler") : new ArrayList<>();
                    KedilerSilme();
                });
        MesajlariSil();
    }


    private void KedilerSilme(){
        for (Map<String, Object> gonderi : yuklenenKediler) {
            String kediID = (String) gonderi.get("kediID");
            db.collection("cats")
                    .document(kediID)
                    .delete();
        }

    }

    private void TakipEdilenlerCikartma(){
        for (String takipEdilen : takipEdilenler) {
            db.collection("users")
                    .document(takipEdilen)
                    .collection("takipciler")
                    .document(MainActivity.kullanici.getID())
                    .delete();
        }
    }

    private void TakipcilerCikartma(){
        for (String takipci : takipciler) {
            db.collection("users")
                    .document(takipci)
                    .collection("takipEdilenler")
                    .document(MainActivity.kullanici.getID())
                    .delete();
        }
    }

    private void MesajlariSil() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey() != null) {
                        String[] idler = child.getKey().split("_");
                        if (idler.length == 2) {
                            String id1 = idler[0];
                            String id2 = idler[1];

                            if (id1.equals(MainActivity.kullanici.getID()) || id2.equals(MainActivity.kullanici.getID())) {
                                ref.child(child.getKey()).removeValue();
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
