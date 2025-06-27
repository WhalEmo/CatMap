package com.emrullah.catmap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

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


    public void SohbetleriCek(ArrayList<Sohbet> sohbetArrayList){
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
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SohbetNesneleriniOlustur(ArrayList<Sohbet> sohbetArrayList){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for(Sohbet sohbet: sohbetArrayList){
            db.collection("users")
                    .document(sohbet.getAlici().getID())
                    .get()
                    .addOnSuccessListener(veri ->{
                        if(veri.exists()){
                            sohbet.getAlici().setAd(veri.getString("Ad"));
                            sohbet.getAlici().setFotoUrl(veri.getString("profilFotoUrl"));
                            sohbet.getAlici().setSoyad(veri.getString("Soyad"));
                            sohbet.getAlici().setKullaniciAdi(veri.getString("KullaniciAdi"));
                            if(sohbet.getAlici().getFotoUrl() != null && !sohbet.getAlici().getFotoUrl().isEmpty()){
                                FotolariCek(sohbet);
                            }
                        }
                    });
            sohbetDB.child(sohbet.getSohbetID())
                    .orderByChild("zaman")
                    .limitToLast(1)
                    .addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                            String mesajID = snapshot.getKey();
                            String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                            Long zaman = snapshot.child("zaman").getValue(Long.class);
                            String gonderen = snapshot.child("gonderen").getValue(String.class);
                            Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID);
                            sohbet.setMesaj(mesaj);
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
                    });
        }
    }

    private void FotolariCek(Sohbet sohbet){
        if(ProfilFotolari.containsKey(sohbet.getAlici().getFotoUrl())){
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

}
