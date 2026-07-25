package com.beem.catmap.YorumYanit;

import android.content.Context;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Begeni_Kod_Yoneticisi_Yanit {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void YanitBegenme(String catId, String yorumId, Yanit_Model yanit, String kullaniciId, Context context, Yanit_Adapter adapter) {
        if (catId == null || catId.isEmpty() || yorumId == null || yorumId.isEmpty()) return;

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanit.getYanitId())
                .collection("begenenlerYanit")
                .document(kullaniciId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    Set<String> begenilenSet = CacheHelperYanit.loadBegenilenSet(context);
                    begenilenSet.add(yanit.getYanitId());
                    CacheHelperYanit.saveBegenilenSet(context, begenilenSet);

                    yanit.setBegeniSayisiYanit(yanit.getBegeniSayisiYanit() + 1);
                    Map<String, Integer> map = CacheHelperYanit.loadBegeniSayilariMap(context);
                    int yeniSayi = map.getOrDefault(yanit.getYanitId(), 0) + 1;

                    db.collection("cats")
                            .document(catId)
                            .collection("yorumlar")
                            .document(yorumId)
                            .collection("yanitlar")
                            .document(yanit.getYanitId())
                            .update("begeniSayisiYanit", yeniSayi);

                    map.put(yanit.getYanitId(), yeniSayi);
                    CacheHelperYanit.saveBegeniSayilariMap(context, map);

                    // ListAdapter için doğrudan veri setlerini aktarıyoruz
                    adapter.setBegeniSayisiYanitMap(map);
                    adapter.setBegenilenYanitIdSeti(begenilenSet);
                });
    }

    public void YanitBegeniKaldirma(String catId, String yorumId, Yanit_Model yanit, String kullaniciId, Context context, Yanit_Adapter adapter) {
        if (catId == null || catId.isEmpty() || yorumId == null || yorumId.isEmpty()) return;

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanit.getYanitId())
                .collection("begenenlerYanit")
                .document(kullaniciId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Set<String> begenilenSet = CacheHelperYanit.loadBegenilenSet(context);
                    begenilenSet.remove(yanit.getYanitId());
                    CacheHelperYanit.saveBegenilenSet(context, begenilenSet);

                    int mevcut = yanit.getBegeniSayisiYanit();
                    if (mevcut > 0) {
                        yanit.setBegeniSayisiYanit(mevcut - 1);
                    }

                    Map<String, Integer> map = CacheHelperYanit.loadBegeniSayilariMap(context);
                    int yeniSayi = Math.max(map.getOrDefault(yanit.getYanitId(), 1) - 1, 0);

                    db.collection("cats")
                            .document(catId)
                            .collection("yorumlar")
                            .document(yorumId)
                            .collection("yanitlar")
                            .document(yanit.getYanitId())
                            .update("begeniSayisiYanit", yeniSayi);

                    map.put(yanit.getYanitId(), yeniSayi);
                    CacheHelperYanit.saveBegeniSayilariMap(context, map);

                    // ListAdapter için doğrudan veri setlerini aktarıyoruz
                    adapter.setBegeniSayisiYanitMap(map);
                    adapter.setBegenilenYanitIdSeti(begenilenSet);
                });
    }

    public void KullanicininBegendigiYanitlar(String catId, Context context, String kullaniciId, Yanit_Adapter adapter, Yorum_Model yorum) {
        if (catId == null || catId.isEmpty() || yorum == null) return;

        Set<String> begenilenYanitIDSeti = new HashSet<>();
        Map<String, Integer> begeniSayisiMap = new HashMap<>();

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorum.getYorumID())
                .collection("yanitlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int toplamYanitSayisi = querySnapshot.size();
                    if (toplamYanitSayisi == 0) {
                        CacheHelperYanit.saveBegeniSayilariMap(context, begeniSayisiMap);
                        adapter.setBegeniSayisiYanitMap(begeniSayisiMap);

                        CacheHelperYanit.saveBegenilenSet(context, begenilenYanitIDSeti);
                        adapter.setBegenilenYanitIdSeti(begenilenYanitIDSeti);
                        return;
                    }

                    AtomicInteger sayac = new AtomicInteger(0);

                    for (DocumentSnapshot yanitDoc : querySnapshot) {
                        String yanitID = yanitDoc.getId();

                        yanitDoc.getReference()
                                .collection("begenenlerYanit")
                                .get()
                                .addOnSuccessListener(begeniSnapshot -> {
                                    int begeniSayisi = begeniSnapshot.size();
                                    begeniSayisiMap.put(yanitID, begeniSayisi);

                                    // ListAdapter'daki mevcut listenin bir kopyasını alıyoruz
                                    List<Yanit_Model> mevcutListe = new ArrayList<>(adapter.getYanitList());
                                    for (Yanit_Model model : mevcutListe) {
                                        if (model.getYanitId().equals(yanitID)) {
                                            model.setBegeniSayisiYanit(begeniSayisi);
                                            break;
                                        }
                                    }

                                    CacheHelperYanit.saveBegeniSayilariMap(context, begeniSayisiMap);
                                    adapter.setBegeniSayisiYanitMap(begeniSayisiMap);

                                    // 🟢 Modeldeki beğeni sayısı değiştiği için DiffUtil hesaplamasına gönderiyoruz
                                    adapter.submitList(mevcutListe);
                                });

                        yanitDoc.getReference()
                                .collection("begenenlerYanit")
                                .document(kullaniciId)
                                .get()
                                .addOnSuccessListener(begeniDoc -> {
                                    if (begeniDoc.exists()) {
                                        begenilenYanitIDSeti.add(yanitID);
                                    }

                                    if (sayac.incrementAndGet() == toplamYanitSayisi) {
                                        CacheHelperYanit.saveBegenilenSet(context, begenilenYanitIDSeti);
                                        adapter.setBegenilenYanitIdSeti(begenilenYanitIDSeti);
                                    }
                                });
                    }
                });
    }
}