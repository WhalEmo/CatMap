package com.emrullah.catmap;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.emrullah.catmap.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetDialog bottomSheetDialog;
    private View bottomSheetView;
    private TextView isim, hakkinda;
    private ImageView imageView;
    private LocationCallback locationCallback;
    private View ikinci;
    private  BottomSheetDialog ikincibottom;
    private RecyclerView yorumlarRecyclerView;
    public static LinearLayout yorumicin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        konumizni();

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
                behavior.setSkipCollapsed(true); // İsteğe bağlı
            }
        });


        isim = bottomSheetView.findViewById(R.id.isimgosterme);
        hakkinda = bottomSheetView.findViewById(R.id.hakkindagosterme);
        imageView = bottomSheetView.findViewById(R.id.kedigosterme);

        ikinci= getLayoutInflater().inflate(R.layout.yorum_gosterme,null);
        ikincibottom=new BottomSheetDialog(this);
        ikincibottom.setContentView(ikinci);

        yorumlarRecyclerView = ikinci.findViewById(R.id.yorumlarRecyclerView);
        yorumicin=ikinci.findViewById(R.id.yorumgndrLayout);
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
        MainActivity.kullanici.setLatitude(latitude);
        MainActivity.kullanici.setLongitude(longitude);
    }

    Marker kullanici;
    LatLng sydney;
    double latitude;
    double longitude;
    private boolean konumAlindi = false;

    public void konumbasma() {
        runOnUiThread(() -> {
            sydney = new LatLng(MainActivity.kullanici.getLatitude(), MainActivity.kullanici.getLongitude());
            kullanici = mMap.addMarker(new MarkerOptions().position(sydney).title("konum"));

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    // Güncel konumu al ve marker'ı güncelle
                    if (latitude != 0 && longitude != 0) {
                        sydney = new LatLng(latitude, longitude);
                        kullanici.setPosition(sydney);
                        if(konumAlindi){
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,16f));
                            konumAlindi=false;
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

    public void vericekme() {
        db.collection("cats").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot satir : queryDocumentSnapshots) {
                double latude = satir.getDouble("latitude");
                double longtude = satir.getDouble("longitude");
                if (Math.abs(latitude - latude) <= 0.009 && Math.abs(longitude - longtude) <= 0.0113) {
                    String kediId = satir.getId();
                    String kedism = satir.getString("kediAdi");
                    String markerUrl=satir.getString("photoUri");
                    String hakkindaa=satir.getString("kediHakkinda");
                   // LatLng kedy = new LatLng(latude, longtude);
                    Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl);
                    kediler.add(kedi);
                    //mMap.addMarker(new MarkerOptions().position(kedy).title(kedism));

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
                                String markerUrl=satir.getString("photoUri");
                                String kedism = satir.getString("kediAdi");
                                String hakkindaa=satir.getString("kediHakkinda");
                               // LatLng kedy = new LatLng(latude, longtude);
                                Kediler kedi=new Kediler(kediId,kedism,hakkindaa,latude,longtude,markerUrl);
                                kediler.add(kedi);
                                //mMap.addMarker(new MarkerOptions().position(kedy).title(kedism));
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
    public void resimlimarker() {
        runOnUiThread(() -> {
        for (Kediler kedi : kediler) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        LatLng kedy = new LatLng(kedi.getLatitude(), kedi.getLongitude());
                        Bitmap customMarkerBitmap = fotoduzenle(bitmap);
                        mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                .position(kedy)
                                .title(kedi.getIsim()));
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


    Map<String, Bitmap> fotoCache = new HashMap<>();
    List<Target> targetListesi = new ArrayList<>();
    public void tiklanan_markerdaki_kedi(String ad, String hakkindasi, String Url) {
        isim.setText(ad);
        hakkinda.setText(hakkindasi);

        // Önbellekten kontrol et
        if (fotoCache.containsKey(Url)) {
            imageView.setImageBitmap(fotoCache.get(Url));
            if (!bottomSheetDialog.isShowing()) {
                bottomSheetDialog.show();
            }
            return;
        }

        // Yoksa yükle ve önbelleğe kaydet
        Target t = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                fotoCache.put(Url, bitmap); // Önbelleğe ekle
                imageView.setImageBitmap(bitmap);
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
        Picasso.get().load(Url).into(t);
    }
    String ID;
    public static String kediID;
    String yorumID;
    public void kedibilgisigetirme(LatLng markerPosition){
        for(Kediler kedi:kediler){
            if(kedi.getLatitude()==markerPosition.latitude&&kedi.getLongitude()==markerPosition.longitude) {
                ID=kedi.getID();
                kediID=ID;
                tiklanan_markerdaki_kedi(kedi.getIsim(), kedi.getHakkindasi(), kedi.getURL());
            }
        }
    }
    ArrayList<Yorum_Model>yorumlar=new ArrayList<>();
    Yorum_Adapter yorumAdapter;


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
                            Date zaman=doc.getDate("zaman");
                            Yorum_Model yorum=new Yorum_Model(IDsi,kAdi,icerik,zaman,null);
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
                    }
                    isLoading = false;
                });
    }

    public void patiyorumyap(View view){
        if (yorumListener != null) {
            yorumListener.remove();  // Önceki listener varsa kaldır
        }
        yorumlar.clear();
        yorumAdapter = new Yorum_Adapter(yorumlar, this);
        yorumlarRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        yorumlarRecyclerView.setAdapter(yorumAdapter);
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
                            Date zaman=doc.getDate("zaman");
                            Yorum_Model yorum=new Yorum_Model(IDsi,kAdi,icerik,zaman,null);
                            switch (dc.getType()){
                                case ADDED:
                                    yorumlar.add(0,yorum);
                                    yorumAdapter.notifyItemInserted(0);
                                    break;
                                case REMOVED:
                                    for(int i=0;i<yorumlar.size();i++){
                                        if(yorumlar.get(i).getYorumID().equals(IDsi)){
                                            yorumlar.remove(i);
                                            yorumAdapter.notifyItemRemoved(i);
                                            break;
                                        }
                                    }
                                    break;
                            }
                        }
                        lastVisible = snapshots.getDocuments().get(snapshots.size() - 1);

                        // Eğer gelen veri PAGE_SIZE'dan küçükse son sayfa olabilir
                        if (snapshots.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }
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

    EditText TEXT;
    public void yorumgonder(View view){
        TEXT=ikinci.findViewById(R.id.yorumEditText);
        DBekle(ID,TEXT.getText().toString());
        TEXT.setText("");
    }
    EditText textt;

    public void yanitgonder(View view){
        Yorum_Model yorumm=yorumlar.get(Yorum_Adapter.yorumindeks);
        RecyclerView.ViewHolder holder = yorumlarRecyclerView.findViewHolderForAdapterPosition(Yorum_Adapter.yorumindeks);
        if (holder != null) {
             textt = holder.itemView.findViewById(R.id.yanitEditText);
            String yanitMetni = textt.getText().toString().trim();
            if (!yanitMetni.isEmpty()) {
                yorumID=yorumm.getYorumID();
                DBekleYanit(ID, yorumID, yanitMetni);
                textt.setText(""); // Yalnızca görünür olan EditText temizlenir
            }
        }

    }
    public void DBekle(String kediId,String yorumIcerik) {
        Map<String, Object> yanitData = new HashMap<>();
        yanitData.put("icerik", yorumIcerik);
        yanitData.put("zaman", FieldValue.serverTimestamp());
        yanitData.put("kullanici_adi", MainActivity.kullanici.getKullaniciAdi()); // FirebaseAuth'tan alınabilir

        FirebaseFirestore.getInstance()
                .collection("cats")
                .document(kediId)
                .collection("yorumlar")
                .add(yanitData)
                .addOnSuccessListener(yanitRef -> Log.d("Firestore", "Yanıt eklendi: " + yanitRef.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Yanıt eklenemedi", e));
    }
    public void DBekleYanit(String kediId,String yorumId,String yorumIcerik){
        Map<String, Object> yanittData = new HashMap<>();
        yanittData.put("yaniticerik", yorumIcerik);
        yanittData.put("yanitzaman", FieldValue.serverTimestamp());
        yanittData.put("kullanici_adi", MainActivity.kullanici.getKullaniciAdi());

        FirebaseFirestore.getInstance()
                .collection("cats")
                .document(kediId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .add(yanittData)
                .addOnSuccessListener(yanit -> Log.d("Firestore", "Yanıt eklendi: " + yanit.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Yanıt eklenemedi", e));
    }


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
            }).start();

        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.getTitle().equals("konum")) {
                    kedibilgisigetirme(marker.getPosition());
                }
                return true;
            }
        });

      }
}
