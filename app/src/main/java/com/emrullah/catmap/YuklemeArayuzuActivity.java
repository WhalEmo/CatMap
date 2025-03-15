package com.emrullah.catmap;

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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class YuklemeArayuzuActivity extends AppCompatActivity {

    private Uri photoUri;
    private File photoFile;
    private ImageView gecicifoto;
    private EditText kedininismi;
    private EditText kedininhakkindasi;
    private FusedLocationProviderClient konumsaglayici;
    private FirebaseFirestore db;
    double latitude=0;
    double longitude=0;
    String kediadi;
    String kedihakkinda;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yukleme_arayuzu);
        gecicifoto = findViewById(R.id.gecicifoto);
        kedininismi=findViewById(R.id.isimText);
        kedininhakkindasi=findViewById(R.id.hakkındaText);
        // FusedLocationProviderClient başlat
        konumsaglayici = LocationServices.getFusedLocationProviderClient(this);
        // Firestore Başlat
        db = FirebaseFirestore.getInstance();
    }

    // Galeriye gitmek ve secmek için ActivityResultContracts kullanalım
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        photoUri  = result.getData().getData();  // Seçilen fotoğrafın URI'si
                        // ImageView'da fotoğrafı göstermek
                        ImageView gecicifoto = findViewById(R.id.gecicifoto);
                        gecicifoto.setImageURI(photoUri );  // Fotoğrafı önizleme alanında gösteriyoruz
                    }
                }
            });

    // Galeriye gitmek için buton

    public void yuklemebasma(View view) {
        // Galeriye gitmek için Intent başlatıyoruz
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);  // Intent'i ActivityResultLauncher ile başlatıyoruz
    }


    // 📌 Kameraya gotur ve sonucu bas
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        if (photoUri != null) {
                            // 📸 Çekilen yüksek kaliteli fotoğrafı ImageView'da göster
                            gecicifoto.setImageURI(photoUri);
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
        System.out.println("İFİN USTUNE GİRDİİİİİ");
        if(requestCode == 102&&grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            getUserLocation();
            System.out.println("İFİN İCİNEE GİRDİİİİİ");
        }
    }

    // 📌 Kullanıcının konumunu al
    private void getUserLocation() {
        //  kullanıcıdan izin iste
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
           // System.out.println("1 kere yazması lazımm ifffinn iciii");
            return;
        }
        System.out.println("1 kere yazması lazımm");

        // 📍 Son bilinen konumu al
        konumsaglayici.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                     latitude = location.getLatitude();  // Enlem
                     longitude = location.getLongitude(); // Boylam
                    veritabanikaydi();

                    // 📌 Kullanıcıya Toast mesajı göster
                    System.out.println( "Konum: " + latitude + ", " + longitude);
                } else {
                    System.out.println( "Konum alınamadı!");
                }
            }
        });

    }


    public void veritabanikaydi(){
        if (latitude == 0 && longitude == 0) {
            Toast toast = Toast.makeText(this, "Lütfen kedinin konumunu giriniz!", Toast.LENGTH_SHORT);
            toast.show();
            //System.out.println("konum ifi ici");
        } else {

            // Firestore'a kaydedilecek veri yapısı
            Map<String, Object> catData = new HashMap<>();
            catData.put("kediAdi", kediadi);
            catData.put("kediHakkinda", kedihakkinda);
            catData.put("latitude", latitude);
            catData.put("longitude", longitude);
            catData.put("photoUri", photoUri.toString());

            // Firestore'a veri gönder
            db.collection("cats")
                    .add(catData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Kedi bilgileri başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                        kediadi=null;
                        kedininismi.getText().clear();
                        kedihakkinda=null;
                        kedininhakkindasi.getText().clear();
                        photoUri=null;
                        gecicifoto.setImageResource(R.drawable.yuklemefotosu);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Veri kaydedilirken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

    }

    //butona basınca kaydetme
    public void kaydet(View view) {
        //anlık cekilmedityse yani dosyadan secildiyse adres girsin
         kediadi = kedininismi.getText().toString().trim();
         kedihakkinda = kedininhakkindasi.getText().toString().trim();
        if (kediadi.isEmpty()) {
            Toast toast = Toast.makeText(this, "Lütfen kedi ismini giriniz!", Toast.LENGTH_SHORT);
            toast.show();
        }
        if (photoUri == null) {
            Toast toast = Toast.makeText(this, "Lütfen kedinin fotoğrafını yükleyiniz!", Toast.LENGTH_SHORT);
            toast.show();
        }
        if ( !kediadi.isEmpty() && photoUri != null) {
            getUserLocation();
        }
       // System.out.println("konum ifi ici");
    }

}
