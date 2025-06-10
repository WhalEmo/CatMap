package com.emrullah.catmap;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;

public class Yanitlari_Cekme {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration yanitListener;
    private int cekileceksayac;
    public void yanitlariCek(Yorum_Model yorum, ArrayList<Yanit_Model> yanitlar) {
        if (yanitListener != null) {
            yanitListener.remove();  // Önceki listener varsa kaldır
        }
        CollectionReference yanitlarRef = db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("yanitlar");

        yanitListener = yanitlarRef
                .orderBy("yanitzaman", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    cekileceksayac=0;
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
                                    yanitlar.add(0, yanit);
                                    cekileceksayac++;
                                    if(cekileceksayac>5){
                                        break;
                                    }
                                    break;
                                case REMOVED:
                                    for (int i = 0; i < yanitlar.size(); i++) {
                                        if (yanitlar.get(i).getYanitId().equals(yanitID)) {
                                            cekileceksayac--;
                                            yanitlar.remove(i);
                                            if(cekileceksayac>5){
                                                break;
                                            }
                                            break;
                                        }
                                    }
                                    break;
                                case MODIFIED:
                                    for (int i = 0; i < yanitlar.size(); i++) {
                                        if (yanitlar.get(i).getYanitId().equals(yanitID)) {
                                            yanitlar.set(i, yanit);
                                            if(cekileceksayac>5){
                                                break;
                                            }
                                            break;
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                    yorum.setYanitlar(yanitlar);

                });
    }


}
