package com.emrullah.catmap;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class YorumActivity extends AppCompatActivity {
    private RecyclerView yorumlarRecyclerView;
    private YorumAdapter yorumAdapter;
    private List<Yorumlar> yorumListesi;
    private ListenerRegistration listenerRegistration; // Dinleyici nesnesi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yorum_gosterme); // Yorumlar için Activity Layout

        yorumlarRecyclerView = findViewById(R.id.yorumlarRecyclerView);

        yorumListesi = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore'dan yorumlar koleksiyonunu dinlemek için addSnapshotListener kullanıyoruz
        listenerRegistration = db.collection("yorumlar")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("Error", "Yorumlar verisi dinlenemedi", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<Yorumlar> yorumlar = new ArrayList<>();

                        // Her bir yorum dokümanı için işlem yapıyoruz
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String kullaniciID = doc.getString("kullanici_id");
                            String yorumIcerigi = doc.getString("icerik");
                            Timestamp tarih = doc.getTimestamp("tarih");

                            // Kullanıcı adı almak için "users" koleksiyonuna sorgu gönder
                            db.collection("users")
                                    .document(kullaniciID)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String kullaniciAd = userDoc.getString("Ad");  // "Ad" alanını alıyoruz
                                            Yorumlar yorum = new Yorumlar(yorumIcerigi, tarih, kullaniciAd);
                                            yorumlar.add(yorum);

                                            // Yorumlar tamamen yüklendiyse, RecyclerView'u güncelle
                                            if (yorumlar.size() == queryDocumentSnapshots.size()) {
                                                yorumListesi.clear();
                                                yorumListesi.addAll(yorumlar);
                                                // Adapter oluşturma ve RecyclerView'a bağlama
                                                yorumAdapter = new YorumAdapter(yorumListesi, YorumActivity.this);
                                                yorumlarRecyclerView.setLayoutManager(new LinearLayoutManager(YorumActivity.this));
                                                yorumlarRecyclerView.setAdapter(yorumAdapter);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e1 -> {
                                        Log.e("Error", "Kullanıcı verisi alınamadı", e1);
                                    });
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dinleyiciyi serbest bırakmayı unutma
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
