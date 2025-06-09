package com.emrullah.catmap;

import android.content.Context;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;

public class Yanitlari_Cekme {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration yanitListener;

    public void yanitlariCek(Yorum_Model yorum, RecyclerView yntRecyclerView, Context context,ArrayList<Yanit_Model> adapterYanitListesi) {
        Yanit_Adapter yanitAdapter = new Yanit_Adapter(adapterYanitListesi, context);
        yntRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        yntRecyclerView.setAdapter(yanitAdapter);

        // Firestore yolunu oluştur
        CollectionReference yanitlarRef = db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("yanitlar");

        // Eski listener varsa kaldır
        if (yanitListener != null) {
            yanitListener.remove();
        }

        // Yeni listener
        yanitListener = yanitlarRef
                .orderBy("yanitzaman", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Yanıtlar", "Dinleyici hatası: ", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();
                            String yanitID = doc.getId();
                            String kAdi = doc.getString("kullanici_adi");
                            String yanitIcerik = doc.getString("yaniticerik");
                            Date yanitZaman = doc.getDate("yanitzaman");

                            Yanit_Model yanit = new Yanit_Model(yanitID, kAdi, yanitIcerik, yanitZaman);

                            switch (dc.getType()) {
                                case ADDED:
                                    adapterYanitListesi.add(0, yanit);
                                    yanitAdapter.notifyItemInserted(0);
                                    break;
                                case REMOVED:
                                    for (int i = 0; i < adapterYanitListesi.size(); i++) {
                                        if (adapterYanitListesi.get(i).getYanitId().equals(yanitID)) {
                                            adapterYanitListesi.remove(i);
                                            yanitAdapter.notifyItemRemoved(i);
                                            break;
                                        }
                                    }
                                    break;
                                case MODIFIED:
                                    for (int i = 0; i < adapterYanitListesi.size(); i++) {
                                        if (adapterYanitListesi.get(i).getYanitId().equals(yanitID)) {
                                            adapterYanitListesi.set(i, yanit);
                                            yanitAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                });
    }
}
