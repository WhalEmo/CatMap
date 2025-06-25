package com.emrullah.catmap;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.Manifest;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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

public class YuklemeArayuzuActivity extends AppCompatActivity {

    private Uri photoUri;
    private File photoFile;
    private EditText kedininismi;
    private EditText kedininhakkindasi;
    private FusedLocationProviderClient konumsaglayici;
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

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yukleme_arayuzu);
        geciciFoto = findViewById(R.id.geciciFoto);
        fotoPager = findViewById(R.id.fotoPager);
        fotoAdapter = new FotoGeciciAdapter(this, secilenFotolar,null);
        fotoPager.setAdapter(fotoAdapter);

        kedininismi=findViewById(R.id.isimText);
        kedininhakkindasi=findViewById(R.id.hakkindaText);
        // FusedLocationProviderClient başlat
        konumsaglayici = LocationServices.getFusedLocationProviderClient(this);
        // Firestore Başlat
        db = FirebaseFirestore.getInstance();
        mesaji = new UyariMesaji(this,false);

    }

    // Galeriye gitmek ve secmek için ActivityResultContracts kullanalım
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                    if (result.getData().getClipData() != null) {
                        ClipData clipData = result.getData().getClipData();
                        if(clipData.getItemCount() + secilenFotolar.size() > MAX_FOTO_SAYISI){
                            mesaji.BasarisizDurum("En fazla " + MAX_FOTO_SAYISI +" fotoğraf seçebilirsiniz!",1000);
                            return;
                        }
                        if(geciciFoto.getVisibility() != View.GONE){
                            geciciFoto.setVisibility(View.GONE);
                        }
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            secilenFotolar.add(uri);
                        }
                        fotoAdapter.notifyDataSetChanged();
                    } else if (result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        secilenFotolar.add(uri);
                        fotoAdapter.notifyDataSetChanged();
                    }
                }
            });


    // Galeriye gitmek için buton

    public void yuklemebasma(View view) {
        // Galeriye gitmek için Intent başlatıyoruz
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // çoklu seçim
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }


    // 📌 Kameraya gotur ve sonucu bas
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        if (photoUri != null) {
                            if(secilenFotolar.size()>=MAX_FOTO_SAYISI){
                                mesaji.BasarisizDurum("En fazla " + MAX_FOTO_SAYISI +" fotoğraf yükleyebilirsiniz!",1000);
                                return;
                            }
                            if(geciciFoto.getVisibility()!=View.GONE){
                                geciciFoto.setVisibility(View.GONE);
                            }
                            secilenFotolar.add(photoUri);
                            fotoAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });

    // 📌 Fotoğraf dosyası oluşturma
    private Uri getPhotoFileUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//cekilen footgrafın ne zaman cekildigini gosterir
        String fileName = "JPEG_" + timeStamp + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);//klasor yolunu tutuyor. klasore gitme yolunu bilen bi nesne
        //filepaths xml otomatik klasor olusturdu
        try {
            photoFile = File.createTempFile(fileName, ".jpg", storageDir);// dosya olustu
            return FileProvider.getUriForFile(this, "com.emrullah.catmap.fileprovider", photoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 📌 Kamerayı açma metodu
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoUri = getPhotoFileUri();
            if (photoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);//put extra kamera aktivitesinde cektigimiz fotoyu
                //yukleme aktivitisine gonderir
                cameraLauncher.launch(takePictureIntent);
            } else {
                Log.e("CameraError", "Dosya oluşturulamadı!");
            }
        } else {
            Log.e("CameraIntent", "Kamera uygulaması bulunamadı!");
        }
    }

    // 📌 Kamera butonuna tıklanınca çalışacak
    public void kameraacma(View view) {
       // Uygulamanın kamera iznine sahip olup olmadığını kontrol eder.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            openCamera();
        }
    }

    // 📌 İzin sonucu işleme
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
        if(requestCode == 102&&grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            getUserLocation();
        }
    }

    // 📌 Kullanıcının konumunu al
    private void getUserLocation() {
        //  kullanıcıdan izin iste
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
            return;
        }

        // 📍 Son bilinen konumu al
        konumsaglayici.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
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
            catData.put("kediAdi", kediadi);
            catData.put("kediHakkinda", kedihakkinda);
            catData.put("latitude", latitude);
            catData.put("longitude", longitude);
            catData.put("photoUri", fotoUrl);
            db.collection("cats")
                    .add(catData)
                    .addOnSuccessListener(documentReference -> {
                        mesaji.BasariliDurum("Kedi bilgileri başarıyla kaydedildi!",1000);
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


    //butona basınca kaydetme
    public void kaydet(View view) {
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


}
