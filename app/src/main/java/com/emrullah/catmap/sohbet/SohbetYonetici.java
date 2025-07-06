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
    private HashMap<String,Target> FotolariCek = new HashMap<>();
    private HashMap<String, ChildEventListener> dinleyiciler = new HashMap<>();
    private HashMap<String, Mesaj> gorulmemisMesajlar = new HashMap<>();

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
                                tamamdir.run();
                                Kullanicilar.put(sohbet.getAlici().getID(),sohbet.getAlici());
                                FotolariCek(sohbet,tamamdir);
                            }
                        });
            }
            SonGorulmeCevrimIci(sohbet);
            if (dinleyiciler.containsKey(sohbet.getSohbetID())){
                sohbetDB.child(sohbet.getSohbetID()).removeEventListener(dinleyiciler.get(sohbet.getSohbetID()));
            }
            ChildEventListener dinleyici = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    System.out.println("bende çalıştım-"+snapshot.getChildrenCount());
                    String mesajID = snapshot.getKey();
                    if(mesajID.equals("yaziyorMu")) return;
                    String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                    Long zaman = snapshot.child("zaman").getValue(Long.class);
                    String gonderen = snapshot.child("gonderen").getValue(String.class);
                    boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);
                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID,goruldu);
                    sohbet.setMesaj(mesaj);
                    yeniGelenGorulmemisMesajlarSayisi(sohbet);

                    SonMesajlar.put(sohbet.getAlici().getID(),mesaj);
                    if(sohbet.isSohbetYuklendiMi()){
                        Sirala(sohbetArrayList);
                        tamamdir.run();
                    }
                    System.out.println(SonMesajlar.size());
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
                    .limitToLast(20)
                    .addChildEventListener(dinleyici);
        }
    }

    private void FotolariCek(Sohbet sohbet, Runnable tamamdir){
        if(sohbet.getAlici().getFotoUrl() == null || sohbet.getAlici().getFotoUrl().isEmpty()){
            tamamdir.run();
            sohbet.setSohbetYuklendiMi(true);
            return;
        }
        if(ProfilFotolari.containsKey(sohbet.getAlici().getFotoUrl())){
            sohbet.getAlici().setFotoBitmap((Bitmap) ProfilFotolari.get(sohbet.getAlici().getFotoUrl()));
            tamamdir.run();
            sohbet.setSohbetYuklendiMi(true);
            return;
        }
        if(sohbet.getAlici().getFotoBitmap() != null){
            ProfilFotolari.put(sohbet.getAlici().getFotoUrl(),sohbet.getAlici().getFotoBitmap());
            sohbet.setSohbetYuklendiMi(true);
            tamamdir.run();
            return;
        }
        Picasso.get()
                .load(sohbet.getAlici().getFotoUrl())
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(FotografTargetHaziriligi(sohbet,tamamdir));
    }

    private void SonGorulmeCevrimIci(Sohbet sohbet){
        FirebaseDatabase.getInstance().getReference("durumlar")
                .child(sohbet.getAlici().getID())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean cevrimici = snapshot.child("cevrimici").getValue(Boolean.class);
                        long sonGorulme = snapshot.child("sonGorulme").getValue(Long.class);
                        sohbet.getAlici().setCevrimiciMi(cevrimici);
                        sohbet.getAlici().setSonGorulme(sonGorulme);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void yeniGelenGorulmemisMesajlarSayisi(Sohbet sohbet){
        if(sohbet.getMesaj().getGonderici().equals(sohbet.getAlici().getID())){
            if(sohbet.getMesaj().isGoruldu()){
                sohbet.setOkunmamisMesajSayisi(0);
            }
            else {
                sohbet.setOkunmamisMesajSayisi(sohbet.getOkunmamisMesajSayisi()+1);
            }
        }
    }

    private Target FotografTargetHaziriligi(Sohbet sohbet, Runnable tamamdir){
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                sohbet.getAlici().setFotoBitmap(bitmap);
                ProfilFotolari.put(sohbet.getAlici().getFotoUrl(),bitmap);
                FotolariCek.remove(sohbet.getAlici().getFotoUrl());
                sohbet.setSohbetYuklendiMi(true);
                tamamdir.run();
            }
            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                sohbet.getAlici().setFotoBitmap(null);
                sohbet.setSohbetYuklendiMi(true);
                tamamdir.run();
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                sohbet.getAlici().setFotoBitmap(null);
                sohbet.setSohbetYuklendiMi(true);
                tamamdir.run();
            }
        };
        FotolariCek.put(sohbet.getAlici().getFotoUrl(),target);
        return  target;
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
    public HashMap getKullanicilar() {
        return Kullanicilar;
    }

    private void Sirala(ArrayList<Sohbet> sohbetler){
        for(int i=0; i<sohbetler.size(); i++){
            long zaman = sohbetler.get(i).getMesaj().getLongZaman();
            Sohbet sohbet = sohbetler.get(i);
            int ink = i;
            for(int j=i+1; j<sohbetler.size(); j++){
                if(zaman < sohbetler.get(j).getMesaj().getLongZaman()){
                    zaman = sohbetler.get(j).getMesaj().getLongZaman();
                    ink = j;
                }
            }
            if(ink != i) {
                sohbetler.set(i, sohbetler.get(ink));
                sohbetler.set(ink, sohbet);
            }
        }
    }
}
