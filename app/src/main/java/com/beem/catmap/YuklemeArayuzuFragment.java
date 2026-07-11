package com.beem.catmap;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.Manifest;
import android.location.Location;

import com.beem.catmap.Maps.FotoGeciciAdapter;
import com.beem.catmap.Profil.Gonderiler.GonderiKaydetmeYardimciSinif;
import com.beem.catmap.ui.camera.CameraFragment;
import com.beem.catmap.ui.manager.ImageUploadManager;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class YuklemeArayuzuFragment extends Fragment {

    private Uri photoUri;
    private File photoFile;
    private EditText kedininismi;
    private EditText kedininhakkindasi;
    private FusedLocationProviderClient konumsaglayici;
    private Button kaydetButton;
    private ImageButton fotoSec;
    private ImageButton kameraAc;
    private FirebaseFirestore db;
    double latitude=0;
    double longitude=0;
    String kediadi;
    String kedihakkinda;
    private UyariMesaji mesaji;
    private ArrayList<Uri> secilenFotolar = new ArrayList<>();
    private ViewPager2 fotoPager;
    private FotoGeciciAdapter fotoAdapter;
    private ImageView geciciFoto;
    final int MAX_FOTO_SAYISI = 5;
    private ConstraintLayout main;
    private InterstitialAd reklamAD;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    public YuklemeArayuzuFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.yukleme_arayuzu, container, false);
        main=view.findViewById(R.id.main);
        geciciFoto = view.findViewById(R.id.geciciFoto);
        fotoPager = view.findViewById(R.id.fotoPager);
        fotoAdapter = new FotoGeciciAdapter(requireContext(), secilenFotolar,null);
        fotoPager.setAdapter(fotoAdapter);

        kedininismi=view.findViewById(R.id.isimText);
        kedininhakkindasi=view.findViewById(R.id.hakkindaText);
        kaydetButton=view.findViewById(R.id.kaydetmeButonu);
        fotoSec=view.findViewById(R.id.dosya_id);
        kameraAc=view.findViewById(R.id.kamera_id);
        butonAyarlari();
        // FusedLocationProviderClient başlat
        konumsaglayici = LocationServices.getFusedLocationProviderClient(requireContext());
        // Firestore Başlat
        db = FirebaseFirestore.getInstance();
        mesaji = new UyariMesaji(requireContext(),false);

        // reklam ayarlarına başla
        ReklamYukleme();


        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi();
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);


        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        CevrimIciYonetimi.getInstance().setYuklemeEkraniGorunuyor(true);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }

    @Override
    public void onStop() {
        super.onStop();
        CevrimIciYonetimi.getInstance().setYuklemeEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CevrimIciYonetimi.getInstance().setYuklemeEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }

    @Override
    public void onPause() {
        super.onPause();
        CevrimIciYonetimi.getInstance().setYuklemeEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }




    // 📌 Fotoğraf dosyası oluşturma
    private Uri getPhotoFileUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//cekilen footgrafın ne zaman cekildigini gosterir
        String fileName = "JPEG_" + timeStamp + ".jpg";

        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);//klasor yolunu tutuyor. klasore gitme yolunu bilen bi nesne
        //filepaths xml otomatik klasor olusturdu
        try {
            photoFile = File.createTempFile(fileName, ".jpg", storageDir);// dosya olustu
            return FileProvider.getUriForFile(requireContext(), "com.beem.catmap.fileprovider", photoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openCamera() {
        Fragment cameraFragment = new CameraFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .addToBackStack(null)
                .commit();
    }

    // 📌 Kullanıcının konumunu al
    private void getUserLocation() {
        //  kullanıcıdan izin iste
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
            return;
        }

        // 📍 Son bilinen konumu al
        konumsaglayici.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();  // Enlem
                    longitude = location.getLongitude(); // Boylam
                    ResimlerVeriTabaniKaydi();

                    // 📌 Kullanıcıya Toast mesajı göster
                    System.out.println( "Konum: " + latitude + ", " + longitude);
                } else {
                    System.out.println( "Konum alınamadı!");
                }
            }
        });

    }

    private void ResimlerVeriTabaniKaydi(){
        if(secilenFotolar.size()==0){
            mesaji.BasarisizDurum("Lütfen fotoğraf ekleyiniz!",1000);
            return;
        }
        ArrayList<String> fotoURL = new ArrayList<>();
        AtomicInteger yuklenenSayisi = new AtomicInteger(0);
        for (Uri uri : secilenFotolar) {
            String dosya = "fotoklasoru/" + System.currentTimeMillis() + "_" + yuklenenSayisi.get() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(dosya);
            storageRef.putFile(uri)
                    .addOnSuccessListener(sonuc -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(url -> {
                            fotoURL.add(url.toString());
                            int tamamlanan = yuklenenSayisi.incrementAndGet();
                            if (tamamlanan == secilenFotolar.size()) {
                                VerilerinVeritabaninaKaydi(fotoURL);
                            }
                        });
                    }).addOnFailureListener(hata -> {
                        mesaji.BasarisizDurum("Fotoğraf yüklenemedi!", 1000);
                    });
        }

    }

    private void VerilerinVeritabaninaKaydi(ArrayList<String> fotoUrl) {
        if (latitude == 0 && longitude == 0) {
            mesaji.BasarisizDurum("Lütfen kedinin konumunu giriniz!",1000);
        } else {
            // Firestore'a kaydedilecek veri yapısı
            Map<String, Object> catData = new HashMap<>();
            String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));
            catData.put("kediAdi", kediadi);
            catData.put("kediHakkinda", kedihakkinda);
            catData.put("latitude", latitude);
            catData.put("longitude", longitude);
            catData.put("geohash", hash);
            catData.put("photoUri", fotoUrl);
            catData.put("YukleyenKullaniciID",MainActivity.kullanici.getID());
            db.collection("cats")
                    .add(catData)
                    .addOnSuccessListener(documentReference -> {
                        mesaji.BasariliDurum("Kedi bilgileri başarıyla kaydedildi!",1000);

                        // burda reklamı ver
                        ReklamVer();

                        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_dialog_tasarimi, null);
                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setView(dialogView)
                                .create();
                        dialogView.findViewById(R.id.btn_yes).setOnClickListener(v -> {
                            mesaji.YuklemeDurum("Ekleniyor...");
                            GonderiKaydetmeYardimciSinif.kullaniciyaGonderiKaydet(
                                    requireActivity(),
                                    documentReference.getId(),
                                    main,
                                    mesaji
                            );

                            dialog.dismiss();
                        });

                        dialogView.findViewById(R.id.btn_no).setOnClickListener(v -> {
                            dialog.dismiss();
                        });

                        dialog.show();

                        secilenFotolar.clear();
                        fotoAdapter.notifyDataSetChanged();
                        geciciFoto.setVisibility(View.VISIBLE);
                        kedininismi.getText().clear();
                        kedininhakkindasi.getText().clear();

                    })
                    .addOnFailureListener(e -> {
                        mesaji.BasarisizDurum("Kedi kaydedilirken hata oluştu.",1000);
                    });
        }
    }


    private void butonAyarlari(){
        kaydetButton.setOnClickListener(v ->{
            kaydet();
        });
        fotoSec.setOnClickListener(v ->{

        });
        kameraAc.setOnClickListener(v ->{
            openCamera();
        });
    }

    private void kaydet() {
        //anlık cekilmedityse yani dosyadan secildiyse adres girsin
         kediadi = kedininismi.getText().toString().trim();
         kedihakkinda = kedininhakkindasi.getText().toString().trim();
         mesaji.YuklemeDurum("Kaydediliyor...");
        if (kediadi.isEmpty()) {
            mesaji.BasarisizDurum("Lütfen kedi ismini giriniz!",1000);
            return;
        }
        if (secilenFotolar == null || secilenFotolar.isEmpty()) {
            mesaji.BasarisizDurum("Lütfen kedinin fotoğrafını yükleyiniz!",1000);
            return;
        }
        if ( !kediadi.isEmpty() && !secilenFotolar.isEmpty()) {
            getUserLocation();
        }
    }


    private void ReklamYukleme(){
        MobileAds.initialize(requireContext(), initializationStatus -> {});

        AdRequest reklamIstek = new AdRequest.Builder().build();
        InterstitialAd.load(requireContext(),
                "ca-app-pub-3940256099942544/1033173712", // sahte id
                reklamIstek,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        reklamAD = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        reklamAD = null;
                    }
                });
    }
    private void ReklamVer(){
        if (reklamAD != null) {
            reklamAD.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Reklam kapatıldıktan sonra yapılacak işlemler
                    // Örneğin yeni aktivite açabilirsin
                    // startActivity(new Intent(MainActivity.this, IkinciActivity.class));
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    // Reklam gösterilemedi
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    reklamAD = null;
                }
            });

            reklamAD.show(requireActivity());
        } else {

        }
    }
}
