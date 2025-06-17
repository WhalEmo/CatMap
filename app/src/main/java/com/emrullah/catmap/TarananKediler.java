package com.emrullah.catmap;

import android.content.Context;
import android.location.Location;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;


public class TarananKediler {

    public interface TarananKediCallback {
        void onKedilerAlindi();
    }

    ExtendedFloatingActionButton taramaButon;

    public TarananKediler() {
    }

    public void ButonGosterim(GoogleMap map, View view){
        taramaButon = view.findViewById(R.id.btnScanArea);

        LatLng[] ekranMerkezi = {map.getCameraPosition().target};
        map.setOnCameraIdleListener(()->{
            LatLng gecerliMerkez = map.getCameraPosition().target;

            float[] results = new float[1];
            Location.distanceBetween(
                    ekranMerkezi[0].latitude,
                    ekranMerkezi[0].longitude,
                    gecerliMerkez.latitude,
                    gecerliMerkez.longitude,
                    results
            );

            if (results[0] > 500) {
                if (taramaButon.getVisibility() == View.GONE) {
                    taramaButon.show();
                }
                ekranMerkezi[0] = gecerliMerkez;
            }
        });
    }

    public void Basildi(ArrayList<Kediler> kediler, GoogleMap map, TarananKediCallback callback, Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference kedilerKoleksiyonu = db.collection("cats");
        final double YAKINLIK_METRE = 1000.0;
        taramaButon.setOnClickListener(buton ->{
            LatLng ekranMerkezi = map.getCameraPosition().target;
            taramaButon.hide();

            UyariMesaji uyarimesa = new UyariMesaji(context,true);
            uyarimesa.YuklemeDurum("Taranıyor..");
            int boyut = kediler.size();
            ArrayList<Kediler> bulunanKediler = new ArrayList<>();

            kedilerKoleksiyonu.get().addOnSuccessListener(Tablo->{
                for (QueryDocumentSnapshot veri: Tablo){
                    double latude = veri.getDouble("latitude");
                    double longtude = veri.getDouble("longitude");
                    float[] araMesafe = new float[1];
                    Location.distanceBetween(
                            ekranMerkezi.latitude,
                            ekranMerkezi.longitude,
                            latude,
                            longtude,
                            araMesafe
                    );
                    if (araMesafe[0] <= YAKINLIK_METRE){
                        String kediId = veri.getId();
                        String kedism = veri.getString("kediAdi");
                        String markerUrl=veri.getString("photoUri");
                        String hakkindaa=veri.getString("kediHakkinda");
                        Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl);
                        kediler.add(kedi);
                        bulunanKediler.add(kedi);
                    }
                }
                if (boyut == kediler.size()){
                    uyarimesa.BasarisizDurum("Kedi Bulunamadı",1000);
                }
                else{
                    float[] araMesafe = new float[1];
                    float enkucukMesafe = Float.MAX_VALUE;
                    Kediler enkucukKedi = null;
                    for (Kediler kedi: bulunanKediler){
                        Location.distanceBetween(
                                ekranMerkezi.latitude,
                                ekranMerkezi.longitude,
                                kedi.getLatitude(),
                                kedi.getLongitude(),
                                araMesafe
                        );
                        if(araMesafe[0] < enkucukMesafe){
                            enkucukMesafe = araMesafe[0];
                            enkucukKedi = kedi;
                        }
                    }
                    uyarimesa.BasariliDurum("Kediler Bulundu",500);

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(enkucukKedi.getLatitude(),enkucukKedi.getLongitude()),19f),2000,null);
                }
                new Thread(()->{
                    callback.onKedilerAlindi();
                }).start();
            });
        });
    }
}


