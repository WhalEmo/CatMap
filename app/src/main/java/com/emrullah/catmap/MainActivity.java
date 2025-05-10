package com.emrullah.catmap;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText KullanicAdi;
    private EditText sifre;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    BottomSheetDialog diyalog;
    private EditText AdEditT;
    private EditText SoyadEditT;
    private EditText EmailEditT;
    public Kullanici kullanici;

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
        kullanici = new Kullanici();
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
        if(diyalog!=null && diyalog.isShowing()){
            diyalog.dismiss();
        }
        View pencere = LayoutInflater.from(this).inflate(R.layout.girispencere,null);
        KullanicAdi = pencere.findViewById(R.id.usernameEditText);
        sifre = pencere.findViewById(R.id.passwordEditText);
        diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        diyalog.show();
    }

    public void girisYap(View view){
        kullanici.setKullaniciAdi(donusum(KullanicAdi));
        kullanici.setSifre(donusum(sifre));
        System.out.println(kullanici.getKullaniciAdi()+" : "+ kullanici.getSifre());

        if (kullanici.getKullaniciAdi().isEmpty() || kullanici.getSifre().isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                boolean girisBasarili = false;

            for (DocumentSnapshot satir : queryDocumentSnapshots) {
                    if (satir.getString("kullaniciAdi").equals(kullanici.getKullaniciAdi()) && satir.getString("kullaniciSifre").equals(kullanici.getSifre())) {
                        girisBasarili = true;
                        Toast.makeText(MainActivity.this, "Hoş geldin " + kullanici.getKullaniciAdi(), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (!girisBasarili) {
                    Toast.makeText(MainActivity.this, "Geçersiz e-posta veya şifre", Toast.LENGTH_SHORT).show();
                }
            });

    }
    public void kaydol(View view){
        kullanici.setAd(donusum(AdEditT));
        kullanici.setSoyad(donusum(SoyadEditT));
        kullanici.setEmail(donusum(EmailEditT));
        kullanici.setKullaniciAdi(donusum(KullanicAdi));
        kullanici.setSifre(donusum(sifre));

        if(!kullanici.KullaniciIs()){
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }
        if(kullanici.getSifre().length()<5){
            Toast.makeText(this, "Lütfen şifreyi en az 5 haneli giriniz!", Toast.LENGTH_SHORT).show();
            return;
        }
        System.out.println(kullanici.getAd());
        VeriTabaninaKayit();

    }

    public void kayitMetodu(View view){
        if(diyalog!=null && diyalog.isShowing()){
            diyalog.dismiss();
        }
        View pencere = LayoutInflater.from(this).inflate(R.layout.kaydolpencere,null);
        KullanicAdi = pencere.findViewById(R.id.usernameEditText);
        sifre = pencere.findViewById(R.id.passwordEditText);
        EmailEditT = pencere.findViewById(R.id.emailEditText);
        AdEditT = pencere.findViewById(R.id.adEditText);
        SoyadEditT = pencere.findViewById(R.id.soyadEditText);
        diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        diyalog.show();
    }

    private void VeriTabaninaKayit(){
        db.collection("users")
                .whereEqualTo("Email",kullanici.getEmail())
                .get()
                .addOnSuccessListener(sonuc->{
                    if(!sonuc.isEmpty()){
                        Toast.makeText(this, "Email ile daha önce kayıt yapılmış.", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        db.collection("users")
                                .whereEqualTo("KullaniciAdi",kullanici.getKullaniciAdi())
                                .get()
                                .addOnSuccessListener(cevap->{
                                    if(!cevap.isEmpty()){
                                        Toast.makeText(this, "Bu  ile d.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e->{

                });
    }

    private String donusum(EditText text){
        return String.valueOf(text.getText());
    }

}