package com.emrullah.catmap.ui.main;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.UyariMesaji;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    private MutableLiveData<String>_kullaniciAdi=new MutableLiveData<>();
    public LiveData<String>kullaniciAdi(){return _kullaniciAdi;}

    public MainViewModel() {
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
                takipEdilenData.put("KullaniciAdi", hedefKullaniciSnapshot.getString("KullaniciAdi"));  // Doğru alan adı
                takipEdilenData.put("profilFotoUrl", hedefKullaniciSnapshot.getString("profilFotoUrl"));
                transaction.set(takipEdilenDocRef, takipEdilenData);
                takipEdilenSayisi += 1;
                takipEklendi = true;
            }

            // Eğer takipçi dokümanı yoksa ekle
            if (!takipciDocSnap.exists()) {
                Map<String, Object> takipciData = new HashMap<>();
                takipciData.put("followedAt", FieldValue.serverTimestamp());
                takipciData.put("KullaniciAdi", hedefKullaniciSnapshot.getString("KullaniciAdi"));  // Doğru alan adı
                takipciData.put("profilFotoUrl", hedefKullaniciSnapshot.getString("profilFotoUrl"));
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
            Log.d("Firestore", "Takip işlemi başarılı ve sayılar güncellendi");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takip işlemi başarısız", e);
        });
    }

    public void takipEdiliyorMu(String bakilanId,Context context){
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
            Log.d("Firestore", "Takipten çıkma işlemi başarılı ve sayılar güncellendi");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Takipten çıkma işlemi başarısız", e);
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

}