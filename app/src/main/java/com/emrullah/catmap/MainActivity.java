package com.emrullah.catmap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
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
    public static Kullanici kullanici;
    private boolean GirisYapildi;
    private ImageView GirisYapButon;
    private ImageView KayitOlButon;
    private ImageView KediKaydetButon;
    private ImageView KediHaritaButon;
    private ImageView Goz;
    boolean AcikMi;



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
        KayitOlButon = findViewById(R.id.kaydolid);
        GirisYapButon = findViewById(R.id.girisid);
        KediHaritaButon = findViewById(R.id.haritaid);
        KediKaydetButon = findViewById(R.id.yukleid);
        SharedPreferences kayit = getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
        GirisYapildi = kayit.getBoolean("GirisYapildi",false);
        if(GirisYapildi){
            kullanici.GetYerelKullanici(this);
            System.out.println(kullanici.getAd());
            KayitOlButon.setVisibility(View.INVISIBLE);
            GirisYapButon.setVisibility(View.INVISIBLE);
        }
        else{
            KediKaydetButon.setVisibility(View.INVISIBLE);
            KediHaritaButon.setVisibility(View.INVISIBLE);
        }
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
        Goz = pencere.findViewById(R.id.eyeIcon);
        diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        diyalog.show();
        AcikMi = false;
        Goz.setOnClickListener( btn ->{
            GozAcKapa(sifre,Goz);
        });
    }

    private void GozAcKapa(EditText sifre, ImageView Goz){
        int imlecKonum = sifre.getSelectionStart();
        if(!AcikMi){
            sifre.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            Goz.setImageResource(R.drawable.acik_goz);
        }
        else{
            sifre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            Goz.setImageResource(R.drawable.kapali_goz);
        }
        sifre.setSelection(imlecKonum);
        AcikMi = !AcikMi;
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

                if(satir.getString("KullaniciAdi")==null || satir.getString("Sifre")==null){ continue; }

                    if (satir.getString("KullaniciAdi").equals(kullanici.getKullaniciAdi()) && satir.getString("Sifre").equals(kullanici.getSifre())) {
                        girisBasarili = true;
                        kullanici.setAd(satir.getString("Ad"));
                        kullanici.setSoyad(satir.getString("Soyad"));
                        kullanici.setEmail(satir.getString("Email"));
                        YerelKayit();
                        Toast.makeText(MainActivity.this, "Hoş geldin " + kullanici.getKullaniciAdi(), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (!girisBasarili) {
                    Toast.makeText(MainActivity.this, "Geçersiz kullanıcı adı veya şifre", Toast.LENGTH_SHORT).show();
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
        if(!EmailKontrol(kullanici.getEmail())){
            Toast.makeText(this, "Lütfen geçerli bir email adresi giriniz!", Toast.LENGTH_SHORT).show();
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
        Goz = pencere.findViewById(R.id.eyeIcon);
        EmailEditT = pencere.findViewById(R.id.emailEditText);
        AdEditT = pencere.findViewById(R.id.adEditText);
        SoyadEditT = pencere.findViewById(R.id.soyadEditText);
        diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        diyalog.show();
        AcikMi = false;
        Goz.setOnClickListener( btn ->{
            GozAcKapa(sifre,Goz);
        });
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
                                        Toast.makeText(this, "Bu kullanıcı adı ile daha önce kayıt yapılmış.", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        db.collection("users")
                                                .add(kullanici.KullaniciData())
                                                .addOnSuccessListener(documentReference -> {
                                                    YerelKayit();
                                                    Toast.makeText(this, "Kayıt Başarılı", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e ->{
                                                    Toast.makeText(this, "Kayıt Başarısız", Toast.LENGTH_SHORT).show();
                                                });

                                    }
                                });
                    }
                })
                .addOnFailureListener(e->{
                });
    }

    private String donusum(EditText text){
        return String.valueOf(text.getText()).trim();
    }

    private boolean EmailKontrol(String Email){
        return Email!=null && Patterns.EMAIL_ADDRESS.matcher(Email).matches();
    }

    private void YerelKayit(){
        SharedPreferences kayit = getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
        SharedPreferences.Editor editor = kayit.edit();
        editor.putString("ID",kullanici.getID());
        editor.putString("Ad",kullanici.getAd());
        editor.putString("Soyad",kullanici.getSoyad());
        editor.putString("Email",kullanici.getEmail());
        editor.putString("KullaniciAdi",kullanici.getKullaniciAdi());
        editor.putString("Sifre",kullanici.getSifre());
        editor.putBoolean("GirisYapildi",true);
        editor.apply();
        diyalog.dismiss();
        ButonlariKaybet();
    }



    public void ButonlariKaybet(){
        KediHaritaButon.setTranslationY(-2000f);
        KediKaydetButon.setTranslationY(-2000f);
        GirisYapButon.setEnabled(true);
        GirisYapButon.setClickable(true);
        GirisYapButon.animate()
                .translationX(GirisYapButon.getWidth()+ 2000f)
                .setDuration(1000)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        KayitOlButon.setEnabled(true);
        KayitOlButon.setClickable(true);
        KayitOlButon.animate()
                .translationX(KayitOlButon.getWidth()-2000f)
                .setDuration(1000)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        KediHaritaButon.setVisibility(View.VISIBLE);
        KediHaritaButon.animate()
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        KediKaydetButon.setVisibility(View.VISIBLE);
        KediKaydetButon.animate()
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(new AccelerateInterpolator())
                .start();

    }

}