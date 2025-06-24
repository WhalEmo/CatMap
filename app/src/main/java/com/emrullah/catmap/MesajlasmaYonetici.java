package com.emrullah.catmap;

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

    public MesajlasmaYonetici(String gonderen, String alici) {
        this.gonderen = gonderen;
        this.alici = alici;
        this.sohbetID = alici+"_"+gonderen;
    }

    public void MesajGonder(String gonderen, String mesaj,MesajAdapter adapter){
        String mesajID = mesajlar.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("gonderen",gonderen);
        veri.put("mesaj",mesaj);
        veri.put("zaman",System.currentTimeMillis());
        mesajlar.child(sohbetID).child(mesajID).setValue(veri);
        Mesaj yeniMesaj = new Mesaj(gonderen,alici,mesaj,System.currentTimeMillis(),mesajID);
        adapter.getMesajArrayList().add(yeniMesaj);
        adapter.notifyItemInserted(adapter.getMesajArrayList().size()-1);
    }

    private void MesajlariCek(ArrayList<Mesaj> mesajArrayList,int adet){
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

                    Mesaj mesaj = new Mesaj(gonderen, alici, mesajicerik, zaman, mesajID);
                    mesajArrayList.add(mesaj);
                }
                // RecyclerView ya da adapter'e bildir
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Hata yönetimi
            }
        });
    }
    private void MesajlariCek(long enEskiZaman, ArrayList<Mesaj> mesajArrayList, int adet){
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
                    Long zaman = msgSnap.child("zaman").getValue(Long.class);

                    Mesaj mesaj = new Mesaj(gonderen, alici, mesajicerik, zaman, mesajID);
                    yeniMesajlar.add(mesaj);
                }
                mesajArrayList.addAll(0, yeniMesajlar);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void MesajlariDinle(ArrayList<Mesaj> mesajArrayList){
        mesajlar.child(sohbetID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String mesajID = snapshot.getKey();
                String mesajicerik = snapshot.child("mesaj").getValue(String.class);
                Long zaman = snapshot.child("zaman").getValue(Long.class);

                Mesaj mesaj = new Mesaj(gonderen, alici, mesajicerik, zaman, mesajID);
                mesajArrayList.add(mesaj);
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
