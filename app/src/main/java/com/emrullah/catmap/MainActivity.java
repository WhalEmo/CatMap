package com.emrullah.catmap;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;


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
    private FrameLayout YuklemeEkrani;
    private TextView Durum;
    private ImageView BasariliTik;
    private ImageView BasarisizCarpi;
    private ProgressBar YuklemeBar;
    private Dialog yuklemeDialog;
    private UyariMesaji uyariMesaji;
    private LinearLayout Profil;
    private ConstraintLayout GirisKayit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            Profil=findViewById(R.id.profilAlani);
            GirisKayit=findViewById(R.id.icerik_layout);
            getSupportFragmentManager().addOnBackStackChangedListener(() -> {
                Fragment profilFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                if (profilFragment instanceof ProfilSayfasiFragment) {
                    Profil.setVisibility(View.GONE);
                    GirisKayit.setVisibility(View.GONE);
                } else {
                    Profil.setVisibility(View.VISIBLE);
                    GirisKayit.setVisibility(View.VISIBLE);
                }
            });
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
            uyariMesaji = new UyariMesaji(this,false);
        }
    }

    public void profilSayfasinaGit(View view){
        Profil.setVisibility(View.GONE);
        GirisKayit.setVisibility(View.GONE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new ProfilSayfasiFragment())
                    .addToBackStack(null) // geri tuşuyla geri gelmek için
                    .commit();
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
        } //
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
        //yukleme durumu
        uyariMesaji.YuklemeDurum("Giriş Yapılıyor...");
        kullanici.setKullaniciAdi(donusum(KullanicAdi));
        kullanici.setSifre(donusum(sifre));
        kullanici.setGirisBasarili(false);
        System.out.println(kullanici.getKullaniciAdi()+" : "+ kullanici.getSifre());

        if (kullanici.getKullaniciAdi().isEmpty() || kullanici.getSifre().isEmpty()) {
            uyariMesaji.BasarisizDurum("Lütfen tüm alanları doldurun",1000);
            return;
        }
        db.collection("users")
                .whereEqualTo("KullaniciAdi", kullanici.getKullaniciAdi())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        uyariMesaji.BasarisizDurum("Kullanıcı adı bulunamadı!",1000);
                        return;
                    }
                    DocumentSnapshot satir = queryDocumentSnapshots.getDocuments().get(0);
                    kullanici.setAd(satir.getString("Ad"));
                    kullanici.setSoyad(satir.getString("Soyad"));
                    kullanici.setEmail(satir.getString("Email"));
                    kullanici.setID(satir.getId());
                    DogrulamaKodYonetici ynt = new DogrulamaKodYonetici();
                    ynt.girisYap(kullanici.getEmail(), kullanici.getSifre(), basarili -> {
                        if (basarili) {
                            YerelKayit();
                            uyariMesaji.BasariliDurum("Giriş Başarılı...",1000);
                        } else {
                            uyariMesaji.BasarisizDurum("Giriş Başarısız...",1000);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                });
    }
    public void kaydol(View view){
        uyariMesaji.YuklemeDurum("Kayıt Yapılıyor...");
        kullanici.setAd(donusum(AdEditT));
        kullanici.setSoyad(donusum(SoyadEditT));
        kullanici.setEmail(donusum(EmailEditT));
        kullanici.setKullaniciAdi(donusum(KullanicAdi));
        kullanici.setSifre(donusum(sifre));

        if(!kullanici.KullaniciIs()){
            uyariMesaji.BasarisizDurum("Lütfen tüm alanları doldurun",1000);
            return;
        }
        if(!EmailKontrol(kullanici.getEmail())){
            uyariMesaji.BasarisizDurum("Lütfen geçerli bir email adresi giriniz!",1000);
            return;
        }
        if(kullanici.getSifre().length()<5){
            uyariMesaji.BasarisizDurum("Lütfen şifreyi en az 5 haneli giriniz!",1000);
            return;
        }
        System.out.println(kullanici.getAd());
        VeriTabaninaKayit();
    }

    public void SifremiUnuttum(View view){
        if(diyalog!=null && diyalog.isShowing()){
            diyalog.dismiss();
        }
        View pencere = LayoutInflater.from(this).inflate(R.layout.sifremi_unuttum,null);
        EmailEditT = pencere.findViewById(R.id.emailEditText);
        diyalog = new BottomSheetDialog(this);
        diyalog.setContentView(pencere);
        pencere.setVisibility(View.VISIBLE);
        diyalog.show();
    }
    public void sifreSifirla(View view){
        view.setClickable(false);
        uyariMesaji.YuklemeDurum("Mail Gönderiliyor...");
        DogrulamaKodYonetici ynt = new DogrulamaKodYonetici();
        ynt.sifreSifirla(donusum(EmailEditT),basariliMi->{
            if(basariliMi){
                uyariMesaji.BasariliDurum("Mail Gönderildi.",1000);
            }
            else {
                uyariMesaji.BasarisizDurum("Mail Gönderilemedi.",1000);
                view.setClickable(true);
            }
        });
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
        pencere.setVisibility(View.VISIBLE);
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
                        uyariMesaji.BasarisizDurum("Email ile daha önce kayıt yapılmış.",1000);
                    }
                    else{
                        db.collection("users")
                                .whereEqualTo("KullaniciAdi",kullanici.getKullaniciAdi())
                                .get()
                                .addOnSuccessListener(cevap->{
                                    if(!cevap.isEmpty()){
                                        uyariMesaji.BasarisizDurum("Bu kullanıcı adı ile daha önce kayıt yapılmış.",1000);
                                    }
                                    else{
                                        DogrulamaKodYonetici ynt = new DogrulamaKodYonetici();
                                        ynt.kaydetSifreEmail(kullanici.getEmail(),kullanici.getSifre(),basariliMi->{
                                            if(basariliMi){
                                                db.collection("users")
                                                        .add(kullanici.KullaniciData())
                                                        .addOnSuccessListener(documentReference -> {
                                                            YerelKayit();
                                                            uyariMesaji.BasariliDurum("Kayıt Başarılı...",1000);
                                                        })
                                                        .addOnFailureListener(e ->{
                                                            uyariMesaji.BasarisizDurum("Kayıt Başarısız!",1000);
                                                        });
                                            }
                                            else {
                                                Toast.makeText(this, "Email veya şifre kaydı başarısız", Toast.LENGTH_SHORT).show();
                                            }
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

    public void Sohbet(View view){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,new MesajFragment(this))
                .addToBackStack(null)  // geri tuşuyla geri döner
                .commit();
    }
}