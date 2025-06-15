package com.emrullah.catmap;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class Yanitlari_Cekme {
    public interface YanitlarCallback {
        void onComplete();
    }

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void yanitlariCek(Yorum_Model yorum, ArrayList<Yanit_Model> yanitlar, Yanit_Adapter yorumAdapter, int limit, boolean clearList, YanitlarCallback callback) {
        if (clearList) {
            yanitlar.clear();
            yorumAdapter.notifyDataSetChanged();
            yorum.setSonYanit(null);
        }

        CollectionReference yanitlarRef = db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("yanitlar");

        Query query = yanitlarRef
                .orderBy("yanitzaman", Query.Direction.ASCENDING)
                .limit(limit);

        DocumentSnapshot lastVisibleDoc = yorum.getSonYanit();
        if (lastVisibleDoc != null) {
            query = query.startAfter(lastVisibleDoc);
        }

        query.get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Yanit_Model yanit = new Yanit_Model(
                                    doc.getId(),
                                    doc.getString("kullanici_adi"),
                                    doc.getString("yaniticerik"),
                                    doc.getDate("yanitzaman")
                            );
                            yanitlar.add(yanit);
                        }

                        yorum.setSonYanit(snapshots.getDocuments().get(snapshots.size() - 1));

                        // Daha fazla göster butonunu güncelle
                        if (snapshots.size() < limit) {
                            yorum.setDahafazlaGozukuyorMu(false);
                        } else {
                            yorum.setDahafazlaGozukuyorMu(true);
                        }
                    } else {
                        // Eğer hiç veri yoksa
                        yorum.setDahafazlaGozukuyorMu(false);
                    }

                    if (yanitlar.isEmpty()) {
                        yorum.setYanitYokMu(true);
                    } else {
                        yorum.setYanitYokMu(false);
                    }

                    yorum.setYanitlar(yanitlar);
                    yorumAdapter.notifyDataSetChanged();

                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.e("Yanitlari_Cekme", "Yanıtlar çekilemedi", e);
                    callback.onComplete();
                });
    }



}

