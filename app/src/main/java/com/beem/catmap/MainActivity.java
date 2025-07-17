package com.beem.catmap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.beem.catmap.mesaj.MesajFragment;
import com.beem.catmap.sohbet.SohbetFragment;
import com.beem.catmap.sohbet.SohbetYonetici;
import com.beem.catmap.ui.main.ProfilSayfasiFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;


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
    private UyariMesaji uyariMesaji;
    private LinearLayout Profil;
    private LinearLayout sohbetAlani;
    private ConstraintLayout GirisKayit;
    private ConstraintLayout ustCubuk;
    private TextView haritatext;
    private TextView yuklemetext;


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
        ustCubuk = findViewById(R.id.ustCubuk);
        sohbetAlani = findViewById(R.id.sohbetAlani);
        kullanici = new Kullanici();
        KayitOlButon = findViewById(R.id.kaydolid);
        GirisYapButon = findViewById(R.id.girisid);
        KediHaritaButon = findViewById(R.id.haritaid);
        KediKaydetButon = findViewById(R.id.yukleid);
        haritatext = findViewById(R.id.haritatext);
        yuklemetext = findViewById(R.id.yuklemetext);
        CevrimIciYonetimi.getInstance().setAnasayfaGorunuyor(true);
        SharedPreferences kayit = getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
        GirisYapildi = kayit.getBoolean("GirisYapildi",false);
        FragmentAyarlari();
        if(GirisYapildi){
            kullanici.GetYerelKullanici(this);
            System.out.println(kullanici.getAd());
            KayitOlButon.setVisibility(View.INVISIBLE);
            GirisYapButon.setVisibility(View.INVISIBLE);
            CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
        }
        else{
            ustCubuk.setVisibility(View.GONE);
            KediKaydetButon.setVisibility(View.INVISIBLE);
            KediHaritaButon.setVisibility(View.INVISIBLE);
            haritatext.setVisibility(View.GONE);
            yuklemetext.setVisibility(View.GONE);
            uyariMesaji = new UyariMesaji(this,false);
        }
        SohbetMesajAyarlari();
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (currentFragment instanceof AnasayfaFragment) {
                sohbetAlani.setVisibility(View.VISIBLE);
                ustCubuk.setVisibility(View.VISIBLE);
                FragmentAyarlari();
            } else {
                sohbetAlani.setVisibility(View.GONE);
                ustCubuk.setVisibility(View.GONE);
            }
        });
    }

    public void profilSayfasinaGit(View view){
        Profil.setVisibility(View.GONE);
        GirisKayit.setVisibility(View.GONE);
        ustCubuk.setVisibility(View.GONE);
        ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(MainActivity.kullanici.getID());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null) // geri tuşuyla geri gelmek için
                    .commit();
    }

    public void yuklemeSayfasi(View view){
        System.out.println("Yukleme Sayfasi gecildi");
        CevrimIciYonetimi.getInstance().YuklemeArayuzAktivitiyeGecildi();
        Intent intent = new Intent(MainActivity.this, YuklemeArayuzuActivity.class);
        startActivity(intent);
    }
    public  void haritaSayfasi(View view){
        CevrimIciYonetimi.getInstance().HaritaArayuzAktivitiyeGecildi();
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        CevrimIciYonetimi.getInstance().setAnasayfaGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
    }
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("onStop");
        CevrimIciYonetimi.getInstance().setAnasayfaGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
    }
    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("onPause");
        CevrimIciYonetimi.getInstance().setAnasayfaGorunuyor(false);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
    }
    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("onResum");
        CevrimIciYonetimi.getInstance().setAnasayfaGorunuyor(true);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
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
                                                            kullanici.setID(documentReference.getId());
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
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
        CevrimIciYonetimi.getInstance().CevrimIciCalistir(kullanici);
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
        ustCubuk.setVisibility(View.VISIBLE);
        haritatext.setVisibility(View.VISIBLE);
        yuklemetext.setVisibility(View.VISIBLE);
    }

    public void Sohbet(View view){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container,new SohbetFragment(()->{
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container,new MesajFragment(this))
                            .addToBackStack(null)
                            .commit();
                }))
                .addToBackStack(null)  // geri tuşuyla geri döner
                .commit();
        GirisKayit.setVisibility(View.GONE);
    }

    /// bu metodda mesajlaşma ve sohbetteki başlatılmadan önce temel ayarlar yapılır
    private void SohbetMesajAyarlari(){
        SohbetYonetici.getInstance().setKullanicilar(new HashMap<>());
        SohbetYonetici.getInstance().setSonMesajlar(new HashMap<>());
        SohbetYonetici.getInstance().setProfilFotolari(new HashMap<>());
    }


    ///
    private void CevrimIciOl(boolean durumu){
        if(durumu == kullanici.isCevrimiciMi()) return;
        if(kullanici.getID()==null) return;
        DatabaseReference durum = FirebaseDatabase.getInstance().getReference("durumlar");
        durum.child(kullanici.getID()).child("cevrimici").setValue(durumu);
        kullanici.setCevrimiciMi(durumu);
        if(!durumu){
            durum.child(kullanici.getID()).child("sonGorulme").setValue(System.currentTimeMillis());
            kullanici.setSonGorulme(System.currentTimeMillis());
        }
    }

    private void FragmentAyarlari(){
        sohbetAlani.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new AnasayfaFragment())
                .commit();
    }
}