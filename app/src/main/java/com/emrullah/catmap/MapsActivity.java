package com.emrullah.catmap;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
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
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.emrullah.catmap.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;


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
    }

    Marker kullanici;
    LatLng sydney;
    double latitude;
    double longitude;

    public void konumbasma() {
        runOnUiThread(() -> {
            sydney = new LatLng(37.911979, 32.499834);
            kullanici = mMap.addMarker(new MarkerOptions().position(sydney).title("konumm"));

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    // Güncel konumu al ve marker'ı güncelle
                    if (latitude != 0 && longitude != 0) {
                        sydney = new LatLng(latitude, longitude);
                        kullanici.setPosition(sydney);
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
    }

    public void vericekme() {
        db.collection("cats").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot satir : queryDocumentSnapshots) {
                double latude = satir.getDouble("latitude");
                double longtude = satir.getDouble("longitude");
                if (Math.abs(latitude - latude) <= 0.009 && Math.abs(longitude - longtude) <= 0.0113) {
                    String kedism = satir.getString("kediAdi");
                    String markerUrl=satir.getString("photoUri");
                    String hakkindaa=satir.getString("kediHakkinda");
                   // LatLng kedy = new LatLng(latude, longtude);
                    Kediler kedi=new Kediler(kedism,hakkindaa,latude,longtude,markerUrl);
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
                                String markerUrl=satir.getString("photoUri");
                                String kedism = satir.getString("kediAdi");
                                String hakkindaa=satir.getString("kediHakkinda");
                               // LatLng kedy = new LatLng(latude, longtude);
                                Kediler kedi=new Kediler(kedism,hakkindaa,latude,longtude,markerUrl);
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
                    // Placeholder yükleniyor
                }
            };

            targets.add(target); // Target'ı kaybetmiyoruz!
            Picasso.get()
                    .load(kedi.getURL())
                    .resize(100, 100)
                    .centerCrop()
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(target);
           }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Başlangıç marker'ı ekleniyor
        LatLng sydney = new LatLng(37.911979, 32.499834);
        mMap.addMarker(new MarkerOptions().position(sydney).title("BEBEGİMM"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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

      }
}
