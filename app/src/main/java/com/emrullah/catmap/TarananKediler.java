package com.emrullah.catmap;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;

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

    private ExtendedFloatingActionButton taramaButon;
    private ImageButton solOk;
    private ImageButton sagOk;
    private LatLng BasilmaEkranMerkezi;
    private int indeks = 0;
    private ArrayList<Kediler> bulunanKediler;
    private boolean goster = true;

    public TarananKediler() {
    }

    public void ButonGosterim(GoogleMap map, View view){
        taramaButon = view.findViewById(R.id.btnScanArea);
        solOk = view.findViewById(R.id.btnLeftArrow);
        sagOk = view.findViewById(R.id.btnRightArrow);

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
            float[] results2 = new float[1];
            if(BasilmaEkranMerkezi!=null){
                Location.distanceBetween(
                        BasilmaEkranMerkezi.latitude,
                        BasilmaEkranMerkezi.longitude,
                        gecerliMerkez.latitude,
                        gecerliMerkez.longitude,
                        results2
                );
                if (results2[0] > 1000){
                    oklariGizle();
                }
                else{
                    oklariGoster();
                }
            }
        });
    }

    public void Basildi(ArrayList<Kediler> kediler, GoogleMap map, TarananKediCallback callback, Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference kedilerKoleksiyonu = db.collection("cats");
        final double YAKINLIK_METRE = 1000.0;
        taramaButon.setOnClickListener(buton ->{
            BasilmaEkranMerkezi = map.getCameraPosition().target;
            taramaButon.hide();

            UyariMesaji uyarimesa = new UyariMesaji(context,true);
            uyarimesa.YuklemeDurum("Taranıyor..");
            int boyut = kediler.size();
            if(bulunanKediler == null){
                bulunanKediler = new ArrayList<>();
            }
            else{
                bulunanKediler.clear();
            }
            kedilerKoleksiyonu.get().addOnSuccessListener(Tablo->{
                for (QueryDocumentSnapshot veri: Tablo){
                    double latude = veri.getDouble("latitude");
                    double longtude = veri.getDouble("longitude");
                    float[] araMesafe = new float[1];
                    Location.distanceBetween(
                            BasilmaEkranMerkezi.latitude,
                            BasilmaEkranMerkezi.longitude,
                            latude,
                            longtude,
                            araMesafe
                    );
                    if (araMesafe[0] <= YAKINLIK_METRE){
                        String kediId = veri.getId();
                        String kedism = veri.getString("kediAdi");
                        String YukleyenId=veri.getString("YukleyenKullaniciID");
                        ArrayList<String> urller = (ArrayList<String>) veri.get("photoUri");
                        String markerUrl = urller.get(0);
                        String hakkindaa=veri.getString("kediHakkinda");
                        Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl,urller,YukleyenId);
                        kediler.add(kedi);
                        bulunanKediler.add(kedi);
                    }
                }
                if (boyut == kediler.size()){
                    uyarimesa.BasarisizDurum("Kedi Bulunamadı",1000);
                    BasilmaEkranMerkezi = null;
                    oklariGizle();
                    return;
                }
                else{
                    float[] araMesafe = new float[1];
                    float enkucukMesafe = Float.MAX_VALUE;
                    Kediler enkucukKedi = null;
                    for (Kediler kedi: bulunanKediler){
                        Location.distanceBetween(
                                BasilmaEkranMerkezi.latitude,
                                BasilmaEkranMerkezi.longitude,
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
                    oklariGoster();
                    indeks = kediler.indexOf(enkucukKedi);
                }
                new Thread(()->{
                    callback.onKedilerAlindi();
                }).start();
            });
        });
    }


    private void oklariGoster() {
        if(!goster){
            return;
        }
        goster = false;
        solOk.setVisibility(View.VISIBLE);
        sagOk.setVisibility(View.VISIBLE);

        solOk.post(() -> {
            float offset = solOk.getWidth() + 40;
            solOk.setTranslationX(-offset);
            solOk.animate()
                    .translationX(0)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });

        sagOk.post(() -> {
            float offset = sagOk.getWidth() + 40;
            sagOk.setTranslationX(offset);
            sagOk.animate()
                    .translationX(0)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }


    private void oklariGizle() {
        if (goster) {
            return;
        }
        goster = true;
        solOk.post(() -> {
            float offset = solOk.getWidth() + 40;
            solOk.animate()
                    .translationX(-offset)
                    .setDuration(400)
                    .withEndAction(() -> solOk.setVisibility(View.INVISIBLE))
                    .start();
        });

        sagOk.post(() -> {
            float offset = sagOk.getWidth() + 40;
            sagOk.animate()
                    .translationX(offset)
                    .setDuration(400)
                    .withEndAction(() -> sagOk.setVisibility(View.INVISIBLE))
                    .start();
        });
    }



    public void SagOkBas(GoogleMap map){
        sagOk.setOnClickListener(v->{
           indeks = (indeks+1)%bulunanKediler.size();
           map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bulunanKediler.get(indeks).getLatitude(),bulunanKediler.get(indeks).getLongitude()),19f),2000,null);
        });
    }

    public void SolOkBas(GoogleMap map){
        solOk.setOnClickListener(v->{
            indeks = (indeks-1+bulunanKediler.size())%bulunanKediler.size();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bulunanKediler.get(indeks).getLatitude(),bulunanKediler.get(indeks).getLongitude()),19f),2000,null);
        });
    }

}


