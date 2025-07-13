package com.emrullah.catmap;

import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.emrullah.catmap.ui.main.MainViewModel;
import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.emrullah.catmap.databinding.ActivityMapsBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hdodenhof.circleimageview.CircleImageView;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback , BottomSheetController{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetDialog bottomSheetDialog;
    private View bottomSheetView;
    private TextView isim, hakkinda;
    private ViewPager2 fotoPager;
    private LocationCallback locationCallback;
    private View ikinci;
    private  BottomSheetDialog ikincibottom;
    private RecyclerView yorumlarRecyclerView;
    public static LinearLayout yorumicin;
    public static LinearLayout ynticin;
    public static LinearLayout carpiicin;
    private RelativeLayout yuklemeEkrani;
    private TextView bosyorum;
    private ImageButton iptalButton;
    public  static EditText kimeyanit;
    public static EditText textt;
    private  EditText TEXT;
    private ImageButton yorumbutton;
    private ImageButton yanıtbutton;
    private TextView yorumSayisiTextView;
    private Begeni_Kod_Yoneticisi_Yorum begeniKodYoneticisi;
    Map<String, Bitmap> fotoCache = new HashMap<>();
    List<Target> targetListesi = new ArrayList<>();
    private FotografYukleyiciYonetici fotografYukleyiciYonetici = new FotografYukleyiciYonetici(fotoCache, targetListesi);
    private FotoGeciciAdapter fotoAdapter;
    private ArrayList<Uri> fotolar = new ArrayList<>();
    private TextView yukleyenAdiText;
    private ImageView yukleyenPP;
    private LinearLayout profilAlan;
    private RelativeLayout anaGorunum;
    private ImageView YrmgndrFotoImageView;
    private ImageView YntgndrFotoImageView;
    private URLye_Ulasma ulasma;
    private ImageView kalpImageView;
    private TextView begeniSayisiTextView;
    private ImageView GonderiEkleButton;
    private UyariMesaji mesaji;
    private Marker sonTiklananMarker;
    String gosterilecekKediID;
    private MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(MainActivity.kullanici.getID());
        // Firestore cache ayarını yap
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        yuklemeEkrani = findViewById(R.id.yuklemeekran);
        anaGorunum=findViewById(R.id.anaGorunum);
        // Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        konumizni();

        mesaji=new UyariMesaji(this,true);
        bottomSheetView = getLayoutInflater().inflate(R.layout.markerdaki_kediyi_gosterme, null);
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // 1. Behavior al
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                // 2. Yüksekliği tam ekran yap
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(layoutParams);

                // 3. BottomSheet'i expanded moda al (tam ekran gibi)
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
//        SohbetMesajAyarlari();

        begeniKodYoneticisi=new Begeni_Kod_Yoneticisi_Yorum();
        isim = bottomSheetView.findViewById(R.id.isimgosterme);
        hakkinda = bottomSheetView.findViewById(R.id.hakkindagosterme);
        fotoPager = bottomSheetView.findViewById(R.id.fotoPager);
        kalpImageView=bottomSheetView.findViewById(R.id.kalpImageView);
        begeniSayisiTextView=bottomSheetView.findViewById(R.id.begeniSayisiTextView);
        GonderiEkleButton=bottomSheetView.findViewById(R.id.GonderiEkleButton);
        yorumSayisiTextView=bottomSheetView.findViewById(R.id.yorumSayisiTextView);
        fotoAdapter = new FotoGeciciAdapter(this,fotolar,fotografYukleyiciYonetici);
        fotoPager.setAdapter(fotoAdapter);


        ikinci= getLayoutInflater().inflate(R.layout.yorum_gosterme,null);
        ikincibottom=new BottomSheetDialog(this);
        ikincibottom.setContentView(ikinci);

        yorumlarRecyclerView = ikinci.findViewById(R.id.yorumlarRecyclerView);
        yorumicin=ikinci.findViewById(R.id.yorumgndrLayout);
        ynticin=ikinci.findViewById(R.id.yntgndrLayout);
        carpiicin=ikinci.findViewById(R.id.carpilayout);
        iptalButton=ikinci.findViewById(R.id.iptalButton);
        kimeyanit=ikinci.findViewById(R.id.kimeyanit);
        textt =ikinci.findViewById(R.id.yntEditText);
        TEXT=ikinci.findViewById(R.id.yorumEditText);
        yorumbutton=ikinci.findViewById(R.id.yorumgonder);
        yanıtbutton=ikinci.findViewById(R.id.yntgonder);
        yorumbutton.setEnabled(false);
        yorumbutton.setAlpha(0.5f);
        yanıtbutton.setEnabled(false);
        yanıtbutton.setAlpha(0.5f);
        bosyorum=ikinci.findViewById(R.id.bosYorumTextView);
        YrmgndrFotoImageView=ikinci.findViewById(R.id.YrmgndrFotoImageView);
        YntgndrFotoImageView=ikinci.findViewById(R.id.YntgndrFotoImageView);
        ulasma=new URLye_Ulasma();
        ulasma.IDdenUrlyeUlasma(MainActivity.kullanici.getID(),YrmgndrFotoImageView);
        ulasma.IDdenUrlyeUlasma(MainActivity.kullanici.getID(),YntgndrFotoImageView);
        BegenileriCek();

        textt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean dolumu = !s.toString().trim().isEmpty();
                yanıtbutton.setEnabled(dolumu);
                yanıtbutton.setAlpha(dolumu ? 1f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        TEXT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean dolumu = !s.toString().trim().isEmpty();
                yorumbutton.setEnabled(dolumu);
                yorumbutton.setAlpha(dolumu ? 1f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        Klavye klavye=new Klavye(this);
        iptalButton.setOnClickListener(v -> {
            View currentFocus = this.getCurrentFocus();
            if (currentFocus != null) {
                klavye.klavyeKapat(currentFocus);
            }
            carpiicin.setVisibility(View.GONE);
            ynticin.setVisibility(View.GONE);
            yorumicin.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                klavye.klavyeAc(TEXT);
            }, 250);
        });
         gosterilecekKediID = getIntent().getStringExtra("kediId");
    }
    public void sonTiklananMarkeriSil() {
        if (sonTiklananMarker != null) {
            sonTiklananMarker.remove();
            markerlar.remove(sonTiklananMarker);
            sonTiklananMarker = null;
        }
    }

    private void konumizni() {
        // Eğer izin verilmemişse
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Kullanıcıdan izin iste
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        } else {
            // İzin zaten verilmiş
            Toast.makeText(this, "Konum izni zaten verilmiş.", Toast.LENGTH_SHORT).show();
            // Burada konumu almaya başlayabilirsin
        }
    }

    // Kullanıcı izne cevap verdiğinde burası çalışır
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi
                Toast.makeText(this, "Konum izni verildi.", Toast.LENGTH_SHORT).show();
                // Burada konumu almaya başlayabilirsin
            } else {
                // İzin reddedildi
                Toast.makeText(this, "Konum izni reddedildi!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    boolean bittimi = true;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bittimi = false;
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
        MainActivity.kullanici.setLatitude(latitude);
        MainActivity.kullanici.setLongitude(longitude);
            if (yorumAdapter != null) {
                yorumAdapter.durdurZamanlayici();
                ArrayList<Yorum_Model> yorumlar = yorumAdapter.getYorumList();
                for (Yorum_Model yorum : yorumlar) {
                    Yanit_Adapter yntadapter = yorum.getYanitAdapter();
                    if (yntadapter != null) {
                        yntadapter.durdurZamanlayici();
                    }
                }
            }

    }

    @Override
    protected void onResume() {
        super.onResume();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(true);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
            if (KediSilmeDurumu.getInstance().isSilindiMi()) {
                vericekme(); // sadece silme olduysa
                KediSilmeDurumu.getInstance().setSilindiMi(false); // sıfırla
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }
    @Override
    protected void onPause() {
        super.onPause();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi();
    }


    private int dpDenPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    private BitmapDescriptor Duzenleme(int dp, Bitmap foto){
        int genislik = dpDenPx(dp);
        int yukseklik = dpDenPx(dp);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(foto, genislik, yukseklik, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    Marker kullanici;
    LatLng sydney;
    double latitude;
    double longitude;
    private boolean konumAlindi = false;

    public void konumbasma() {
        runOnUiThread(() -> {
            sydney = new LatLng(MainActivity.kullanici.getLatitude(), MainActivity.kullanici.getLongitude());
            kullanici = mMap.addMarker(new MarkerOptions().position(sydney).title("konum").icon(Duzenleme(70, BitmapFactory.decodeResource(getResources(), R.drawable.kullanimarker))));

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    // Güncel konumu al ve marker'ı güncelle
                    if (latitude != 0 && longitude != 0) {
                        sydney = new LatLng(latitude, longitude);
                        kullanici.setPosition(sydney);
                        if(konumAlindi){
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,8f));
                            new Handler().postDelayed(() -> {
                                yuklemeEkrani.setVisibility(View.GONE);
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16f), 2000, null);
                            }, 1000);
                            konumAlindi=false;
                            TarananKediler tarama =  new TarananKediler();
                            tarama.ButonGosterim(mMap,findViewById(android.R.id.content));
                            tarama.Basildi(kediler,mMap,()->{
                                resimlimarker();
                            },MapsActivity.this);
                            tarama.SagOkBas(mMap);
                            tarama.SolOkBas(mMap);
                        }
                        // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                    }

                    // Tekrar 5 saniye sonra çalıştırmak için tekrar çağır
                    handler.postDelayed(this, 500);  // 5000 ms = 5 saniye
                }
            };
            // İlk başlatma
            handler.post(updateRunnable);
        });
    }

    public void konumalma() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);  // 10 saniyede bir güncelleme almak
        locationRequest.setFastestInterval(1000); // En hızlı güncelleme 5 saniye
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Yüksek doğruluk

        // Konum güncellemeleri alacak callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            // Yeni konumu işliyoruz
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            //  Toast.makeText(MapsActivity.this,
                            //       "Latitude: " + latitude + "\nLongitude: " + longitude,
                            //       Toast.LENGTH_LONG).show();
                            // Burada marker'ı taşıyabilirsiniz veya diğer işlemleri yapabilirsiniz
                        }
                    }
                }
            }
        };

        // Konum güncellemelerini başlatıyoruz
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        konumAlindi = true;
    }
    private Target picassoTarget;
    public void HaritadaGor(String kediid) {
        if(bottomSheetDialog.isShowing()){
            bottomSheetDialog.dismiss();
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cats")
                .document(kediid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String kediId = documentSnapshot.getId();
                        String YukleyenID=documentSnapshot.getString("YukleyenKullaniciID");
                        double latitudee = documentSnapshot.getDouble("latitude");
                        double longitudee = documentSnapshot.getDouble("longitude");
                        String isim = documentSnapshot.getString("kediAdi");
                        ArrayList<String> fotoUrl = (ArrayList<String>) documentSnapshot.get("photoUri");
                        String hakkindaa=documentSnapshot.getString("kediHakkinda");

                        LatLng konum = new LatLng(latitudee, longitudee);
                        if (Math.abs(latitude - latitudee) <= 0.009 && Math.abs(longitude - longitudee) <= 0.0113){
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(konum, 16f));
                        }else {
                            if (mMap != null) {
                                Kediler kedi=new Kediler(kediId,isim,hakkindaa,latitudee,longitudee,fotoUrl.get(0),fotoUrl,YukleyenID);
                                kediler.add(kedi);
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(konum, 16f));
                                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                    picassoTarget = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            Bitmap customMarkerBitmap = fotoduzenle(bitmap);
                                            Marker yeniMarker = mMap.addMarker(new MarkerOptions()
                                                    .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                                    .position(konum)
                                                    .title(isim));
                                            yeniMarker.setTag(kediid);
                                            markerlar.add(yeniMarker);
                                        }

                                        @Override
                                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                                            Log.e("HaritadaGor", "Resim yüklenemedi: " + e.getMessage());
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {}
                                    };
                                    targets.add(picassoTarget);
                                    Picasso.get()
                                            .load(fotoUrl.get(0))
                                            .resize(100, 100)
                                            .centerCrop()
                                            .into(picassoTarget);
                                }
                            }
                        }
                    } else {
                        Log.e("HaritadaGor", "Belge bulunamadı.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HaritadaGor", "Firestore hatası: ", e);
                });
    }


    public void vericekme() {
        db.collection("cats").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot satir : queryDocumentSnapshots) {
                double latude = satir.getDouble("latitude");
                double longtude = satir.getDouble("longitude");
                if (Math.abs(latitude - latude) <= 0.009 && Math.abs(longitude - longtude) <= 0.0113) {
                    String kediId = satir.getId();
                    String kedism = satir.getString("kediAdi");
                    String YukleyenID=satir.getString("YukleyenKullaniciID");
                    ArrayList<String> URLler = (ArrayList<String>) satir.get("photoUri");
                    String markerUrl= URLler.get(0);
                    String hakkindaa=satir.getString("kediHakkinda");
                    Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl,URLler,YukleyenID);
                    kediler.add(kedi);
                }
            }
            Thread t2 = new Thread(() -> {
                resimlimarker();
            });
            t2.start();
        }).addOnFailureListener(e -> {
            Log.e("FIREBASE", "Hata oluştu: ", e);
        });
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setSmallestDisplacement(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    double latitudee = location.getLatitude();
                    double longitudee = location.getLongitude();
                    db.collection("cats").get().addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot satir : queryDocumentSnapshots) {
                            double latude = satir.getDouble("latitude");
                            double longtude = satir.getDouble("longitude");
                            if (Math.abs(latitudee - latude) <= 0.009 && Math.abs(longitudee - longtude) <= 0.0113) {
                                String kediId = satir.getId();
                                String kedism = satir.getString("kediAdi");
                                String YukleyenID=satir.getString("YukleyenKullaniciID");
                                ArrayList<String> URLler = (ArrayList<String>) satir.get("photoUri");
                                String markerUrl= URLler.get(0);
                                String hakkindaa=satir.getString("kediHakkinda");
                                Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl,URLler,YukleyenID);
                                kediler.add(kedi);
                            }
                        }
                        Thread t = new Thread(() -> {
                            resimlimarker();
                        });
                        t.start();

                    }).addOnFailureListener(e -> {
                        Log.e("FIREBASE", "Hata oluştu: ", e);
                    });
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
   }
   private void GonderiBegenisiEkleme(String kediId){
       DocumentReference ref = db.collection("users").document(MainActivity.kullanici.getID());
       DocumentReference kediRef = db.collection("cats").document(kediId);
       ref.update("begendigiGonderiler", FieldValue.arrayUnion(kediId))
               .addOnSuccessListener(aVoid -> {
                   CacheHelperGonderiBegeni.getInstance().begen(kediId);
                   kediRef.update("begeniSayisi", FieldValue.increment(1));
               })
               .addOnFailureListener(e -> {

               });
   }
   private void GonderiBegenisiKaldirma(String kediId){
       DocumentReference ref = db.collection("users").document(MainActivity.kullanici.getID());
       DocumentReference kediRef = db.collection("cats").document(kediId);
       ref.update("begendigiGonderiler", FieldValue.arrayRemove(kediId))
               .addOnSuccessListener(aVoid -> {
                   CacheHelperGonderiBegeni.getInstance().begeniKaldir(kediId);
                   kediRef.update("begeniSayisi", FieldValue.increment(-1));
               })
               .addOnFailureListener(e -> {
                   Log.e("BegeniKaldirma", "Beğeni kaldırılamadı: " + e.getMessage());
               });
   }
   private void BegenileriCek() {
       DocumentReference kullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
       kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
           if (documentSnapshot.exists()) {
               ArrayList<String> liste = (ArrayList<String>) documentSnapshot.get("begendigiGonderiler");
               if (liste != null) {
                   CacheHelperGonderiBegeni.getInstance().setBegeniList(new HashSet<>(liste));
               } else {
                   liste = new ArrayList<>();
                   CacheHelperGonderiBegeni.getInstance().setBegeniList(new HashSet<>(liste));
               }
           } else {
               Log.d("BegeniYukleme", "Kullanıcı belgesi yok");
           }
       });
   }
   private void BegeniSayisiCekToplam(String kediIDsi){
       FirebaseFirestore db = FirebaseFirestore.getInstance();
       DocumentReference kediRef = db.collection("cats").document(kediIDsi);
       kediRef.get().addOnSuccessListener(documentSnapshot -> {
           if (documentSnapshot.exists()) {
               Long begeniSayisi = documentSnapshot.getLong("begeniSayisi");
               if (begeniSayisi != null) {
                   begeniSayisiTextView.setText(begeniSayisi.toString());
               } else {
                   begeniSayisiTextView.setText("0");
               }
           }
       }).addOnFailureListener(e -> {
           Log.e("BegeniSayisi", "Beğeni sayısı çekilemedi: " + e.getMessage());
       });
   }

   // View'ı Bitmap'e Çeviren Yardımcı Fonksiyon
   private Bitmap fotoduzenle(Bitmap imageBitmap){
       View markerView = LayoutInflater.from(this).inflate(R.layout.marker_tasarim, null);

       CircleImageView markerImage = markerView.findViewById(R.id.marker_image); // Eğer yuvarlak istiyorsan
       markerImage.setImageBitmap(imageBitmap);

       markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
       markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

       Bitmap returnedBitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
       Canvas canvas = new Canvas(returnedBitmap);
       markerView.draw(canvas);
       return returnedBitmap;
   }

   ArrayList<Kediler>kediler=new ArrayList<>();
    List<Target> targets = new ArrayList<>(); // Target'ları burada saklıyoruz
    ArrayList<Marker>markerlar=new ArrayList<>();
    HashMap<String, Object> markerKEY = new HashMap<>();
    public void resimlimarker() {
        runOnUiThread(() -> {
        for (Kediler kedi : kediler) {
            if(kedi.isMarkerOlustuMu()){
                continue;
            }else{
                kedi.setMarkerOlustuMu(true);
            }
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        LatLng kedy = new LatLng(kedi.getLatitude(), kedi.getLongitude());
                        Bitmap customMarkerBitmap = fotoduzenle(bitmap);
                        if(markerKEY.containsKey(kedi.getURL())){
                            return;
                        }
                        Marker marker= mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                    .position(kedy)
                                    .title(kedi.getIsim()));
                        markerlar.add(marker);
                        markerKEY.put(kedi.getURL(),null);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Log.e("PİCASSO", "Fotoğraf yüklenemedi: " + e.getMessage());
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };

            targets.add(target);
            Picasso.get()
                    .load(kedi.getURL())
                    .resize(100, 100)
                    .centerCrop()
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(target);
           }
        });
    }

    private boolean isBackPressed = false;
    public void tiklanan_markerdaki_kedi(String ad, String hakkindasi, Uri Url,Kediler kedi,String YukleyenId) {
        profilAlan=bottomSheetView.findViewById(R.id.profilAlani);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (fragment instanceof ProfilSayfasiFragment) {
                profilAlan.setVisibility(View.GONE);
                anaGorunum.setVisibility(View.GONE);
            } else {
                profilAlan.setVisibility(View.VISIBLE);
                anaGorunum.setVisibility(View.VISIBLE);
                if (bottomSheetDialog != null && !bottomSheetDialog.isShowing()&&isBackPressed==true) {
                    bottomSheetDialog.show();
                }
            }
        });
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                isBackPressed = true;
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    // Geri tuş işlemi bittikten sonra flag sıfırlama (küçük gecikme ile)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        isBackPressed = false;
                    }, 100);
                } else {
                    finish();
                }
                // Sıfırlama burada olabilir ama dikkat et, bazen burada sıfırlarsan flag erken sıfırlanır.
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        YukleyenKullaniciDBgetir(YukleyenId);
        isim.setText(ad);
        hakkinda.setText(hakkindasi);
        fotolar.clear();
        fotoAdapter.notifyDataSetChanged();
        for(String url: kedi.getURLler()){
            fotolar.add(Uri.parse(url));
        }
        fotoAdapter.notifyDataSetChanged();
        if (!bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
        }
        /*
       +
       ymn // Önbellekten kontrol et"gt
        if (fotoCache.containsKey(Url.toString())) {
            if (!bottomSheetDialog.isShowing()) {
                bottomSheetDialog.show();
            }
            return;bv
        }
        // Yoksa yükle ve önbelleğe kaydet
        Target t = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                fotoCache.put(Url.toString(), bitmap); // Önbelleğe ekle
                if (!bottomSheetDialog.isShowing()) {
                    bottomSheetDialog.show();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.e("PİCASSO", "Fotoğraf yüklenemedi: " + e.getMessage());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };

        // Çöp toplayıcıya kaptırmamak için Target'ı listeye ekle
        targetListesi.add(t);
        Picasso.get().load(Url).into(t);*/
    }

    String ID;
    public static String kediID;
    String yorumID;
    String kediYukleyenID;
    public void kedibilgisigetirme(LatLng markerPosition){
        for(Kediler kedi:kediler){
            if(kedi.getLatitude()==markerPosition.latitude&&kedi.getLongitude()==markerPosition.longitude) {
                ID=kedi.getID();
                kediID=ID;
                kediYukleyenID=kedi.getYukleyenId();
                YorumSayisiToplam();
                if (CacheHelperGonderiBegeni.getInstance().begenmisMi(ID)) {
                    kalpImageView.setImageResource(R.drawable.baseline_favorite_24);
                } else {
                    kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24);
                }
                tiklanan_markerdaki_kedi(kedi.getIsim(), kedi.getHakkindasi(), Uri.parse(kedi.getURL()),kedi,kedi.getYukleyenId());
                BegeniSayisiCekToplam(ID);
                kalpImageView.setOnClickListener(v -> {
                    if (CacheHelperGonderiBegeni.getInstance().begenmisMi(kediID)) {
                        kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24);
                        String sayi=begeniSayisiTextView.getText().toString();
                        int begeni = Integer.parseInt(sayi);
                        begeni=begeni-1;
                        begeniSayisiTextView.setText(String.valueOf(begeni));
                        GonderiBegenisiKaldirma(ID);
                    } else {
                        kalpImageView.setImageResource(R.drawable.baseline_favorite_24);
                        String sayi=begeniSayisiTextView.getText().toString();
                        int begeni = Integer.parseInt(sayi);
                        begeni=begeni+1;
                        begeniSayisiTextView.setText(String.valueOf(begeni));
                        GonderiBegenisiEkleme(ID);
                    }
                });

            }
        }
    }
    public void YukleyenKullaniciDBgetir(String YId) {
        yukleyenAdiText = bottomSheetView.findViewById(R.id.yukleyenAdiText);
        yukleyenPP = bottomSheetView.findViewById(R.id.YukprofilFotoImageView);

        db.collection("users")
                .document(YId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String kullaniciAdi = documentSnapshot.getString("KullaniciAdi");
                        String ProfilUrl = documentSnapshot.getString("profilFotoUrl");
                        yukleyenAdiText.setText("@" + kullaniciAdi);

                        if (ProfilUrl != null) {
                            Picasso.get()
                                    .load(ProfilUrl)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.kullanici)
                                    .into(yukleyenPP);
                        }

                        // Eğer kendi profilimizse
                        if (YId.equals(MainActivity.kullanici.getID())) {
                            if (mViewModel == null) {
                                mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
                            }

                            DocumentReference kullaniciRef = db.collection("users").document(YId);
                            kullaniciRef.get().addOnSuccessListener(innerSnapshot -> {
                                List<Map<String, Object>> gonderilenKediler = (List<Map<String, Object>>) innerSnapshot.get("GonderilenKediler");

                                boolean kediZatenVar = false;

                                if (gonderilenKediler != null) {
                                    for (Map<String, Object> item : gonderilenKediler) {
                                        if (kediID.equals(item.get("kediID"))) {
                                            kediZatenVar = true;
                                            break;
                                        }
                                    }
                                }

                                GonderiEkleButton.setVisibility(View.VISIBLE);
                                boolean finalKediZatenVar = kediZatenVar;

                                GonderiEkleButton.setOnClickListener(v -> {
                                    PopupMenu popupMenu = new PopupMenu(bottomSheetView.getContext(), v);
                                    popupMenu.getMenuInflater().inflate(R.menu.kediyi_gosterme_uc_nokta, popupMenu.getMenu());
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.gonderi_ekle) {
                                            new AlertDialog.Builder(bottomSheetView.getContext())
                                                    .setTitle("Ekleme")
                                                    .setMessage("Bu kediyi gönderilerinize eklemek istiyor musunuz?")
                                                    .setPositiveButton("Evet", (dialog, which) -> {
                                                        mesaji.YuklemeDurum("Ekleniyor...");
                                                        if (finalKediZatenVar) {
                                                            mesaji.BasarisizDurum("Bu kedi zaten gönderilerinizde var!", 2000);
                                                        } else {
                                                            GonderiKaydetmeYardimciSinif.kullaniciyaGonderiKaydet(
                                                                    MapsActivity.this,
                                                                    kediID,
                                                                    null,
                                                                    mesaji
                                                            );
                                                            bottomSheetDialog.dismiss();
                                                        }
                                                    })
                                                    .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                                    .show();
                                            return true;
                                        }else if(item.getItemId() == R.id.HaritadanSilme){
                                            new AlertDialog.Builder(bottomSheetView.getContext())
                                                    .setTitle("Silme")
                                                    .setMessage("Kediyi haritadan silmek istiyor musunuz? Bu işlemi yaptığınızda, kediye ait gönderiler de silinecektir.")
                                                    .setPositiveButton("Evet", (dialog, which) -> {
                                                        mViewModel.HaritadanSilme(MapsActivity.kediID, () -> {
                                                            KediSilmeDurumu.getInstance().setSilindiMi(true);
                                                            mViewModel.kullaniciyaGonderiSil(MapsActivity.kediID,mesaji);
                                                            mViewModel.gonderiSil(MapsActivity.kediID);
                                                            sonTiklananMarkeriSil();
                                                            bottomSheetDialog.dismiss();
                                                        });
                                                        popupMenu.dismiss();
                                                    })
                                                    .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                                    .show();
                                            return true;
                                        }
                                        return false;
                                    });
                                    popupMenu.show();
                                });

                            });
                        } else {
                            GonderiEkleButton.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Kullanıcı alınamadı: " + e.getMessage());
                });
    }


    public void yukleyenProfilineGit(View view) {
        bottomSheetDialog.hide(); // dismiss yok eder hide gizler
        profilAlan.setVisibility(View.GONE);//dıstaki keilippharket eden
        anaGorunum.setVisibility(View.GONE);//MAPS
        ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(kediYukleyenID);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    ArrayList<Yorum_Model>yorumlar=new ArrayList<>();
    Yorum_Adapter yorumAdapter;
    public void YorumSayisiToplam(){
        yorumSayisiTextView.setText("Yükleniyor...");
        yorumSayisiTextView.setTextColor(Color.parseColor("#333333"));

        Animation fadeAnim = AnimationUtils.loadAnimation(this, R.anim.animasyonlu_yukleniyor);
        yorumSayisiTextView.startAnimation(fadeAnim);
        begeniKodYoneticisi.yorumSayisiniGetir(sayi -> {
            yorumSayisiTextView.clearAnimation();
            yorumSayisiTextView.setTextColor(Color.BLACK);
            yorumSayisiTextView.setText(sayi + " Yorum");
        });
    }


    private ListenerRegistration yorumListener;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private DocumentSnapshot lastVisible = null;
    private static final int PAGE_SIZE = 10;

    private void loadMoreYorumlar() {
        if (isLoading || isLastPage || lastVisible == null)
            return;

        isLoading = true;

        CollectionReference yorumlarRef = db.collection("cats")
                .document(ID)
                .collection("yorumlar");

        yorumlarRef
                .orderBy("zaman", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String IDsi=doc.getId();
                            String kAdi=doc.getString("kullanici_adi");
                            String icerik=doc.getString("icerik");
                            String YukleyenId=doc.getString("Yukleyen_ID");
                            Date zaman=doc.getDate("zaman");
                            Yorum_Model yorum=new Yorum_Model(IDsi,kAdi,icerik,zaman,null,YukleyenId);
                            yorumlar.add(yorum);
                        }
                        yorumAdapter.notifyDataSetChanged();

                        lastVisible = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                        return;
                    }
                    isLoading = false;
                });
    }


    public void patiyorumyap(View view){
        if (yorumListener != null) {
            yorumListener.remove();  // Önceki listener varsa kaldır
        }
        carpiicin.setVisibility(View.GONE);
        ynticin.setVisibility(View.GONE);
        yorumicin.setVisibility(View.VISIBLE);
        textt.setText("");
        Yorum_Adapter.yorumindeks = -1;


        yorumlar.clear();
        yorumAdapter = new Yorum_Adapter(yorumlar, this);
        yorumlarRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        yorumlarRecyclerView.setAdapter(yorumAdapter);

        yorumAdapter.setKullaniciAdiTiklamaListener(new KullaniciAdiTiklamaListener() {
            @Override
            public void onKullaniciAdiTiklandi(String kullaniciID) {
                bottomSheetDialog.hide();
                ikincibottom.hide();
                ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(kullaniciID);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment)
                        .addToBackStack(null)
                        .commit();

            }
        });
        FragmentManager.OnBackStackChangedListener listener = new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                if (fragment instanceof ProfilSayfasiFragment) {
                    profilAlan.setVisibility(View.GONE);
                    anaGorunum.setVisibility(View.GONE);
                } else {
                    profilAlan.setVisibility(View.VISIBLE);
                    anaGorunum.setVisibility(View.VISIBLE);

                    if (bottomSheetDialog != null && !bottomSheetDialog.isShowing()) {
                        bottomSheetDialog.show();
                    }
                    if (ikincibottom != null && !ikincibottom.isShowing()) {
                        ikincibottom.show();
                    }
                    getSupportFragmentManager().removeOnBackStackChangedListener(this);
                }
            }
        };
        // Listener’ı ekle
        getSupportFragmentManager().addOnBackStackChangedListener(listener);

        Set<String> cachedSet = CacheHelperYorum.loadBegenilenSet(this);
        Map<String, Integer> begeniMap = CacheHelperYorum.loadBegeniSayilariMap(this);
        yorumAdapter.setBegenilenYorumIDSeti(cachedSet);
        yorumAdapter.setBegeniSayisiMap(begeniMap);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            yorumAdapter.notifyItemChanged(Yorum_Adapter.yorumindeks);
            //yorumAdapter.notifyDataSetChanged();
        }, 100);
        begeniKodYoneticisi.KullanicininBegendigiYorumalar(this, MainActivity.kullanici.getID(), yorumAdapter);

        yorumAdapter.baslatZamanlayici();
        isLastPage = false;
        isLoading = false;
        lastVisible = null;

        CollectionReference yorumlarRef=db.collection("cats")
                .document(ID)
                .collection("yorumlar");
        yorumListener = yorumlarRef
                .orderBy("zaman", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .addSnapshotListener((snapshots,e)->{
                    if (e != null) {
                        Log.e("Yorumlar", "Dinleyici hatası: ", e);
                        return;
                    }
                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc=dc.getDocument();
                            String IDsi=doc.getId();
                            String kAdi=doc.getString("kullanici_adi");
                            String icerik=doc.getString("icerik");
                            String YukleyenID=doc.getString("Yukleyen_ID");
                            Date zaman=doc.getDate("zaman");
                            Yorum_Model yorum=new Yorum_Model(IDsi,kAdi,icerik,zaman,null,YukleyenID);
                            switch (dc.getType()){
                                case ADDED:
                                    yorumlar.add(0,yorum);
                                    yorumAdapter.notifyItemInserted(0);
                                    break;
                            }
                        }

                        if (!snapshots.isEmpty()) {
                            lastVisible = snapshots.getDocuments().get(snapshots.size() - 1);

                            if (snapshots.size() < PAGE_SIZE) {
                                isLastPage = true;
                            }
                        } else {
                            isLastPage = true;
                        }
                    }
                    if (yorumlar.isEmpty()) {
                        bosyorum.setVisibility(View.VISIBLE);
                        yorumlarRecyclerView.setVisibility(View.GONE);
                        isLastPage = true;
                    }else{
                        bosyorum.setVisibility(View.GONE);
                        yorumlarRecyclerView.setVisibility(View.VISIBLE);
                    }

                });
        yorumlarRecyclerView.clearOnScrollListeners(); // önceki scrollListener'ı temizle
        yorumlarRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {// Bu listener, RecyclerView kaydırıldıkça tetiklenir.
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount
                            && firstVisibleItem >= 0) {
                        loadMoreYorumlar(); // scroll'da daha fazla veri getir
                    }
                }
            }
        });
      if(!ikincibottom.isShowing()){
          ikincibottom.show();

      }
    }


    public void yorumgonder(View view){
        DBekle(ID,TEXT.getText().toString(),MainActivity.kullanici.getID());
        TEXT.setText("");
        yorumAdapter.yorumMuGeldi=true;
    }


    public void yntgonder(View view){
        if (Yorum_Adapter.yorumindeks < 0 || Yorum_Adapter.yorumindeks >= yorumlar.size()) return;
        Yorum_Model yorumm=yorumlar.get(Yorum_Adapter.yorumindeks);
        RecyclerView.ViewHolder holder = yorumlarRecyclerView.findViewHolderForAdapterPosition(Yorum_Adapter.yorumindeks);
        if (holder != null) {
            String yanitMetni = textt.getText().toString().trim();
            Yanit_Model yanit=new Yanit_Model("geciciid",MainActivity.kullanici.getKullaniciAdi(),yanitMetni,null,MainActivity.kullanici.getID());
           yorumm.getYanitlar().add(0,yanit);
            yorumm.setYanitYokMu(false);
            yorumAdapter.notifyItemChanged(Yorum_Adapter.yorumindeks); // görünüm güncelle
            if (!yanitMetni.isEmpty()) {
                yorumID=yorumm.getYorumID();
                DBekleYanit(ID, yorumID, yanitMetni,yanit,MainActivity.kullanici.getID());
                textt.setText(""); // Yalnızca görünür olan EditText temizlenir
                yanit.yanitMiGeldi=true;
                Yorum_Adapter.yorumindeks = -1;
            }
        }

    }
    public void DBekle(String kediId,String yorumIcerik,String YukleyenId) {
        Map<String, Object> yorumData = new HashMap<>();
        yorumData.put("icerik", yorumIcerik);
        yorumData.put("zaman", FieldValue.serverTimestamp());
        yorumData.put("kullanici_adi", MainActivity.kullanici.getKullaniciAdi()); // FirebaseAuth'tan alınabilir
        yorumData.put("Yukleyen_ID",YukleyenId);

        FirebaseFirestore.getInstance()
                .collection("cats")
                .document(kediId)
                .collection("yorumlar")
                .add(yorumData)
                .addOnSuccessListener(yanitRef ->{
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Yanıt eklenemedi", e));
    }
    public void DBekleYanit(String kediId,String yorumId,String yorumIcerik,Yanit_Model yanit,String YntyukleyenId){
        Map<String, Object> yanittData = new HashMap<>();
        yanittData.put("yaniticerik", yorumIcerik);
        yanittData.put("yanitzaman", FieldValue.serverTimestamp());
        yanittData.put("kullanici_adi", MainActivity.kullanici.getKullaniciAdi());
        yanittData.put("YanitiYukleyenID",YntyukleyenId);

        FirebaseFirestore.getInstance()
                .collection("cats")
                .document(kediId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .add(yanittData)
                .addOnSuccessListener(documentReference  ->{
                    String yanitID = documentReference.getId();
                    yanit.setYanitId(yanitID);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Yanıt eklenemedi", e));
    }


/*    private void SohbetMesajAyarlari(){
        Context con = this;
        MesajlasmaYonetici.getInstance().setSohbetButon(new SohbetButonListener() {
            @Override
            public void MesajlasmaFragmentStart() {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,new MesajFragment(con))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }*/


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(() -> {
            Thread t = new Thread(() -> {
                konumalma();
            });
            t.start();
            Thread t2=new Thread(()-> {
                konumbasma();
            });
            t2.start();
            new Thread(()->{
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                vericekme();
                if (gosterilecekKediID != null) {
                    HaritadaGor(gosterilecekKediID);
                    gosterilecekKediID = null;
                }
            }).start();

        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.getTitle().equals("konum")) {
                    sonTiklananMarker = marker;
                    kedibilgisigetirme(marker.getPosition());
                }
                return true;
            }
        });

      }

    @Override
    public void hideBottomSheet() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.hide();
        }
    }

    @Override
    public void showBottomSheet() {
        if (bottomSheetDialog != null && !bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
        }

    }

}
