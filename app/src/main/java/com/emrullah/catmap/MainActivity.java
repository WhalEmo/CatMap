package com.emrullah.catmap;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText KullanicAdi;
    private EditText sifre;
    private LayoutInflater inf;
    private View pencere;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void yuklemeSayfasi(View view){
        System.out.println("Yukleme Sayfasi gecildi");
        Intent intent = new Intent(MainActivity.this, YuklemeArayuzuActivity.class);
        startActivity(intent);
    }
    public  void haritaSayfasi(View view){
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    public void girisMetodu(View view){
        inf = LayoutInflater.from(this);
        pencere = inf.inflate(R.layout.girispencere, null);
        KullanicAdi = pencere.findViewById(R.id.usernameEditText);
        sifre = pencere.findViewById(R.id.passwordEditText);
        BottomSheetDialog diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        diyalog.show();
    }

    public void girisYap(View view){
        String kullaniciAdi = String.valueOf(KullanicAdi.getText());
        String ksifre = String.valueOf(sifre.getText());
        System.out.println(kullaniciAdi+" : "+ ksifre);

        if (kullaniciAdi.isEmpty() || ksifre.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                boolean girisBasarili = false;

            for (DocumentSnapshot satir : queryDocumentSnapshots) {
                    if (satir.getString("kullaniciAdi").equals(kullaniciAdi) && satir.getString("kullaniciSifre").equals(ksifre)) {
                        girisBasarili = true;
                        Toast.makeText(MainActivity.this, "Hoş geldin " + kullaniciAdi, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (!girisBasarili) {
                    Toast.makeText(MainActivity.this, "Geçersiz e-posta veya şifre", Toast.LENGTH_SHORT).show();
                }
            });

    }
}