package com.beem.catmap.Maps;

import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

import com.beem.catmap.BottomSheetController;
import com.beem.catmap.CevrimIciYonetimi;
import com.beem.catmap.Klavye;
import com.beem.catmap.MainActivity;
import com.beem.catmap.Maps.MapKedi.KediSilmeDurumu;
import com.beem.catmap.Maps.MapKedi.Kediler;
import com.beem.catmap.Maps.MapKedi.KullaniciAdiTiklamaListener;
import com.beem.catmap.Profil.Gonderiler.CacheHelperGonderiBegeni;
import com.beem.catmap.Profil.Gonderiler.GonderiKaydetmeYardimciSinif;
import com.beem.catmap.R;
import com.beem.catmap.URLye_Ulasma;
import com.beem.catmap.UyariMesaji;
import com.beem.catmap.YorumYanit.Begeni_Kod_Yoneticisi_Yorum;
import com.beem.catmap.YorumYanit.CacheHelperYorum;
import com.beem.catmap.YorumYanit.Yanit_Adapter;
import com.beem.catmap.YorumYanit.Yanit_Model;
import com.beem.catmap.YorumYanit.Yorum_Adapter;
import com.beem.catmap.YorumYanit.Yorum_Model;
import com.beem.catmap.Profil.MainViewModel;
import com.beem.catmap.Profil.ProfilSayfasiFragment;
import com.beem.catmap.mesaj.MesajFragment;
import com.beem.catmap.models.CatModel;
import com.beem.catmap.sohbet.SohbetFragment;
import com.beem.catmap.ui.manager.UiMessageManager;
import com.beem.catmap.ui.manager.UiMessageState;
import com.beem.catmap.ui.upload.YuklemeArayuzuFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.beem.catmap.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback , BottomSheetController {

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
    public LinearLayout yorumicin;
    public LinearLayout ynticin;
    public LinearLayout carpiicin;
    private RelativeLayout yuklemeEkrani;
    private TextView bosyorum;
    private ImageButton iptalButton;
    public EditText kimeyanit;
    public EditText textt;
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
    private ImageView btnShowFact;
    private FrameLayout rightSlidingPanel;
    private boolean isPanelVisible = false;
    ImageButton btnClose;
    private int screenWidth;
    private BottomNavigationView bottom_navigation;
    private MapViewModel mapViewModel;
    private int hedefYorumIndeks = -1;
    private double sonCekilenLat = 0.0;
    private double sonCekilenLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.catmap_background));

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }


        System.out.println(MainActivity.kullanici.getID());
        // Firestore cache ayarını yap
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        observeViewModel();
        uiMessageManagerObserver();

        FloatingActionButton fabCurrentLocation = findViewById(R.id.fabCurrentLocation);

        fabCurrentLocation.setOnClickListener(v -> {
            if (mMap != null && sonCekilenLat != 0.0 && sonCekilenLng != 0.0) {
                LatLng currentLatLng = new LatLng(sonCekilenLat, sonCekilenLng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));

            } else {
                Toast.makeText(MapsActivity.this, "Konum aranıyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
            }
        });

        bottom_navigation=findViewById(R.id.bottom_navigation);
        yuklemeEkrani = findViewById(R.id.yuklemeekran);
        btnShowFact=findViewById(R.id.btnShowFact);
        btnClose = findViewById(R.id.btnClosePanel);
        rightSlidingPanel = findViewById(R.id.rightSlidingPanel);
        TextView tvCatFactSliding = findViewById(R.id.tvCatFactSliding);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         screenWidth = displayMetrics.widthPixels;
        AdView adView = findViewById(R.id.adView);

        btnShowFact.setOnClickListener(v -> {
            if (!isPanelVisible) {
                CatFactService.getRandomCatFact(this, new CatFactService.CatFactCallback() {
                    @Override
                    public void onSuccess(String translatedFact) {
                        tvCatFactSliding.setText(translatedFact);
                        AdRequest adRequest = new AdRequest.Builder().build();
                        adView.loadAd(adRequest);
                        showPanel();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        tvCatFactSliding.setText("Hata: " + errorMessage);
                        showPanel();
                    }
                });
            } else {
                hidePanel(screenWidth);
            }
        });
        btnClose.setOnClickListener(v -> {
            rightSlidingPanel.animate()
                    .translationX(screenWidth) // dışarı kaydır
                    .setDuration(300)
                    .withEndAction(() -> {
                        tvCatFactSliding.setText("");
                        isPanelVisible = false;
                    })
                    .start();
        });
        anaGorunum=findViewById(R.id.anaGorunum);

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

        altCubuk();
        binding.bottomNavigation.setSelectedItemId(R.id.haritagit);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            merkeziBackStackChangedListener(null);
        });

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

        ikincibottom.setOnShowListener(dialog -> {
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
        if (gosterilecekKediID != null) {
            HaritadaGor(gosterilecekKediID);
        }
         /// REKLAM
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
        else if(rightSlidingPanel.getTranslationX() == 0){
            rightSlidingPanel.animate()
                    .translationX(screenWidth)
                    .setDuration(300)
                    .start();
        }
        else {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
            if (binding.bottomNavigation.getSelectedItemId() != R.id.haritagit) {
                binding.bottomNavigation.setSelectedItemId(R.id.haritagit);
            } else {
                CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi();
                super.onBackPressed();
            }
        }
    }


    private void merkeziBackStackChangedListener(Fragment paramFragment){
        Fragment currentFragment;
        if(paramFragment == null){
            currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }
        else{
            currentFragment = paramFragment;
        }
            if (currentFragment == null) return;

            String tag = currentFragment.getTag();
            System.out.println("tag: " + tag);
            boolean menuGozukmeli = Arrays.asList("MAP_FRAGMENT_TAG", "PROFILE", "CHAT", "YUKLE").contains(tag);
            binding.bottomNavigation.setVisibility(menuGozukmeli ? View.VISIBLE : View.GONE);

            binding.mapOverlayContainer.setVisibility("MAP_FRAGMENT_TAG".equals(tag) ? View.VISIBLE : View.GONE);

            if (!(currentFragment instanceof ProfilSayfasiFragment)) {
                if (profilAlan != null) profilAlan.setVisibility(View.VISIBLE);
                if (anaGorunum != null) anaGorunum.setVisibility(View.VISIBLE);

                if (bottomSheetDialog != null && !bottomSheetDialog.isShowing() && isBackPressed) {
                    bottomSheetDialog.show();
                }
            } else {
                if (profilAlan != null) profilAlan.setVisibility(View.VISIBLE);
                if (anaGorunum != null) anaGorunum.setVisibility(View.VISIBLE);
            }
    }

    public void sonTiklananMarkeriSil() {
        if (sonTiklananMarker != null) {
            sonTiklananMarker.remove();
            markerlar.remove(sonTiklananMarker);
            sonTiklananMarker = null;
        }
    }
    private void showPanel() {
        rightSlidingPanel.animate()
                .translationX(0)
                .setDuration(300)
                .start();
        isPanelVisible = true;
    }

    // Paneli gizle (animasyonlu)
    private void hidePanel(int screenWidth) {
        rightSlidingPanel.animate()
                .translationX(screenWidth)
                .setDuration(300)
                .start();
        isPanelVisible = false;
    }

    private void konumizni() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        } else {
            Toast.makeText(this, "Konum izni zaten verilmiş.", Toast.LENGTH_SHORT).show();
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
                finish();
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
        LocationEngine.INSTANCE.stopTracking();

    }

    @Override
    protected void onResume() {
        super.onResume();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(true);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(MainActivity.kullanici);
            if (KediSilmeDurumu.getInstance().isSilindiMi()) {
                vericekme();
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



    private int dpDenPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    private BitmapDescriptor Duzenleme(int dp, Bitmap foto){
        int genislik = dpDenPx(dp);
        int yukseklik = dpDenPx(dp);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(foto, genislik, yukseklik, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }
    double latitude;
    double longitude;
    private boolean konumAlindi = false;

    public void konumbasma() {
        runOnUiThread(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    // Güncel konumu al ve marker'ı güncelle
                    if (latitude != 0 && longitude != 0) {
                        if(konumAlindi){
                            new Handler().postDelayed(() -> {
                                yuklemeEkrani.setVisibility(View.GONE);
                                bottom_navigation.setVisibility(View.VISIBLE);
                                btnShowFact.setVisibility(View.VISIBLE);
                            }, 500);
                            konumAlindi=false;
                            TarananKediler tarama =  new TarananKediler();
                            tarama.ButonGosterim(mMap,findViewById(android.R.id.content));
                            tarama.Basildi(kediler,mMap,()->{
                                resimlimarker();
                            },MapsActivity.this);
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

    private void konumalma(){
        LocationRequest konumIstegi = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder konumAyarlariIstegi = new LocationSettingsRequest.Builder()
                .addAllLocationRequests(Collections.singleton(konumIstegi));

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(konumAyarlariIstegi.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            konumalmaBaslat(konumIstegi);
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, 2001);
                    finish();
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            }
        });
    }

    public void konumalmaBaslat(LocationRequest locationRequest) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

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
        System.out.println("kedi: "+ kediid);
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
                            System.out.println("ife girdi*");
                        }else {
                            System.out.println("elsee girdi*");
                            if (mMap != null) {
                                System.out.println("ife girdi**");
                                Kediler kedi=new Kediler(kediId,isim,hakkindaa,latitudee,longitudee,fotoUrl.get(0),fotoUrl,YukleyenID);
                                kediler.add(kedi);
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(konum, 16f));
                                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                    System.out.println("ife girdi3");
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

       CircleImageView markerImage = markerView.findViewById(R.id.marker_cat_image); // Eğer yuvarlak istiyorsan
       markerImage.setImageBitmap(imageBitmap);

       markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
       markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

       Bitmap returnedBitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
       Canvas canvas = new Canvas(returnedBitmap);
       markerView.draw(canvas);
       return returnedBitmap;
   }

   ArrayList<Kediler>kediler=new ArrayList<>();
    List<Target> targets = new ArrayList<>();
    ArrayList<Marker>markerlar=new ArrayList<>();
    HashMap<String, Object> markerKEY = new HashMap<>();
    public void resimlimarker() {
        runOnUiThread(() -> {
            for (Kediler kedi : kediler) {
                if (markerKEY.containsKey(kedi.getURL()) || kedi.isMarkerOlustuMu()) {
                    continue;
                }
                kedi.setMarkerOlustuMu(true);
                markerKEY.put(kedi.getURL(), null);
                Glide.with(MapsActivity.this)
                        .asBitmap()
                        .load(kedi.getURL())
                        .override(100, 100)
                        .centerCrop()
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                LatLng kedy = new LatLng(kedi.getLatitude(), kedi.getLongitude());
                                Bitmap customMarkerBitmap = fotoduzenle(resource);

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                        .position(kedy)
                                        .title(kedi.getIsim()));

                                markerlar.add(marker);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                Log.e("GLIDE", "Fotoğraf yüklenemedi: " + kedi.getURL());
                                kedi.setMarkerOlustuMu(false);
                                markerKEY.remove(kedi.getURL());
                            }
                        });
            }
        });
    }

    private boolean isBackPressed = false;
    public void tiklanan_markerdaki_kedi(String ad, String hakkindasi, Uri Url,Kediler kedi,String YukleyenId) {
        profilAlan=bottomSheetView.findViewById(R.id.profilAlani);
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
                .replace(R.id.fragment_container, fragment)
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
        yorumAksiyonListener();
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
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();

            }
        });

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

    public void yntgonder(View view) {
        if (hedefYorumIndeks < 0 || hedefYorumIndeks >= yorumlar.size()) return;

        Yorum_Model yorumm = yorumlar.get(hedefYorumIndeks);
        String yanitMetni = textt.getText().toString().trim();

        if (!yanitMetni.isEmpty()) {
            Yanit_Model yanit = new Yanit_Model("geciciid", MainActivity.kullanici.getKullaniciAdi(), yanitMetni, null, MainActivity.kullanici.getID());
            yorumm.getYanitlar().add(0, yanit);
            yorumm.setYanitYokMu(false);

            yorumAdapter.notifyItemChanged(hedefYorumIndeks);

            DBekleYanit(ID, yorumm.getYorumID(), yanitMetni, yanit, MainActivity.kullanici.getID());

            textt.setText("");
            hedefYorumIndeks = -1;

            carpiicin.setVisibility(View.GONE);
            ynticin.setVisibility(View.GONE);
            yorumicin.setVisibility(View.VISIBLE);

            new Klavye(this).klavyeKapat(textt);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        btnShowFact.setVisibility(View.VISIBLE);
        markerKEY.clear();
        markerlar.clear();
        mMap = googleMap;
        LocationEngine.INSTANCE.startTracking(this, mMap);
        mMap.setOnMapLoadedCallback(() -> {
            if (gosterilecekKediID != null) {
                HaritadaGor(gosterilecekKediID);
                gosterilecekKediID = null;
            }
        });
        TarananKediler tarama = new TarananKediler();
        tarama.ButonGosterim(mMap, findViewById(android.R.id.content));
        mMap.setOnCameraIdleListener(() -> {
            tarama.ButonGosterim(mMap, findViewById(android.R.id.content));
        });
        tarama.Basildi(kediler, mMap, () -> {
            resimlimarker();
        }, MapsActivity.this);

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



    private void KlavyeAyari(View rootView, LinearLayout YorumLinearLayout){
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()); // Klavye boyutu
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Sistem barları
            boolean klavyeAcik = imeInsets.bottom > 0;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) YorumLinearLayout.getLayoutParams();

            int klavyeYuksekligi = imeInsets.bottom;
            int sistemCubuğuYuksekligi = navInsets.bottom;

            int netYukseklik = klavyeYuksekligi - sistemCubuğuYuksekligi;
            if (netYukseklik < 0) netYukseklik = 0;

            params.bottomMargin = klavyeAcik ? netYukseklik + dpToPx(4) : dpToPx(8);
            YorumLinearLayout.setLayoutParams(params);
            return insets;
        });

    }

    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private void altCubuk() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction transaction = fm.beginTransaction();
            String TAG = "";
            if (id == R.id.haritagit) {
                if (btnShowFact != null) btnShowFact.setVisibility(View.VISIBLE);
                TAG = "MAP_FRAGMENT_TAG";
            } else if (id == R.id.profilim) {
                if (btnShowFact != null) btnShowFact.setVisibility(View.GONE);
                TAG = "PROFILE";
            } else if (id == R.id.sohbet) {
                if (btnShowFact != null) btnShowFact.setVisibility(View.GONE);
                TAG = "CHAT";
            } else if (id == R.id.yuklekedi) {
                if (btnShowFact != null) btnShowFact.setVisibility(View.GONE);
                TAG = "YUKLE";
            }

            for (Fragment f : fm.getFragments()) {
                if (f != null) {
                    if (f.getTag() == null) {
                        transaction.remove(f);
                    } else if (!f.getTag().equals(TAG)) {
                        transaction.hide(f);
                    }
                }
            }

            Fragment targetFragment = fm.findFragmentByTag(TAG);

            if (targetFragment == null) {
                if (id == R.id.haritagit) {
                    targetFragment = new SupportMapFragment();
                    ((SupportMapFragment) targetFragment).getMapAsync(this);
                } else if (id == R.id.profilim) {
                    targetFragment = ProfilSayfasiFragment.newInstance(MainActivity.kullanici.getID());
                } else if (id == R.id.sohbet) {
                    targetFragment = new SohbetFragment(() -> {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new MesajFragment(this))
                                .addToBackStack(null)
                                .commit();
                    });
                } else if (id == R.id.yuklekedi) {
                    targetFragment = new YuklemeArayuzuFragment();
                }
                if (targetFragment != null) {
                    transaction.add(R.id.fragment_container, targetFragment, TAG);
                }
            } else {
                transaction.show(targetFragment);
            }
            transaction.commit();

            merkeziBackStackChangedListener(targetFragment);
            return true;
        });
    }


    public void setSelectedItemSpeacial(int position){
        binding.bottomNavigation.setSelectedItemId(position);
    }


    private void observeViewModel() {
        mapViewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                yuklemeEkrani.setVisibility(View.VISIBLE);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    yuklemeEkrani.setVisibility(View.GONE);
                    bottom_navigation.setVisibility(View.VISIBLE);
                    if (btnShowFact != null) btnShowFact.setVisibility(View.VISIBLE);
                }, 500);
            }
        });

        mapViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("MVVM_HATA", errorMessage);
            }
        });

        mapViewModel.getCatsList().observe(this, catModels -> {
            if (catModels != null && !catModels.isEmpty()) {
                kediler.clear();
                for (CatModel model : catModels) {
                    Kediler kedi = new Kediler(
                            model.getId(),
                            model.getKediAdi(),
                            model.getKediHakkinda(),
                            model.getLatitude(),
                            model.getLongitude(),
                            model.getMainPhotoUrl(),
                            new ArrayList<>(model.getPhotoUri()),
                            model.getYukleyenKullaniciID()
                    );
                    kediler.add(kedi);
                }

                resimlimarker();
            }
        });

        LocationEngine.INSTANCE.getFetchDataEvent().observe(this, event -> {
            if(event != null && mapViewModel != null){
                mapViewModel.fetchCatsNearLocation(event.latitude, event.longitude);
                sonCekilenLat = event.latitude;
                sonCekilenLng = event.longitude;
            }
        });
    }


    private void yorumAksiyonListener(){
        yorumAdapter.setAksiyonListener((kullaniciAdi, yukleyenId, position, ayniButonaMiBasildi) -> {
            if (carpiicin == null || ynticin == null || yorumicin == null || textt == null || kimeyanit == null) return;

            if (ayniButonaMiBasildi) {
                hedefYorumIndeks = -1;

                yorumicin.setVisibility(View.VISIBLE);
                carpiicin.setVisibility(View.GONE);
                ynticin.setVisibility(View.GONE);

                // Klavyeyi kapat
                Klavye klavye = new Klavye(MapsActivity.this);
                klavye.klavyeKapat(textt);

            } else {
                hedefYorumIndeks = position;

                String metin = "@" + kullaniciAdi + " ";
                SpannableString spannableString = new SpannableString(metin);

                // Mavi Renk
                spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 0, metin.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Tıklanabilirlik (Kullanıcı profiline gitme)
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        // Activity içinde olduğumuz için fragment geçişini direkt yapabiliriz
                        if (bottomSheetDialog != null) bottomSheetDialog.hide();
                        if (ikincibottom != null) ikincibottom.hide();

                        ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(yukleyenId);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.BLUE);
                        ds.setUnderlineText(false);
                    }
                };
                spannableString.setSpan(clickableSpan, 0, metin.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                textt.setText(spannableString);
                textt.setMovementMethod(LinkMovementMethod.getInstance());
                textt.setSelection(textt.getText().length());
                kimeyanit.setHint(kullaniciAdi + " 'e yanıt veriyorsun");

                yorumicin.setVisibility(View.GONE);
                carpiicin.setVisibility(View.VISIBLE);
                ynticin.setVisibility(View.VISIBLE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Klavye klavye = new Klavye(MapsActivity.this);
                    klavye.klavyeAc(textt);
                }, 250);
            }
        });
    }

    private void uiMessageManagerObserver(){
        UiMessageManager.INSTANCE.getMessageEvent().observe(this, message -> {
            if (message != null) {
                if (message instanceof UiMessageState.Success) {
                    String msg = ((UiMessageState.Success) message).getMessage();
                    int duration = ((UiMessageState.Success) message).getDurationMs();
                    // TODO: Yarın bir gün buraya senin "mesaji.BasariliDurum(msg, duration)" yapın entegre edilecek!
                    android.widget.Toast.makeText(this, "[BAŞARI] " + msg, android.widget.Toast.LENGTH_SHORT).show();

                } else if (message instanceof com.beem.catmap.ui.manager.UiMessageState.Error) {
                    String msg = ((com.beem.catmap.ui.manager.UiMessageState.Error) message).getMessage();
                    // TODO: Buraya özel kırmızı premium hata barı gelecek!
                    android.widget.Toast.makeText(this, "[HATA] " + msg, android.widget.Toast.LENGTH_SHORT).show();

                } else if (message instanceof com.beem.catmap.ui.manager.UiMessageState.Info) {
                    String msg = ((com.beem.catmap.ui.manager.UiMessageState.Info) message).getMessage();
                    // TODO: Buraya "Yükleniyor..." dönen şık loading barı gelecek!
                    android.widget.Toast.makeText(this, "[BİLGİ] " + msg, android.widget.Toast.LENGTH_SHORT).show();
                }

                // 🎯 İşlem bittiğinde mesajı sıfırlıyoruz ki cihaz dönünce tekrar tetiklenmesin
                com.beem.catmap.ui.manager.UiMessageManager.INSTANCE.clear();
            }
        });
    }


}
