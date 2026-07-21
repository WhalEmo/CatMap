package com.beem.catmap.Maps;

import static com.beem.catmap.ui.navigation.NavigationExtensionsKt.setupFragment;

import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
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
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.beem.catmap.BottomSheetController;
import com.beem.catmap.CevrimIciYonetimi;
import com.beem.catmap.Klavye;
import com.beem.catmap.KullaniciAuth.Kullanici;
import com.beem.catmap.MainActivity;
import com.beem.catmap.Maps.MapKedi.KediSilmeDurumu;
import com.beem.catmap.Maps.MapKedi.Kediler;
import com.beem.catmap.Maps.MapKedi.KullaniciAdiTiklamaListener;
import com.beem.catmap.Profil.Gonderiler.CacheHelperGonderiBegeni;
import com.beem.catmap.Profil.Gonderiler.GonderiDetayFragment;
import com.beem.catmap.Profil.Gonderiler.GonderiKaydetmeYardimciSinif;
import com.beem.catmap.Profil.Takipler.TakiplerFragment;
import com.beem.catmap.Profil.engellenenler.engellenenlerFragmnet;
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
import com.beem.catmap.data.repository.UserRepository;
import com.beem.catmap.mesaj.MesajFotoGosterFragment;
import com.beem.catmap.mesaj.MesajFragment;
import com.beem.catmap.models.CatModel;
import com.beem.catmap.sohbet.SohbetFragment;
import com.beem.catmap.ui.auth.AuthFragment;
import com.beem.catmap.ui.camera.CameraFragment;
import com.beem.catmap.ui.manager.CatMapToastEngine;
import com.beem.catmap.ui.manager.UiMessageManager;
import com.beem.catmap.ui.manager.UiMessageState;
import com.beem.catmap.ui.map.CatMapFragment;
import com.beem.catmap.ui.navigation.CatMapNavigationEngine;
import com.beem.catmap.ui.navigation.CatMapNavigationRenderer;
import com.beem.catmap.ui.navigation.FragmentProvider;
import com.beem.catmap.ui.navigation.NavigationHelper;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.beem.catmap.ui.upload.YuklemeArayuzuFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.beem.catmap.databinding.ActivityMapsBinding;
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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class MapsActivity extends AppCompatActivity implements BottomSheetController {

    private ActivityMapsBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
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
    private ImageView YrmgndrFotoImageView;
    private ImageView YntgndrFotoImageView;
    private URLye_Ulasma ulasma;
    private ImageView kalpImageView;
    private TextView begeniSayisiTextView;
    private ImageView GonderiEkleButton;
    private UyariMesaji mesaji;
    public Marker sonTiklananMarker;
    String gosterilecekKediID;
    private MainViewModel mViewModel;
    private FrameLayout rightSlidingPanel;
    private boolean isPanelVisible = false;
    ImageButton btnClose;
    private int screenWidth;
    private int hedefYorumIndeks = -1;
    private double sonCekilenLat = 0.0;
    private double sonCekilenLng = 0.0;

    private CatMapNavigationEngine navigationEngine;
    private CatMapNavigationRenderer navigationRenderer;

    private UserRepository userRepository;

    private final FragmentProvider fragmentProvider = new FragmentProvider() {
        @Nullable
        @Override
        public Fragment createFragment(@NonNull String tag) {
            Screen screen = Screen.Companion.fromTag(tag);
            return switch (screen) {
                case MAP -> new CatMapFragment();
                case UPLOAD -> new YuklemeArayuzuFragment();
                case OTHER_PROFILE -> setupFragment(new ProfilSayfasiFragment());
                case CAMERA -> new CameraFragment();
                case CHAT -> new SohbetFragment();
                case PROFILE -> setupFragment(new ProfilSayfasiFragment());
                case MESSAGE -> new MesajFragment();
                case BLOCKED_USERS -> new engellenenlerFragmnet();
                case FOLLOWERS -> setupFragment(new TakiplerFragment());
                case POST -> setupFragment(new GonderiDetayFragment());
                case MESSAGE_PHOTO_PREVIEW -> setupFragment(new MesajFotoGosterFragment());
                case AUTH -> setupFragment(new AuthFragment());
            };
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        userRepository = UserRepository.Companion.getInstance(getApplicationContext());

        setTheme(R.style.Theme_CatMap);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH_DETECTOR", "Uygulama fena patladı dayıcım! İşte hatan: ", throwable);

            Process.killProcess(Process.myPid());
            System.exit(10);
        });

        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.catmap_background));

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }

        // Firestore cache ayarını yap
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationEngine = new CatMapNavigationEngine(this, binding);
        navigationRenderer = new CatMapNavigationRenderer(this, R.id.fragment_container, fragmentProvider);

        SmartNavigationEngine.registerActivityCallbacks(
                () -> {
                    if (rightSlidingPanel != null && rightSlidingPanel.getTranslationX() == 0) {
                        rightSlidingPanel.animate()
                                .translationX(screenWidth)
                                .setDuration(300)
                                .start();
                        return true;
                    }
                    return false;
                },
                () -> {
                    return null;
                }
        );

        if (userRepository.isUserLoggedIn()) {
            CevrimIciYonetimi.getInstance().CevrimIciCalistir(userRepository.getCurrentUser());
            BegenileriCek();
            SmartNavigationEngine.init(navigationEngine, Screen.MAP);
        } else {
            SmartNavigationEngine.init(navigationEngine, Screen.AUTH);
        }

        uiMessageManagerObserver();

        rightSlidingPanel = findViewById(R.id.rightSlidingPanel);
        btnClose = findViewById(R.id.btnClosePanel);
        TextView tvCatFactSliding = findViewById(R.id.tvCatFactSliding);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         screenWidth = displayMetrics.widthPixels;
        AdView adView = findViewById(R.id.adView);

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

        konumizni();

        ViewGroup mainRoot = findViewById(R.id.maps_main_root);

        mesaji=new UyariMesaji(this,true);
        bottomSheetView = getLayoutInflater().inflate(R.layout.markerdaki_kediyi_gosterme, mainRoot, false);
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



        ikinci= getLayoutInflater().inflate(R.layout.yorum_gosterme,mainRoot, false);
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
        if (userRepository.isUserLoggedIn()) {
            ulasma.IDdenUrlyeUlasma(userRepository.getCurrentUserId(), YrmgndrFotoImageView);
            ulasma.IDdenUrlyeUlasma(userRepository.getCurrentUserId(), YntgndrFotoImageView);
        }

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
         /// REKLAM
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

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
            UiMessageManager.INSTANCE.emitMessage(new UiMessageState.Info("Konum izni zaten verilmiş."));
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
        if (userRepository != null && userRepository.isUserLoggedIn()) {
            Kullanici user = userRepository.getCurrentUser();
            CevrimIciYonetimi.getInstance().CevrimIciCalistir(user);
            user.setLatitude(latitude);
            user.setLongitude(longitude);
        }
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
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(userRepository.getCurrentUser());
    }
    @Override
    protected void onStop() {
        super.onStop();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(userRepository.getCurrentUser());
    }
    @Override
    protected void onPause() {
        super.onPause();
        CevrimIciYonetimi.getInstance().setHaritaEkraniGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(userRepository.getCurrentUser());
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

   private void GonderiBegenisiEkleme(String kediId){
       DocumentReference ref = db.collection("users").document(userRepository.getCurrentUserId());
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
       DocumentReference ref = db.collection("users").document(userRepository.getCurrentUserId());
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
       DocumentReference kullaniciRef = db.collection("users").document(userRepository.getCurrentUserId());
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


    ArrayList<Marker>markerlar=new ArrayList<>();

    public void tiklanan_markerdaki_kedi(String ad, String hakkindasi, Uri Url,Kediler kedi,String YukleyenId) {
        profilAlan=bottomSheetView.findViewById(R.id.profilAlani);
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
    public void kedibilgisigetirme(Kediler  kedi){
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
                        if (YId.equals(userRepository.getCurrentUserId())) {
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
                                                                    this,
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
        bottomSheetDialog.hide();
        //if (profilAlan != null) profilAlan.setVisibility(View.GONE);
        if (kediYukleyenID != null && !kediYukleyenID.isEmpty()) {
            NavigationHelper.navigateToProfile(kediYukleyenID);
        }
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
                NavigationHelper.navigateToProfile(
                        kullaniciID
                );
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
        begeniKodYoneticisi.KullanicininBegendigiYorumalar(this, userRepository.getCurrentUserId(), yorumAdapter);

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
        DBekle(ID,TEXT.getText().toString(),userRepository.getCurrentUserId());
        TEXT.setText("");
        yorumAdapter.yorumMuGeldi=true;
    }

    public void yntgonder(View view) {
        if (hedefYorumIndeks < 0 || hedefYorumIndeks >= yorumlar.size()) return;

        Yorum_Model yorumm = yorumlar.get(hedefYorumIndeks);
        String yanitMetni = textt.getText().toString().trim();

        if (!yanitMetni.isEmpty()) {
            Yanit_Model yanit = new Yanit_Model("geciciid", userRepository.getCurrentUser().getKullaniciAdi(), yanitMetni, null, userRepository.getCurrentUserId());
            yorumm.getYanitlar().add(0, yanit);
            yorumm.setYanitYokMu(false);

            yorumAdapter.notifyItemChanged(hedefYorumIndeks);

            DBekleYanit(ID, yorumm.getYorumID(), yanitMetni, yanit, userRepository.getCurrentUserId());

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
        yorumData.put("kullanici_adi", userRepository.getCurrentUser().getKullaniciAdi()); // FirebaseAuth'tan alınabilir
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
        yanittData.put("kullanici_adi", userRepository.getCurrentUser().getKullaniciAdi());
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


    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
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

                        NavigationHelper.navigateToProfile(
                                yukleyenId
                        );
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

    private void uiMessageManagerObserver() {
        UiMessageManager.INSTANCE.getMessageEvent().observe(this, message -> {
            if (message != null) {
                String msgText = "";
                int iconRes = R.drawable.ic_check;
                int strokeColor = getResources().getColor(R.color.catmap_success);
                int durationMs = 3500;

                if (message instanceof UiMessageState.Success) {
                    msgText = ((UiMessageState.Success) message).getMessage();
                    durationMs = ((UiMessageState.Success) message).getDurationMs();
                    iconRes = R.drawable.ic_check; // Başarı tık ikonu
                    strokeColor = getResources().getColor(R.color.catmap_success);

                } else if (message instanceof UiMessageState.Error) {
                    msgText = ((UiMessageState.Error) message).getMessage();
                    durationMs = 5000;
                    iconRes = R.drawable.ic_close; // Hata çarpı ikonu
                    strokeColor = getResources().getColor(R.color.catmap_error);

                } else if (message instanceof UiMessageState.Info) {
                    msgText = ((UiMessageState.Info) message).getMessage();
                    durationMs = 4000;
                    iconRes = R.drawable.ic_gallery;
                    strokeColor = getResources().getColor(R.color.catmap_text_muted);
                }

                CatMapToastEngine.show(this, msgText, iconRes, strokeColor, durationMs);

                UiMessageManager.INSTANCE.clear();
            }
        });
    }


}
