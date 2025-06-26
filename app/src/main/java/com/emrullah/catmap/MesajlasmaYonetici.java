package com.emrullah.catmap;

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MesajlasmaYonetici {
    private DatabaseReference mesajlar = FirebaseDatabase.getInstance().getReference("mesajlar");

    private String sohbetID;
    private String gonderen;
    private String alici;

    public MesajlasmaYonetici(String gonderen, String alici,Runnable mesajlaricek) {
        this.gonderen = gonderen;
        this.alici = alici;
        sohbetIDOlustur(gonderen,alici,sohbetID1->{
            this.sohbetID = "A8mt0DjcK1oulvcZFWtU_CJl7rX5pUF2BzDUI9sLl";
            System.out.println("çektim");
            mesajlaricek.run();
        });
    }


    public void MesajGonder(String gonderen, String mesaj, MesajAdapter adapter){
        String mesajID = mesajlar.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("gonderen",gonderen);
        veri.put("mesaj",mesaj);
        veri.put("zaman",System.currentTimeMillis());
        mesajlar.child(sohbetID).child(mesajID).setValue(veri);
        mesajMap.put(mesajID,null);
        Mesaj yeniMesaj = new Mesaj(gonderen,mesaj,System.currentTimeMillis(),mesajID);
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
                    String mesajicerik = msgSnap.child("mesaj").getValue(String.class);
                    Long zaman = msgSnap.child("zaman").getValue(Long.class);
                    String gonderen = msgSnap.child("gonderen").getValue(String.class);
                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID);
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
                    String mesajicerik = msgSnap.child("mesaj").getValue(String.class);
                    String gonderen = msgSnap.child("gonderen").getValue(String.class);
                    Long zaman = msgSnap.child("zaman").getValue(Long.class);

                    Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID);
                    yeniMesajlar.add(mesaj);
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
        mesajlar.child(sohbetID)
                .orderByChild("zaman")
                .limitToLast(1)
                .addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String mesajID = snapshot.getKey();
                String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                Long zaman = snapshot.child("zaman").getValue(Long.class);
                String gonderen = snapshot.child("gonderen").getValue(String.class);
                System.out.println("dinleme");
                if (mesajMap.containsKey(mesajID)){
                    mesajMap.remove(mesajID);
                    return;
                }

                Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman, mesajID);
                adapter.getMesajArrayList().add(mesaj);
                adapter.notifyItemInserted(adapter.getMesajArrayList().size()-1);
                System.out.println("tammadir");
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
        });
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

}
