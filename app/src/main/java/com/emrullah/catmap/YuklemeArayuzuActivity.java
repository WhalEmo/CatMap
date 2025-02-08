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

public class YuklemeArayuzuActivity extends AppCompatActivity {

    private Uri photoUri;
    private File photoFile;
    private ImageView gecicifoto;
    private EditText kedininismi;
    private EditText kedininhakkindasi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yukleme_arayuzu);
        gecicifoto = findViewById(R.id.gecicifoto);
        kedininismi=findViewById(R.id.isimText);
        kedininhakkindasi=findViewById(R.id.hakkındaText);
    }

    // Galeriye gitmek için ActivityResultContracts kullanalım
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();  // Seçilen fotoğrafın URI'si

                        // ImageView'da fotoğrafı göstermek
                        ImageView gecicifoto = findViewById(R.id.gecicifoto);
                        gecicifoto.setImageURI(selectedImageUri);  // Fotoğrafı önizleme alanında gösteriyoruz
                    }
                }
            });

    // Galeriye gitmek için buton

    public void yuklemebasma(View view) {
        // Galeriye gitmek için Intent başlatıyoruz
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);  // Intent'i ActivityResultLauncher ile başlatıyoruz
    }


    // 📌 Kamera sonucu yakalama
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            photoFile = File.createTempFile(fileName, ".jpg", storageDir);
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
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
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
    }

    //butona basınca kaydetme
    public void kaydet(View view){
        String kediadi=kedininismi.getText().toString().trim();
        String kedihakkinda=kedininhakkindasi.getText().toString().trim();
        if(kediadi.isEmpty()){
            Toast toast = Toast.makeText(this, "Lütfen kedi ismini giriniz!", Toast.LENGTH_SHORT);
            toast.show();
        }
        if(kedihakkinda.isEmpty()){
            Toast toast = Toast.makeText(this, "Lütfen kedi hakkindasi giriniz!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
