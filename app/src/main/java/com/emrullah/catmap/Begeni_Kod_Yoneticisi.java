package com.emrullah.catmap;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Begeni_Kod_Yoneticisi {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void YorumBegenme(Yorum_Model yorum,String kullaniciId,Context context,Yorum_Adapter adapter){
        db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("begenenler")
                .document(kullaniciId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
            // Başarılı ekleme → Cache güncelle
            Set<String> begenilenSet = CacheHelper.loadBegenilenSet(context);
            begenilenSet.add(yorum.getYorumID());
            CacheHelper.saveBegenilenSet(context, begenilenSet);


            yorum.setBegeniSayisi(yorum.getBegeniSayisi() + 1);

            // Adapter'ı güncelle ve bildir
            adapter.setBegenilenYorumIDSeti(begenilenSet);
            adapter.notifyDataSetChanged();
        })
                .addOnFailureListener(e -> {
                    // Hata durumunda istersen Toast veya Log
                });
    }
    public void YorumBegeniKladirma(Yorum_Model yorum,String kullaniciId,Context context,Yorum_Adapter adapter){
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("cats")
                    .document(MapsActivity.kediID)
                    .collection("yorumlar")
                    .document(yorum.getYorumID())
                    .collection("begenenler")
                    .document(kullaniciId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
            // Başarılı silme → Cache güncelle
            Set<String> begenilenSet = CacheHelper.loadBegenilenSet(context);
            begenilenSet.remove(yorum.getYorumID());
            CacheHelper.saveBegenilenSet(context, begenilenSet);


            int mevcut = yorum.getBegeniSayisi();
            if (mevcut > 0) {
                yorum.setBegeniSayisi(mevcut - 1);
            }

            // Adapter'ı güncelle ve bildir
            adapter.setBegenilenYorumIDSeti(begenilenSet);
            adapter.notifyDataSetChanged();
        })
                .addOnFailureListener(e -> {
                    // Hata durumunda istersen Toast veya Log
                });
    }
    public void KullanicininBegendigiYorumalar(Context context, String kullaniciId, Yorum_Adapter adapter){
        Set<String> begenilenYorumIDSeti = new HashSet<>();
        db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int toplamYorumSayisi = querySnapshot.size();
                    AtomicInteger sayac = new AtomicInteger(0);

                    for (DocumentSnapshot yorumDoc : querySnapshot) {
                        String yorumID = yorumDoc.getId();


                        // Beğeni sayısını çek
                        yorumDoc.getReference()
                                .collection("begenenler")
                                .get()
                                .addOnSuccessListener(begeniSnapshot -> {
                                    int begeniSayisi = begeniSnapshot.size();

                                    // Yorum modelini adapter üzerinden bul ve güncelle
                                    for (Yorum_Model model : adapter.getYorumList()) {
                                        if (model.getYorumID().equals(yorumID)) {
                                            model.setBegeniSayisi(begeniSayisi);
                                            break;
                                        }
                                    }
                                });

                        yorumDoc.getReference()
                                .collection("begenenler")
                                .document(kullaniciId)
                                .get()
                                .addOnSuccessListener(begeniDoc -> {
                                    if (begeniDoc.exists()) {
                                        begenilenYorumIDSeti.add(yorumID);
                                    }

                                    if (sayac.incrementAndGet() == toplamYorumSayisi) {
                                        // Firestore’dan veri hazır, cache’e kaydet
                                        CacheHelper.saveBegenilenSet(context, begenilenYorumIDSeti);
                                        // Adapter’a bildir
                                        adapter.setBegenilenYorumIDSeti(begenilenYorumIDSeti);
                                        adapter.notifyDataSetChanged();
                                    }
                                    int begenisayisi=querySnapshot.size();
                                });
                    }

                    if (toplamYorumSayisi == 0) {
                        CacheHelper.saveBegenilenSet(context, begenilenYorumIDSeti);
                        adapter.setBegenilenYorumIDSeti(begenilenYorumIDSeti);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    public void YorumlarınBegeniSayisi(){

    }

}
