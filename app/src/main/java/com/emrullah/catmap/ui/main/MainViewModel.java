package com.emrullah.catmap.ui.main;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Adapter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.emrullah.catmap.GonderiYuklemeListener;
import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.UyariMesaji;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainViewModel extends ViewModel {
    private FirebaseFirestore db;
    public MutableLiveData<String>_Url=new MutableLiveData<>();
    public LiveData<String>UrlLiveData(){return _Url;}
    public MutableLiveData<Long>_takipEdilenSayisi=new MutableLiveData<>();
    public LiveData<Long>takipEdilenSayisiLiveData(){

        return _takipEdilenSayisi;
    }
    public MutableLiveData<Long>_takipciSayisi=new MutableLiveData<>();
    public LiveData<Long>takipciSayisiLiveData(){
        return _takipciSayisi;
    }
    public MutableLiveData<String>_hakkinda = new MutableLiveData<>();
    public LiveData<String>hakkinda(){return _hakkinda;}
    private final MutableLiveData<Boolean>_takipDurumu = new MutableLiveData<>();
    public LiveData<Boolean>getTakipDurumu() {
        return _takipDurumu;
    }
    private MutableLiveData<Boolean> _beniTakipEdiyor = new MutableLiveData<>();
    public LiveData<Boolean> getBeniTakipEdiyor() {
        return _beniTakipEdiyor;
    }
    private MutableLiveData<ArrayList<String>> _benimEngellediklerim = new MutableLiveData<>();
    private MutableLiveData<ArrayList<String>> _beniEngelleyenler = new MutableLiveData<>();
    public LiveData<ArrayList<String>> BenimEngellediklerimLiveData() {
        return _benimEngellediklerim;
    }
    public LiveData<ArrayList<String>> BeniEngelleyenlerLiveData() {
        return _beniEngelleyenler;
    }
    private MutableLiveData<String>_kullaniciAdi=new MutableLiveData<>();
    public LiveData<String>kullaniciAdi(){return _kullaniciAdi;}
    private MutableLiveData<ArrayList<Gonderi>> _kediIdGonderilist = new MutableLiveData<>();
    public LiveData<ArrayList<Gonderi>>kediGonderi(){return _kediIdGonderilist;}
    public MutableLiveData<Integer>_GonderiSayisi=new MutableLiveData<>();
    public LiveData<Integer>GonderiSayisi(){return _GonderiSayisi;}
    private MediatorLiveData<Pair<Boolean, Boolean>> takipDurumuCift = new MediatorLiveData<>();

    private final MutableLiveData<String> yukleyenID = new MutableLiveData<>();

    public void gonderiSil(String kediId) {
        List<Gonderi> mevcutListe = _kediIdGonderilist.getValue();
        if (mevcutListe != null) {
            ArrayList<Gonderi> guncelListe = new ArrayList<>(mevcutListe);
            guncelListe.removeIf(g -> g.getKediID().equals(kediId));
            _kediIdGonderilist.setValue(guncelListe);
            int sayi=_GonderiSayisi.getValue();
            _GonderiSayisi.setValue(sayi-1);
        }
    }

    public void setYukleyenID(String id) {
        yukleyenID.setValue(id);
    }

    public LiveData<String> getYukleyenID() {
        return yukleyenID;
    }

    public void takipDurumlariniBirlestir() {
        takipDurumuCift.addSource(_takipDurumu, benimTakipDurumum ->
                takipDurumuCift.setValue(new Pair<>(benimTakipDurumum, _beniTakipEdiyor.getValue()))
        );

        takipDurumuCift.addSource(_beniTakipEdiyor, onunTakipDurumu ->
                takipDurumuCift.setValue(new Pair<>(_takipDurumu.getValue(), onunTakipDurumu))
        );
    }

    public LiveData<Pair<Boolean, Boolean>> getTakipDurumuCift() {
        return takipDurumuCift;
    }


    public MainViewModel() {
        takipDurumlariniBirlestir();
        db = FirebaseFirestore.getInstance();
    }

    public void profilFotoUrlGetirVeCachele(Context context,String kullaniciId) {
                db.collection("users")
                .document(kullaniciId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String url = documentSnapshot.getString("profilFotoUrl");
                        _Url.postValue(url);
                        if(MainActivity.kullanici.getID()==kullaniciId) {
                            SharedPreferences sp = context.getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
                            sp.edit().putString("profil_url", url).apply();
                        }
                    }
                });
    }

    public void profilFotoUrlKaydetFirebaseVeCachele(Uri imageUri, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference()
                .child("profilFotolari")
                .child(UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(MainActivity.kullanici.getID())
                            .update("profilFotoUrl", url)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FirestoreUpdate", "Profil fotoğrafı URL'si başarıyla güncellendi.");
                                // SharedPreferences'e kaydet
                                SharedPreferences sp = context.getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
                                sp.edit().putString("profil_url", url).apply();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreUpdate", "Profil fotoğrafı URL'si güncellenemedi!", e);
                            });
                }))
                .addOnFailureListener(e -> {
                    Log.e("StorageUpload", "Profil fotoğrafı yüklenemedi!", e);
                });
    }

    public void TakipEt(String TakipEttiginId){
        List<String> benimEngellediklerim = _benimEngellediklerim.getValue();
        List<String> beniEngelleyenler = _beniEngelleyenler.getValue();

        if ((benimEngellediklerim != null && benimEngellediklerim.contains(TakipEttiginId)) ||
                (beniEngelleyenler != null && beniEngelleyenler.contains(MainActivity.kullanici.getID()))) {
            return;
        }
        DocumentReference mevcutKullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
        DocumentReference TakipEtiginRef = db.collection("users").document(TakipEttiginId);

        db.runTransaction(transaction -> {
            DocumentSnapshot mevcutKullaniciSnapshot = transaction.get(mevcutKullaniciRef);
            DocumentSnapshot hedefKullaniciSnapshot = transaction.get(TakipEtiginRef);

            // Takip edilen sayısı
            Long takipEdilenSayisi = mevcutKullaniciSnapshot.getLong("TakipEdilenSayisi");
            if (takipEdilenSayisi == null) takipEdilenSayisi = 0L;

            // Takipçi sayısı
            Long takipciSayisi = hedefKullaniciSnapshot.getLong("takipciSayisi");
            if (takipciSayisi == null) takipciSayisi = 0L;

            // Kullanıcının alt koleksiyonundaki 'takipEdilenler' koleksiyon referansı
            CollectionReference takipEdilenlerSubCol = mevcutKullaniciRef.collection("takipEdilenler");
            // Hedef kullanıcının alt koleksiyonundaki 'takipciler' koleksiyon referansı
            CollectionReference takipcilerSubCol = TakipEtiginRef.collection("takipciler");

            // Kontrol için mevcut takip edilen dokümanı alma
            DocumentReference takipEdilenDocRef = takipEdilenlerSubCol.document(TakipEttiginId);
            DocumentSnapshot takipEdilenDocSnap = transaction.get(takipEdilenDocRef);

            // Kontrol için mevcut takipçi dokümanı alma
            DocumentReference takipciDocRef = takipcilerSubCol.document(MainActivity.kullanici.getID());
            DocumentSnapshot takipciDocSnap = transaction.get(takipciDocRef);

            boolean takipEklendi = false;

            // Eğer takip edilen dokümanı yoksa ekle
            if (!takipEdilenDocSnap.exists()) {
                Map<String, Object> takipEdilenData = new HashMap<>();
                takipEdilenData.put("followedAt", FieldValue.serverTimestamp());
                takipEdilenData.put("KullaniciAdi", hedefKullaniciSnapshot.getString("KullaniciAdi"));  // doğru
                takipEdilenData.put("profilFotoUrl", hedefKullaniciSnapshot.getString("profilFotoUrl"));
                takipEdilenData.put("ID", hedefKullaniciSnapshot.getId());
                transaction.set(takipEdilenDocRef, takipEdilenData);
                takipEdilenSayisi += 1;
                takipEklendi = true;
            }

            // Eğer takipçi dokümanı yoksa ekle
            if (!takipciDocSnap.exists()) {
                Map<String, Object> takipciData = new HashMap<>();
                takipciData.put("followedAt", FieldValue.serverTimestamp());
                takipciData.put("KullaniciAdi", mevcutKullaniciSnapshot.getString("KullaniciAdi"));  // ✅ düzeltildi
                takipciData.put("profilFotoUrl", mevcutKullaniciSnapshot.getString("profilFotoUrl"));
                takipciData.put("ID", mevcutKullaniciSnapshot.getId());
                transaction.set(takipciDocRef, takipciData);
                if (takipEklendi) { // Sadece takip eklenmişse takipçi sayısını artır
                    takipciSayisi += 1;
                }
            }

            // Takip sayıları güncelle
            transaction.update(mevcutKullaniciRef, "TakipEdilenSayisi", takipEdilenSayisi);
            transaction.update(TakipEtiginRef, "takipciSayisi", takipciSayisi);

            return null;
        }).addOnSuccessListener(aVoid -> {
            _takipDurumu.setValue(true);
            Log.d("Firestore", "Takip işlemi başarılı ve sayılar güncellendi");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takip işlemi başarısız", e);
        });
    }

    public void takipEdiliyorMu(String bakilanId){
        db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipEdilenler")
                .document(bakilanId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    _takipDurumu.setValue(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e("TakipKontrol", "Hata oluştu", e);
                    _takipDurumu.setValue(false);
                });
    }
    public void beniTakipEdiyorMu(String kullaniciId) {
        db.collection("users")
                .document(MainActivity.kullanici.getID()) // BENİM kullanıcı dokümanım
                .collection("takipciler")
                .document(kullaniciId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    _beniTakipEdiyor.setValue(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e("TakipçiKontrol", "Hata oluştu", e);
                    _beniTakipEdiyor.setValue(false);
                });
    }


    public void TakiptenCikarma(String TakiptenCiktiginId){
        DocumentReference mevcutKullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
        DocumentReference TakipEtiginRef = db.collection("users").document(TakiptenCiktiginId);

        db.runTransaction(transaction -> {
            DocumentSnapshot mevcutKullaniciSnapshot = transaction.get(mevcutKullaniciRef);
            DocumentSnapshot hedefKullaniciSnapshot = transaction.get(TakipEtiginRef);

            // Takip edilen sayısı
            Long takipEdilenSayisi = mevcutKullaniciSnapshot.getLong("TakipEdilenSayisi");
            if (takipEdilenSayisi == null) takipEdilenSayisi = 0L;

            // Takipçi sayısı
            Long takipciSayisi = hedefKullaniciSnapshot.getLong("takipciSayisi");
            if (takipciSayisi == null) takipciSayisi = 0L;

            // Alt koleksiyon referansları

            CollectionReference takipEdilenlerSubCol = mevcutKullaniciRef.collection("takipEdilenler");
            CollectionReference takipcilerSubCol = TakipEtiginRef.collection("takipciler");

            // Takip edilen dokümanı referansı
            DocumentReference takipEdilenDocRef = takipEdilenlerSubCol.document(TakiptenCiktiginId);
            DocumentSnapshot takipEdilenDocSnap = transaction.get(takipEdilenDocRef);

            // Takipçi dokümanı referansı
            DocumentReference takipciDocRef = takipcilerSubCol.document(MainActivity.kullanici.getID());
            DocumentSnapshot takipciDocSnap = transaction.get(takipciDocRef);

            boolean takiptenCikildi = false;

            // Eğer takip edilen dokümanı varsa sil
            if (takipEdilenDocSnap.exists()) {
                transaction.delete(takipEdilenDocRef);
                takipEdilenSayisi = Math.max(takipEdilenSayisi - 1, 0);
                takiptenCikildi = true;
            }

            // Eğer takipçi dokümanı varsa sil
            if (takipciDocSnap.exists()) {
                transaction.delete(takipciDocRef);
                if (takiptenCikildi) {
                    takipciSayisi = Math.max(takipciSayisi - 1, 0);
                }
            }

            // Takip sayıları güncelle
            transaction.update(mevcutKullaniciRef, "TakipEdilenSayisi", takipEdilenSayisi);
            transaction.update(TakipEtiginRef, "takipciSayisi", takipciSayisi);

            return null;
        }).addOnSuccessListener(aVoid -> {
            _takipDurumu.setValue(false);
            Log.d("Firestore", "Takipten çıkma işlemi başarılı ve sayılar güncellendi");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takipten çıkma işlemi başarısız", e);
        });
    }
    public void TakipcidenCikarma(String takipciID) {
        DocumentReference mevcutKullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
        DocumentReference takipciRef = db.collection("users").document(takipciID);

        db.runTransaction(transaction -> {
            DocumentSnapshot mevcutKullaniciSnapshot = transaction.get(mevcutKullaniciRef);
            DocumentSnapshot takipciSnapshot = transaction.get(takipciRef);

            // Sayılar
            Long takipciSayisi = mevcutKullaniciSnapshot.getLong("takipciSayisi");
            if (takipciSayisi == null) takipciSayisi = 0L;

            Long takipEdilenSayisi = takipciSnapshot.getLong("TakipEdilenSayisi");
            if (takipEdilenSayisi == null) takipEdilenSayisi = 0L;

            // Alt koleksiyon referansları
            CollectionReference takipcilerSubCol = mevcutKullaniciRef.collection("takipciler");
            CollectionReference takipEdilenlerSubCol = takipciRef.collection("takipEdilenler");

            // Döküman referansları
            DocumentReference takipciDocRef = takipcilerSubCol.document(takipciID);
            DocumentSnapshot takipciDocSnap = transaction.get(takipciDocRef);

            DocumentReference takipEdilenDocRef = takipEdilenlerSubCol.document(MainActivity.kullanici.getID());
            DocumentSnapshot takipEdilenDocSnap = transaction.get(takipEdilenDocRef);

            boolean takipciSilindi = false;

            // Eğer takipçi olarak varsa sil
            if (takipciDocSnap.exists()) {
                transaction.delete(takipciDocRef);
                takipciSayisi = Math.max(takipciSayisi - 1, 0);
                takipciSilindi = true;
            }

            // Karşı tarafın takip ettiği kişi olarak ben varsam, sil
            if (takipEdilenDocSnap.exists()) {
                transaction.delete(takipEdilenDocRef);
                if (takipciSilindi) {
                    takipEdilenSayisi = Math.max(takipEdilenSayisi - 1, 0);
                }
            }

            // Sayıları güncelle
            transaction.update(mevcutKullaniciRef, "takipciSayisi", takipciSayisi);
            transaction.update(takipciRef, "TakipEdilenSayisi", takipEdilenSayisi);

            return null;
        }).addOnSuccessListener(aVoid -> {
            _beniTakipEdiyor.setValue(false);
            Log.d("Firestore", "Takipçiden çıkarma işlemi başarılı");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takipçiden çıkarma işlemi başarısız", e);
        });
    }


    public void HakkindaDBEkle(String hakkindasi,Context context){
        Map<String, Object> veri = new HashMap<>();
        veri.put("Hakkinda", hakkindasi);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(MainActivity.kullanici.getID())
                .update(veri)
                .addOnSuccessListener(documentReference  ->{
                    // SharedPreferences'e kaydet
                    SharedPreferences sp = context.getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
                    sp.edit().putString("Hakkinda", hakkindasi).apply();
                    _hakkinda.postValue(hakkindasi);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Yanıt eklenemedi", e));
    }
    public void HakkindaGetir(String KullaniciId){
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(KullaniciId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    //if gereksiz olabilir bida bak
                    if (documentSnapshot.exists()) {
                        String hakkindaMetni = documentSnapshot.getString("Hakkinda");
                        if (hakkindaMetni != null) {
                            _hakkinda.postValue(hakkindaMetni);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Hakkında metni alınamadı", e));
    }
    public void TakipTakipciSayisi(String Id,Context context){
        DocumentReference kullaniciRef=db.collection("users")
                .document(Id);
        kullaniciRef.get().addOnSuccessListener(documentSnapshot->{
            if (documentSnapshot.exists()){
                Long TakipEdilenSayisi=documentSnapshot.getLong("TakipEdilenSayisi");
                Long TakipciSayisi=documentSnapshot.getLong("takipciSayisi");

                if (TakipEdilenSayisi == null) TakipEdilenSayisi = 0L;
                if (TakipciSayisi == null) TakipciSayisi = 0L;

                _takipEdilenSayisi.postValue(TakipEdilenSayisi);
                _takipciSayisi.postValue(TakipciSayisi);
                SharedPreferences sp = context.getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
                sp.edit()
                        .putLong("cache_takip", TakipEdilenSayisi)
                        .putLong("cache_takipci", TakipciSayisi)
                        .apply();
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takip sayıları alınamadı", e);
        });

    }

    public void KAdiDBekle(String kullaniciAdi,Context context,UyariMesaji uyari){
        uyari.YuklemeDurum("Kaydediliyor...");
        db.collection("users")
                .whereEqualTo("KullaniciAdi",kullaniciAdi)
                .get()
                .addOnSuccessListener(c->{
                    if(c.isEmpty()){
                        db.collection("users")
                                .document(MainActivity.kullanici.getID())
                                .update("KullaniciAdi",kullaniciAdi)
                                .addOnSuccessListener(b->{
                                    MainActivity.kullanici.setKullaniciAdi(kullaniciAdi);

                                    SharedPreferences sp = context.getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
                                    sp.edit().putString("KullaniciAdi",kullaniciAdi).apply();
                                    _kullaniciAdi.postValue(kullaniciAdi);
                                    uyari.BasariliDurum("Güncelleme başarılı.",1000);
                                    uyari.DahaOnceAlinmisMi=false;
                                });
                    }else{
                        uyari.BasarisizDurum("Bu kullanıcı adı daha önce alınmış",1000);
                        uyari.DahaOnceAlinmisMi=true;
                    }
                });
    }
    public void KullaniciAdiGetirDB(String KullaniciId){
        db.collection("users")
                .document(KullaniciId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String isim=documentSnapshot.getString("KullaniciAdi");
                         _kullaniciAdi.postValue(isim);
                    }
                });
    }
    public void engelle(String engellenecekKullaniciId,String kisiId,UyariMesaji uyari) {
        uyari.YuklemeDurum("Engelleniyor...");
        DocumentReference kullaniciRef = db.collection("users").document(kisiId);

        kullaniciRef.update("blockedUsers", FieldValue.arrayUnion(engellenecekKullaniciId))
                .addOnSuccessListener(aVoid -> {
                    // Güncel listeyi tekrar çek
                    kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ArrayList<String> engellilerListesi = (ArrayList<String>) documentSnapshot.get("blockedUsers");
                            if (engellilerListesi == null) engellilerListesi = new ArrayList<>();
                            _benimEngellediklerim.setValue(engellilerListesi);
                        }
                    });
                    uyari.BasariliDurum("Engellendi",1000);
                })
                .addOnFailureListener(e -> {
                    uyari.BasarisizDurum("Engellenemedi",1000);
                    Log.e("Engelle", "Engelleme başarısız: " + e.getMessage());
                });
    }
    public void EngellileriGetir(String kisiId){
        DocumentReference kullaniciRef = db.collection("users").document(kisiId);

        kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ArrayList<String> engellilerListesi = (ArrayList<String>) documentSnapshot.get("blockedUsers");
                if (engellilerListesi == null) engellilerListesi = new ArrayList<>();
                _beniEngelleyenler.setValue(engellilerListesi);
            } else {
                _beniEngelleyenler.setValue(new ArrayList<>());
            }
        }).addOnFailureListener(e -> {
            Log.e("EngelVerisi", "Engelli kullanıcılar yüklenemedi: " + e.getMessage());
        });
    }
    public void benimEngellediklerimiiGetir(){
        DocumentReference kullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());

        kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ArrayList<String> engellilerListesi = (ArrayList<String>) documentSnapshot.get("blockedUsers");
                if (engellilerListesi == null) engellilerListesi = new ArrayList<>();
                _benimEngellediklerim.setValue(engellilerListesi);
            } else {
                _benimEngellediklerim.setValue(new ArrayList<>());
            }
        }).addOnFailureListener(e -> {
            Log.e("EngelVerisi", "Engelli kullanıcılar yüklenemedi: " + e.getMessage());
        });
    }
    public void engelKaldir(String engellenenKullaniciId,String kisiId,UyariMesaji uyari) {
        uyari.YuklemeDurum("Engel kaldırılıyor...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference kullaniciRef = db.collection("users").document(kisiId);
        kullaniciRef.update("blockedUsers", FieldValue.arrayRemove(engellenenKullaniciId))
                .addOnSuccessListener(aVoid -> {
                    // Güncel listeyi tekrar çek
                    kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ArrayList<String> engellilerListesi = (ArrayList<String>) documentSnapshot.get("blockedUsers");
                            if (engellilerListesi == null) engellilerListesi = new ArrayList<>();
                            _benimEngellediklerim.setValue(engellilerListesi);
                        }
                    });
                    uyari.BasariliDurum("Engel kaldırıldı",1000);
                })
                .addOnFailureListener(e -> {
                    uyari.BasarisizDurum("Engel kaldırılamadı",1000);
                    Log.e("Engelle", "Engelleme başarısız: " + e.getMessage());
                });
    }
    public void GonderiCekme(String id, UyariMesaji uyari, GonderiYuklemeListener listener) {
        DocumentReference kullaniciRef = db.collection("users").document(id);
        kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ArrayList<Map<String, Object>> liste = (ArrayList<Map<String, Object>>) documentSnapshot.get("GonderilenKediler");
                if (liste == null) liste = new ArrayList<>();
                ArrayList<Gonderi>gonderiListesi=new ArrayList<Gonderi>();
                if (liste.isEmpty()) {
                    _kediIdGonderilist.setValue(gonderiListesi); // boş liste dön
                    return;
                }
                liste.sort((o1, o2) -> {
                    Timestamp t1 = (Timestamp) o1.get("tarih");
                    Timestamp t2 = (Timestamp) o2.get("tarih");
                    return t2.compareTo(t1);
                });

                final int toplam = liste.size();
                final int[] tamamlanan = {0};

                for (Map<String, Object> gonderiMap : liste) {
                    String kediID = (String) gonderiMap.get("kediID");
                    Timestamp tarih = (Timestamp) gonderiMap.get("tarih");
                    db.collection("cats")
                            .document(kediID)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    ArrayList<String> fotoList = (ArrayList<String>) doc.get("photoUri");
                                    String aciklama = doc.getString("kediHakkinda");
                                    String kediadi=doc.getString("kediAdi");
                                    Long begeniSayisi = doc.getLong("begeniSayisi");
                                    if (fotoList != null && !fotoList.isEmpty()) {
                                        gonderiListesi.add(new Gonderi(fotoList, aciklama,kediadi,tarih,begeniSayisi,kediID));
                                    }
                                }
                                tamamlanan[0]++;
                                if (tamamlanan[0] == toplam) {
                                    gonderiListesi.sort((g1, g2) -> g2.getTarih().compareTo(g1.getTarih()));
                                    _kediIdGonderilist.setValue(gonderiListesi);
                                }
                                if (listener != null) {
                                    listener.onTumGonderilerYuklendi();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("KediGetirme", "Hata: " + e.getMessage());
                            });
                }
            } else {
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Hata: " + e.getMessage());
        });
    }
    public void GonderiSayisiniCek(String id) {
        DocumentReference kullaniciRef = db.collection("users").document(id);
        kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ArrayList<Map<String, Object>> liste = (ArrayList<Map<String, Object>>) documentSnapshot.get("GonderilenKediler");
                if (liste == null) {
                    _GonderiSayisi.setValue(0); // hiç gönderi yoksa
                } else {
                    _GonderiSayisi.setValue(liste.size()); // mevcut sayıyı ata
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("GonderiSayisi", "Veri çekme hatası: " + e.getMessage());
            _GonderiSayisi.setValue(0); // hata durumunda yine sıfır ata
        });
    }

    public void kullaniciyaGonderiSil(String kediID,UyariMesaji mesaji) {
        mesaji.YuklemeDurum("Gönderi siliniyor");
        DocumentReference kullaniciRef = db.collection("users").document(MainActivity.kullanici.getID());
        kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> gonderilenKediler = (List<Map<String, Object>>) documentSnapshot.get("GonderilenKediler");

            if (gonderilenKediler != null) {
                Map<String, Object> silinecekKedi = null;

                for (Map<String, Object> item : gonderilenKediler) {
                    if (kediID.equals(item.get("kediID"))) {
                        silinecekKedi = item;
                        break;
                    }
                }
                if (silinecekKedi != null) {
                    kullaniciRef.update("GonderilenKediler", FieldValue.arrayRemove(silinecekKedi))
                            .addOnSuccessListener(aVoid -> {
                                mesaji.BasariliDurum("Silindi", 1000);
                                Log.d("Silme", "Kedi silindi: " + kediID);
                            })
                            .addOnFailureListener(e -> {
                                mesaji.BasarisizDurum("Silinemedi", 1000);
                                Log.e("Silme", "Silme hatası: " + e.getMessage());
                            });
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("Silme", "Veri çekme hatası: " + e.getMessage());
        });
    }
    public interface SilmeCallback {
        void onSilmeTamamlandi();
    }
    public void HaritadanSilme(String kediId,SilmeCallback callback){
            db.collection("cats")
                    .document(kediId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) {
                            callback.onSilmeTamamlandi(); // -> MapsActivity'ye bildir
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("KediSilme", "Kedi silinirken hata oluştu: " + e.getMessage());
                    });
    }

}
