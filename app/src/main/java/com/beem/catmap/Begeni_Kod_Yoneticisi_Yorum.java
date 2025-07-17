package com.beem.catmap;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Begeni_Kod_Yoneticisi_Yorum {
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

            Set<String> begenilenSet = CacheHelperYorum.loadBegenilenSet(context);
            begenilenSet.add(yorum.getYorumID());
            CacheHelperYorum.saveBegenilenSet(context, begenilenSet);


            yorum.setBegeniSayisi(yorum.getBegeniSayisi() + 1);
            Map<String, Integer> map = CacheHelperYorum.loadBegeniSayilariMap(context);
            int yeniSayi = map.getOrDefault(yorum.getYorumID(), 0) + 1;

                    db.collection("cats")
                            .document(MapsActivity.kediID)
                            .collection("yorumlar")
                            .document(yorum.getYorumID())
                            .update("begeniSayisi", yeniSayi);

                     map.put(yorum.getYorumID(), yeniSayi);
                     CacheHelperYorum.saveBegeniSayilariMap(context, map);
                     adapter.setBegeniSayisiMap(map);
                     adapter.setBegenilenYorumIDSeti(begenilenSet);
                     adapter.notifyDataSetChanged();
        })
                .addOnFailureListener(e -> {
                    // Hata durumunda istersen Toast veya Log
                });
    }
    public void YorumBegeniKladirma(Yorum_Model yorum,String kullaniciId,Context context,Yorum_Adapter adapter){
            db.collection("cats")
                    .document(MapsActivity.kediID)
                    .collection("yorumlar")
                    .document(yorum.getYorumID())
                    .collection("begenenler")
                    .document(kullaniciId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
            // Başarılı silme → Cache güncelle
            Set<String> begenilenSet = CacheHelperYorum.loadBegenilenSet(context);
            begenilenSet.remove(yorum.getYorumID());
            CacheHelperYorum.saveBegenilenSet(context, begenilenSet);


            int mevcut = yorum.getBegeniSayisi();
            if (mevcut > 0) {
                yorum.setBegeniSayisi(mevcut - 1);
            }

            Map<String, Integer> map = CacheHelperYorum.loadBegeniSayilariMap(context);
            int yeniSayi = Math.max(map.getOrDefault(yorum.getYorumID(), 1) - 1, 0);
                        // Firebase'e yeni sayı yaz
                        db.collection("cats")
                                .document(MapsActivity.kediID)
                                .collection("yorumlar")
                                .document(yorum.getYorumID())
                                .update("begeniSayisi", yeniSayi);

            map.put(yorum.getYorumID(), yeniSayi);
            CacheHelperYorum.saveBegeniSayilariMap(context, map);
            adapter.setBegeniSayisiMap(map);
            adapter.setBegenilenYorumIDSeti(begenilenSet);
            adapter.notifyDataSetChanged();
        })
                .addOnFailureListener(e -> {
                    // Hata durumunda istersen Toast veya Log
                });
    }
    public void KullanicininBegendigiYorumalar(Context context, String kullaniciId, Yorum_Adapter adapter){
        Set<String> begenilenYorumIDSeti = new HashSet<>();
        Map<String, Integer> begeniSayisiMap = new HashMap<>();
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
                                    begeniSayisiMap.put(yorumID,begeniSayisi);


                                    // Yorum modelini adapter üzerinden bul ve güncelle
                                    for (Yorum_Model model : adapter.getYorumList()) {
                                        if (model.getYorumID().equals(yorumID)) {
                                            model.setBegeniSayisi(begeniSayisi);
                                            break;
                                        }
                                    }
                                    CacheHelperYorum.saveBegeniSayilariMap(context, begeniSayisiMap);
                                    adapter.setBegeniSayisiMap(begeniSayisiMap);

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
                                        CacheHelperYorum.saveBegenilenSet(context, begenilenYorumIDSeti);
                                        adapter.setBegenilenYorumIDSeti(begenilenYorumIDSeti);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }

                    if (toplamYorumSayisi == 0) {
                        CacheHelperYorum.saveBegenilenSet(context, begenilenYorumIDSeti);
                        adapter.setBegenilenYorumIDSeti(begenilenYorumIDSeti);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    public interface YorumSayisiCallback {
        void onYorumSayisiAlindi(int sayi);
    }


    public void yorumSayisiniGetir(YorumSayisiCallback callback) {
        db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int toplam = querySnapshot.size();
                    callback.onYorumSayisiAlindi(toplam);
                })
                .addOnFailureListener(e -> {
                    callback.onYorumSayisiAlindi(0);
                });
    }

}
