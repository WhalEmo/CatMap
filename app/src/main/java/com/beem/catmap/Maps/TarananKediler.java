package com.beem.catmap.Maps;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.RelativeLayout;

import com.beem.catmap.Maps.MapKedi.Kediler;
import com.beem.catmap.R;
import com.beem.catmap.UyariMesaji;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TarananKediler {

    public interface TarananKediCallback {
        void onKedilerAlindi();
    }

    private ExtendedFloatingActionButton taramaButon;
    private RelativeLayout container;
    private LatLng BasilmaEkranMerkezi;
    private ArrayList<Kediler> bulunanKediler;

    public TarananKediler() {}

    public void ButonGosterim(GoogleMap map, View view) {
        taramaButon = view.findViewById(R.id.btnScanArea);
        container = view.findViewById(R.id.mapOverlayContainer);
        final LatLng[] ekranMerkezi = {map.getCameraPosition().target};

        map.setOnCameraIdleListener(() -> {
            LatLng gecerliMerkez = map.getCameraPosition().target;
            float[] results = new float[1];
            Location.distanceBetween(
                    ekranMerkezi[0].latitude, ekranMerkezi[0].longitude,
                    gecerliMerkez.latitude, gecerliMerkez.longitude, results);

            if (results[0] > 500) {
                if (taramaButon.getVisibility() == View.GONE) {
                    if (container.getVisibility() == View.GONE) container.setVisibility(View.VISIBLE);
                    taramaButon.show();
                }
                ekranMerkezi[0] = gecerliMerkez;
            }
        });
    }

    public void Basildi(ArrayList<Kediler> kediler, GoogleMap map, TarananKediCallback callback, Context context) {
        taramaButon.setOnClickListener(buton -> {
            BasilmaEkranMerkezi = map.getCameraPosition().target;
            taramaButon.hide();

            UyariMesaji uyarimesa = new UyariMesaji(context, true);
            uyarimesa.YuklemeDurum("Çevredeki Kediler Taranıyor..");

            if (bulunanKediler == null) {
                bulunanKediler = new ArrayList<>();
            } else {
                bulunanKediler.clear();
            }

            // 1. GeoHash Arama Sınırlarını (Bounding Box) Belirle - Yarıçap: 5000 Metre (5 km)
            final GeoLocation center = new GeoLocation(BasilmaEkranMerkezi.latitude, BasilmaEkranMerkezi.longitude);
            final double radiusInM = 5000.0;
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
            final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 2. Sunucu Taraflı Filtreleme (Sadece O Bölgedeki Verileri İndir)
            for (GeoQueryBounds b : bounds) {
                Query q = db.collection("cats")
                        .orderBy("geohash")
                        .startAt(b.startHash)
                        .endAt(b.endHash);
                tasks.add(q.get());
            }

            // 3. Arka Planda Tüm Sorguların Tamamlanmasını Bekle
            Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                for (Task<QuerySnapshot> task : tasks) {
                    QuerySnapshot snap = task.getResult();
                    for (DocumentSnapshot veri : snap.getDocuments()) {
                        double latude = veri.getDouble("latitude");
                        double longtude = veri.getDouble("longitude");

                        // GeoHash kare mantığıyla çalıştığı için köşelerde kalanları net 5km ile filtrele
                        GeoLocation docLocation = new GeoLocation(latude, longtude);
                        double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);

                        if (distanceInM <= radiusInM) {
                            String kediId = veri.getId();
                            String kedism = veri.getString("kediAdi");
                            String YukleyenId = veri.getString("YukleyenKullaniciID");
                            ArrayList<String> urller = (ArrayList<String>) veri.get("photoUri");
                            String markerUrl = urller.get(0);
                            String hakkindaa = veri.getString("kediHakkinda");

                            Kediler kedi = new Kediler(kediId, kedism, hakkindaa, latude, longtude, markerUrl, urller, YukleyenId);
                            kediler.add(kedi);
                            bulunanKediler.add(kedi);
                        }
                    }
                }

                // 4. UI Güncellemeleri
                if (bulunanKediler.isEmpty()) {
                    uyarimesa.BasarisizDurum("Yakınlarda Kedi Bulunamadı", 1500);
                    BasilmaEkranMerkezi = null;
                } else {
                    uyarimesa.BasariliDurum(bulunanKediler.size() + " Kedi Bulundu!", 1000);

                    // Merkeze en yakın kediyi bulup kamerayı odaklar
                    Kediler enYakin = bulunanKediler.get(0);
                    double minDistance = GeoFireUtils.getDistanceBetween(new GeoLocation(enYakin.getLatitude(), enYakin.getLongitude()), center);

                    for (Kediler k : bulunanKediler) {
                        double dist = GeoFireUtils.getDistanceBetween(new GeoLocation(k.getLatitude(), k.getLongitude()), center);
                        if (dist < minDistance) {
                            minDistance = dist;
                            enYakin = k;
                        }
                    }

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(enYakin.getLatitude(), enYakin.getLongitude()), 17f), 1500, null);
                }

                // Thread bloklanmasını önlemek için callback'i tetikle
                new Thread(callback::onKedilerAlindi).start();
            });
        });
    }
}