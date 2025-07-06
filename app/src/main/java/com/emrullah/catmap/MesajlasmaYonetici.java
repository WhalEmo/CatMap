package com.emrullah.catmap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.sohbet.SohbetYonetici;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MesajlasmaYonetici {
    private DatabaseReference mesajlar = FirebaseDatabase.getInstance().getReference("mesajlar");

    private String sohbetID;
    private Kullanici gonderen = MainActivity.kullanici;
    private Kullanici alici;
    private static MesajlasmaYonetici yonetici;
    private ChildEventListener dinleyici;
    private ValueEventListener yaziyorDinleyici;
    private ValueEventListener cevrimiciDinleyici;



    public static MesajlasmaYonetici getInstance(){
        if(yonetici == null){
            yonetici = new MesajlasmaYonetici();
        }
        return yonetici;
    }

    public void MesajlasmaYoneticiStart(Runnable mesajlaricek) {
        sohbetIDOlustur(gonderen.getID(),alici.getID(),sohbetID1->{
            this.sohbetID = sohbetID1;
            System.out.println("çektim");
            mesajlaricek.run();
        });
    }

    public MesajlasmaYonetici(){
    }


    public void MesajGonder(String mesaj, MesajAdapter adapter){
        String mesajID = mesajlar.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("gonderen",gonderen.getID());
        veri.put("mesaj",mesaj);
        veri.put("zaman",System.currentTimeMillis());
        veri.put("goruldu",false);
        mesajlar.child(sohbetID).child(mesajID).setValue(veri);
        mesajlar.child(sohbetID).child("yaziyorMu").child(gonderen.getID()).setValue(false);
        mesajMap.put(mesajID,null);
        Mesaj yeniMesaj = new Mesaj(gonderen.getID(),mesaj,System.currentTimeMillis(),mesajID,false);
        adapter.getMesajArrayList().add(yeniMesaj);
        adapter.notifyItemInserted(adapter.getMesajArrayList().size()-1);
    }

    public void MesajlariCek(MesajAdapter adapter,int adet, ProgressBar yukleniyor, RecyclerView mesajkutucuklari, Runnable dinleme){
        mesajkutucuklari.setVisibility(View.GONE);
        yukleniyor.setVisibility(View.VISIBLE);
        System.out.println("ilk cekme");
        Query sonMesajlar = mesajlar.child(sohbetID)
                .orderByChild("zaman")
                .limitToLast(adet);
        sonMesajlar.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String mesajID = msgSnap.getKey();
                    if (mesajID.equals("yaziyorMu")) continue;
                    System.out.println("--+ "+ mesajID);
                    String mesajicerik = msgSnap.child("mesaj").getValue(String.class);
                    Long zaman = msgSnap.child("zaman").getValue(Long.class);
                    String gonderen = msgSnap.child("gonderen").getValue(String.class);
                    boolean goruldu = msgSnap.child("goruldu").getValue(Boolean.class);
                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID,goruldu);
                    Goruldu(mesaj,adapter);
                    adapter.getMesajArrayList().add(mesaj);
                    mesajMap.put(mesajID,null);
                }
                adapter.notifyDataSetChanged();
                yukleniyor.setVisibility(View.GONE);
                mesajkutucuklari.setVisibility(View.VISIBLE);
                System.out.println("ilkçekme bitti");
                dinleme.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Hata yönetimi
            }
        });
    }
    public void MesajlariCek(long enEskiZaman, MesajAdapter adapter, int adet, Runnable tamamdir){
        if(adapter.getMesajArrayList().size()<adet) return;
        System.out.println("aktif cekme");
        Query eskiMesajlar = mesajlar.child(sohbetID)
                .orderByChild("zaman")
                .endAt(enEskiZaman - 1)
                .limitToLast(adet);

        eskiMesajlar.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<Mesaj> yeniMesajlar = new ArrayList<>();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String mesajID = msgSnap.getKey();
                    if (mesajID.equals("yaziyorMu")) continue;
                    System.out.println("--- "+ mesajID);
                    String mesajicerik = msgSnap.child("mesaj").getValue(String.class);
                    String gonderen = msgSnap.child("gonderen").getValue(String.class);
                    Long zaman = msgSnap.child("zaman").getValue(Long.class);
                    boolean goruldu = msgSnap.child("goruldu").getValue(Boolean.class);
                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID,goruldu);
                    yeniMesajlar.add(mesaj);
                    Goruldu(mesaj,adapter);
                }
                adapter.getMesajArrayList().addAll(0, yeniMesajlar);
                adapter.notifyItemRangeInserted(0, yeniMesajlar.size()-1);
                tamamdir.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tamamdir.run();
            }
        });
    }

    private HashMap<String, Object> mesajMap = new HashMap<>();
    public void MesajlariDinle(MesajAdapter adapter, Runnable tamamdir){

        dinleyici = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String mesajID = snapshot.getKey();
                if (mesajID.equals("yaziyorMu")) return;
                String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                Long zaman = snapshot.child("zaman").getValue(Long.class);
                String gonderen = snapshot.child("gonderen").getValue(String.class);
                boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);
                System.out.println("dinleme");
                if (mesajMap.containsKey(mesajID)){
                    mesajMap.remove(mesajID);
                    return;
                }

                Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID,goruldu);
                adapter.getMesajArrayList().add(mesaj);
                adapter.notifyItemInserted(adapter.getMesajArrayList().size()-1);
                Goruldu(mesaj,adapter);
                System.out.println("tammadir");
                tamamdir.run();
                // Yeni mesajı listeye ekle ve ekranda göster
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                System.out.println("calisiyor2guncel");
                String mesajID = snapshot.getKey();
                if (mesajID.equals("yaziyorMu")) return;
                boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);
                for (int i=0; i<adapter.getMesajArrayList().size(); i++) {
                    if(adapter.getMesajArrayList().get(i).getMesajID().equals(mesajID)){
                        adapter.getMesajArrayList().get(i).setGoruldu(goruldu);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }

                // Gerekirse mesaj güncellenirse burası çalışır
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // Mesaj silinirse burası çalışır (gerekirse listeden çıkar)
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                // Hata yönetimi
            }
        };

        mesajlar.child(sohbetID)
                .orderByChild("zaman")
                .limitToLast(20)
                .addChildEventListener(dinleyici);
    }

    private interface SohbetIDCallback {
        void onResult(String sohbetID);
    }

    private void sohbetIDOlustur(String gonderen, String alici, SohbetIDCallback callback){
        String sohbetID = alici+"_"+gonderen;
        String sohbetID2 = gonderen+"_"+alici;

        DatabaseReference ref1 = mesajlar.child(sohbetID);
        DatabaseReference ref2 = mesajlar.child(sohbetID2);

        ref1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    callback.onResult(sohbetID);
                } else {
                    ref2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                callback.onResult(sohbetID2);
                            } else {
                                ref1.child("yaziyorMu").child(gonderen).setValue(false);
                                ref1.child("yaziyorMu").child(alici).setValue(false);
                                callback.onResult(sohbetID);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void ProfilCubugunuDoldur(TextView kisiAdiText, ImageView kisiProfilFoto,TextView durum){
        if(SohbetYonetici.getInstance().getKullanicilar().containsKey(alici.getID())){
            alici = (Kullanici) SohbetYonetici.getInstance().getKullanicilar().get(alici.getID());
        }
        if(alici.getKullaniciAdi()!=null){
            if(!alici.getKullaniciAdi().isEmpty()) {
                kisiAdiText.setText(alici.getKullaniciAdi());
                if (alici.isCevrimiciMi()) {
                    durum.setText("Çevrimiçi");
                    System.out.println("çevrimiçi");
                } else {
                    durum.setText("Son Görülme: "+alici.getSonGorulme());
                }
                if (alici.getFotoBitmap() != null) {
                    kisiProfilFoto.setImageBitmap(alici.getFotoBitmap());
                    return;
                } else if (alici.getFotoUrl() == null) {
                    kisiProfilFoto.setImageResource(R.drawable.kullanici);
                } else if (alici.getFotoUrl().isEmpty()) {
                    kisiProfilFoto.setImageResource(R.drawable.kullanici);
                    return;
                }
            }
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(alici.getID())
                .get()
                .addOnSuccessListener(veri ->{
                    if(veri.exists()){
                        alici.setAd(veri.getString("Ad"));
                        alici.setFotoUrl(veri.getString("profilFotoUrl"));
                        alici.setSoyad(veri.getString("Soyad"));
                        alici.setKullaniciAdi(veri.getString("KullaniciAdi"));
                        kisiAdiText.setText(alici.getKullaniciAdi());
                        Picasso.get()
                                .load(alici.getFotoUrl())
                                .placeholder(R.drawable.kullanici)
                                .error(R.drawable.kullanici)
                                .into(kisiProfilFoto);
                    }
                });

    }

    public void YaziyorDinleyici(TextView kisiDurumText){
        yaziyorDinleyici = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean yaziyor = snapshot.getValue(Boolean.class);
                if(yaziyor == null) return;
                if(yaziyor){
                    kisiDurumText.setText("Yazıyor...");
                }
                else{
                    if(!alici.isCevrimiciMi()){
                        kisiDurumText.setText("Son Görülme: "+alici.getSonGorulme());
                    }
                    else {
                        kisiDurumText.setText("Çevrimiçi");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                kisiDurumText.setText("Son Görülme: "+alici.getSonGorulme());
            }
        };

        mesajlar.child(sohbetID)
                .child("yaziyorMu")
                .child(alici.getID())
                .addValueEventListener(yaziyorDinleyici);
    }

    public void CevrimIciDinleyici(TextView kisiDurumText){
        cevrimiciDinleyici = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean cevrimiciMi = snapshot.child("cevrimici").getValue(Boolean.class);
                long sonGorulme = snapshot.child("sonGorulme").getValue(Long.class);
                alici.setCevrimiciMi(cevrimiciMi);
                alici.setSonGorulme(sonGorulme);
                if(cevrimiciMi){
                    kisiDurumText.setText("Çevrimiçi");
                }
                else{
                    kisiDurumText.setText("Son Görülme: "+alici.getSonGorulme());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        FirebaseDatabase.getInstance().getReference("durumlar")
                .child(alici.getID())
                .addValueEventListener(cevrimiciDinleyici);

    }

    private void Goruldu(Mesaj mesaj, MesajAdapter adapter){
        if(mesaj.isGoruldu()){
            adapter.notifyItemChanged(adapter.getMesajArrayList().indexOf(mesaj));
            return;
        }
        if(mesaj.getGonderici().equals(MainActivity.kullanici.getID())) return;
        mesajlar.child(sohbetID).child(mesaj.getMesajID()).child("goruldu").setValue(true)
                .addOnSuccessListener(basarili->{
                    System.out.println("calisiyor");
                    mesaj.setGoruldu(true);
                    adapter.notifyItemChanged(adapter.getMesajArrayList().indexOf(mesaj));
                });
    }

    public void MesajSil(String MesajID){
        mesajlar.child(sohbetID).child(MesajID).removeValue();
    }
    public void MesajGuncelle(String mesajID, String yeniMesaj){
        mesajlar.child(sohbetID).child(mesajID).child("mesaj").setValue(yeniMesaj);
    }


    public void DinleyiciKaldir(){
        if(dinleyici == null && yaziyorDinleyici==null && cevrimiciDinleyici==null){
            return;
        }
        if(yaziyorDinleyici != null){
            mesajlar.child(sohbetID).child("yaziyorMu").child(alici.getID()).removeEventListener(yaziyorDinleyici);
            yaziyorDinleyici = null;
        }
        if(cevrimiciDinleyici != null){
            FirebaseDatabase.getInstance().getReference("durumlar").child(alici.getID()).removeEventListener(cevrimiciDinleyici);
            cevrimiciDinleyici = null;
        }
        if(dinleyici == null) return;
        mesajlar.child(sohbetID).removeEventListener(dinleyici);
        dinleyici = null;
    }

    public void YaziyorMu(boolean yaziyor){
        mesajlar.child(sohbetID).child("yaziyorMu").child(gonderen.getID()).setValue(yaziyor);
    }

    public String getSohbetID() {
        return sohbetID;
    }

    public void setSohbetID(String sohbetID) {
        this.sohbetID = sohbetID;
    }

    public Kullanici getGonderen() {
        return gonderen;
    }

    public void setGonderen(Kullanici gonderen) {
        this.gonderen = gonderen;
    }

    public Kullanici getAlici() {
        return alici;
    }

    public void setAlici(Kullanici alici) {
        this.alici = alici;
    }
}
