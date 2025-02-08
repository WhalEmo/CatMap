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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class YuklemeArayuzuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.yukleme_arayuzu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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






    private File photoFile; // Fotoğrafın kaydedileceği dosya
    private Uri photoUri; // Fotoğrafın URI'si

    private Uri getPhotoFileUri() {
        // Benzersiz bir dosya adı oluştur
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + ".jpg";

        // Dosyanın saklanacağı dizini belirle
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Fotoğrafın saklanacağı dosyayı oluştur
        photoFile = new File(storageDir, fileName);

        // FileProvider ile güvenli bir URI oluştur
        return FileProvider.getUriForFile(this, "com.emrullah.catmap.fileprovider", photoFile);
    }


    // 📌 Kamera sonucu yakalama
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        // Çekilen fotoğrafı önizlemede göster
                        ImageView gecicifoto = findViewById(R.id.gecicifoto);
                        gecicifoto.setImageBitmap(imageBitmap);
                    }
                }
            });
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Fotoğrafı kaydedeceğimiz dosyanın URI'sini alalım
            photoUri = getPhotoFileUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            cameraLauncher.launch(takePictureIntent);
        } else {
            Log.e("CameraIntent", "Kamera uygulaması bulunamadı!");
        }
    }

    public void kameraacma(View view){
        // Kamera iznini kontrol et
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Eğer izin verilmemişse, izin iste
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            // İzin verilmişse, kamerayı aç
            openCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Eğer izin verildiyse kamerayı aç
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();  // Kamerayı aç
            } else {
                System.out.println("izin verilmedi");
            }
        }

    }
}

