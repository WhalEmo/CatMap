package com.emrullah.catmap.sohbet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.Mesaj;
import com.emrullah.catmap.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;

public class SohbetYonetici {
    private DatabaseReference sohbetDB = FirebaseDatabase.getInstance().getReference("mesajlar");
    private HashMap<String,Object> ProfilFotolari;
    private HashMap<String,Kullanici> Kullanicilar;
    private HashMap<String,Mesaj> SonMesajlar;
    private static SohbetYonetici yonetici;
    private HashMap<String, ChildEventListener> dinleyiciler = new HashMap<>();

    public static SohbetYonetici getInstance(){
        if(yonetici == null){
            yonetici = new SohbetYonetici();
        }
        return yonetici;
    }


    public SohbetYonetici(){}


    public void SohbetleriCek(ArrayList<Sohbet> sohbetArrayList, Runnable tamamdir){
        sohbetDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot sohbet: snapshot.getChildren()){
                    String sohbetID = sohbet.getKey();
                    if(sohbetID.split("_")[0].equals(MainActivity.kullanici.getID())){
                        Kullanici alici = new Kullanici();
                        alici.setID(sohbetID.split("_")[1]);
                        Sohbet sohbet1 = new Sohbet(sohbetID, alici, new Mesaj());
                        sohbetArrayList.add(sohbet1);
                    }
                    if(sohbetID.split("_")[1].equals(MainActivity.kullanici.getID())){
                        Kullanici alici = new Kullanici();
                        alici.setID(sohbetID.split("_")[0]);
                        Sohbet sohbet1 = new Sohbet(sohbetID, alici,null);
                        sohbetArrayList.add(sohbet1);
                    }
                }
                SohbetNesneleriniOlustur(sohbetArrayList,tamamdir);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SohbetNesneleriniOlustur(ArrayList<Sohbet> sohbetArrayList, Runnable tamamdir){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for(Sohbet sohbet: sohbetArrayList){
            if(Kullanicilar.containsKey(sohbet.getAlici().getID())){
                sohbet.setAlici(Kullanicilar.get(sohbet.getAlici().getID()));
                FotolariCek(sohbet,tamamdir);
                sohbet.setMesaj(SonMesajlar.get(sohbet.getAlici().getID()));
                tamamdir.run();
            }
            else{
                db.collection("users")
                        .document(sohbet.getAlici().getID())
                        .get()
                        .addOnSuccessListener(veri ->{
                            if(veri.exists()){
                                sohbet.getAlici().setAd(veri.getString("Ad"));
                                sohbet.getAlici().setFotoUrl(veri.getString("profilFotoUrl"));
                                sohbet.getAlici().setSoyad(veri.getString("Soyad"));
                                sohbet.getAlici().setKullaniciAdi(veri.getString("KullaniciAdi"));
                                FotolariCek(sohbet,tamamdir);
                                Kullanicilar.put(sohbet.getAlici().getID(),sohbet.getAlici());
                                tamamdir.run();
                            }
                        });
            }
            if (dinleyiciler.containsKey(sohbet.getSohbetID())){
                tamamdir.run();
                sohbetDB.child(sohbet.getSohbetID()).removeEventListener(dinleyiciler.get(sohbet.getSohbetID()));
            }
            ChildEventListener dinleyici = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    System.out.println("bende çalıştım");
                    String mesajID = snapshot.getKey();
                    String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                    Long zaman = snapshot.child("zaman").getValue(Long.class);
                    String gonderen = snapshot.child("gonderen").getValue(String.class);
                    boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);
                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID,goruldu);
                    sohbet.setMesaj(mesaj);
                    SonMesajlar.put(sohbet.getAlici().getID(),mesaj);
                    System.out.println(SonMesajlar.size());
                    tamamdir.run();
                    // Yeni mesajı listeye ekle ve ekranda göster
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
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
            dinleyiciler.put(sohbet.getSohbetID(),dinleyici);
            sohbetDB.child(sohbet.getSohbetID())
                    .orderByChild("zaman")
                    .limitToLast(1)
                    .addChildEventListener(dinleyici);
        }
    }

    private void FotolariCek(Sohbet sohbet, Runnable tamamdir){
        if(sohbet.getAlici().getFotoUrl() == null || sohbet.getAlici().getFotoUrl().isEmpty()){
            return;
        }
        if(ProfilFotolari.containsKey(sohbet.getAlici().getFotoUrl())){
            sohbet.getAlici().setFotoBitmap((Bitmap) ProfilFotolari.get(sohbet.getAlici().getFotoUrl()));
            tamamdir.run();
            return;
        }
        Picasso.get()
                .load(sohbet.getAlici().getFotoUrl())
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        sohbet.getAlici().setFotoBitmap(bitmap);
                        ProfilFotolari.put(sohbet.getAlici().getFotoUrl(),bitmap);
                        tamamdir.run();
                    }
                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        sohbet.getAlici().setFotoBitmap(null);
                    }
                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        sohbet.getAlici().setFotoBitmap(null);
                    }
                });
    }
    public void setSonMesajlar(HashMap<String, Mesaj> sonMesajlar) {
        SonMesajlar = sonMesajlar;
    }

    public void setProfilFotolari(HashMap<String, Object> profilFotolari) {
        ProfilFotolari = profilFotolari;
    }

    public void setKullanicilar(HashMap<String, Kullanici> kullanicilar) {
        Kullanicilar = kullanicilar;
    }
}
