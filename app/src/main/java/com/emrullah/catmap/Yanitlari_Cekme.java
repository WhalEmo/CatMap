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
    private DocumentSnapshot lastVisibleDoc;

    public void yanitlariCek(Yorum_Model yorum, ArrayList<Yanit_Model> yanitlar,Yanit_Adapter yorumAdapter,int limit, boolean clearList) {
        if (clearList) {
            lastVisibleDoc=null;
            yanitlar.clear();
            yorumAdapter.notifyDataSetChanged();
        }

        CollectionReference yanitlarRef = db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("yanitlar");

        Query query = yanitlarRef  //query sorgu cumelsi olusturu meslea 5 tane getir ,sırala gibi
                .orderBy("yanitzaman", Query.Direction.DESCENDING)
                .limit(limit);
        if(lastVisibleDoc!=null){
            query=query.startAfter(lastVisibleDoc);
        }
        query.get().addOnSuccessListener(snapshots -> {
            if (snapshots != null && !snapshots.isEmpty()) {
                if (clearList) {
                    yanitlar.clear();
                    yorumAdapter.notifyDataSetChanged();
                }

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Yanit_Model yanit = new Yanit_Model(
                            doc.getId(),
                            doc.getString("kullanici_adi"),
                            doc.getString("yaniticerik"),
                            doc.getDate("yanitzaman")
                    );
                    yanitlar.add(yanit);
                }
                yorumAdapter.notifyDataSetChanged();

                lastVisibleDoc = snapshots.getDocuments().get(snapshots.size() - 1);//, sonuncu belgeyi almak
                yorum.setSonYanit(lastVisibleDoc); // yorum modeline eklemiş olman gerek
            }
            yorum.setYanitlar(yanitlar);
        });


    }


}
