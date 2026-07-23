package com.beem.catmap.YorumYanit;

import android.content.Context;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Begeni_Kod_Yoneticisi_Yorum {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 🟢 String catId parametresi eklendi
    public void YorumBegenme(String catId, Yorum_Model yorum, String kullaniciId, Context context, Yorum_Adapter adapter) {
        if (catId == null || catId.isEmpty()) return;

        db.collection("cats")
                .document(catId)
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
                            .document(catId)
                            .collection("yorumlar")
                            .document(yorum.getYorumID())
                            .update("begeniSayisi", yeniSayi);

                    map.put(yorum.getYorumID(), yeniSayi);
                    CacheHelperYorum.saveBegeniSayilariMap(context, map);
                    adapter.setBegeniSayisiMap(map);
                    adapter.setBegenilenYorumIDSeti(begenilenSet);
                    adapter.notifyDataSetChanged();
                });
    }

    // 🟢 String catId parametresi eklendi
    public void YorumBegeniKladirma(String catId, Yorum_Model yorum, String kullaniciId, Context context, Yorum_Adapter adapter) {
        if (catId == null || catId.isEmpty()) return;

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("begenenler")
                .document(kullaniciId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Set<String> begenilenSet = CacheHelperYorum.loadBegenilenSet(context);
                    begenilenSet.remove(yorum.getYorumID());
                    CacheHelperYorum.saveBegenilenSet(context, begenilenSet);

                    int mevcut = yorum.getBegeniSayisi();
                    if (mevcut > 0) {
                        yorum.setBegeniSayisi(mevcut - 1);
                    }

                    Map<String, Integer> map = CacheHelperYorum.loadBegeniSayilariMap(context);
                    int yeniSayi = Math.max(map.getOrDefault(yorum.getYorumID(), 1) - 1, 0);

                    db.collection("cats")
                            .document(catId)
                            .collection("yorumlar")
                            .document(yorum.getYorumID())
                            .update("begeniSayisi", yeniSayi);

                    map.put(yorum.getYorumID(), yeniSayi);
                    CacheHelperYorum.saveBegeniSayilariMap(context, map);
                    adapter.setBegeniSayisiMap(map);
                    adapter.setBegenilenYorumIDSeti(begenilenSet);
                    adapter.notifyDataSetChanged();
                });
    }

    // 🟢 String catId parametresi eklendi
    public void KullanicininBegendigiYorumalar(String catId, Context context, String kullaniciId, Yorum_Adapter adapter) {
        if (catId == null || catId.isEmpty()) return;

        Set<String> begenilenYorumIDSeti = new HashSet<>();
        Map<String, Integer> begeniSayisiMap = new HashMap<>();

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int toplamYorumSayisi = querySnapshot.size();
                    AtomicInteger sayac = new AtomicInteger(0);

                    for (DocumentSnapshot yorumDoc : querySnapshot) {
                        String yorumID = yorumDoc.getId();

                        yorumDoc.getReference()
                                .collection("begenenler")
                                .get()
                                .addOnSuccessListener(begeniSnapshot -> {
                                    int begeniSayisi = begeniSnapshot.size();
                                    begeniSayisiMap.put(yorumID, begeniSayisi);

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

    public void yorumSayisiniGetir(String catId, YorumSayisiCallback callback) {
        if (catId == null || catId.isEmpty()) {
            callback.onYorumSayisiAlindi(0);
            return;
        }

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int toplam = querySnapshot.size();
                    callback.onYorumSayisiAlindi(toplam);
                })
                .addOnFailureListener(e -> callback.onYorumSayisiAlindi(0));
    }
}