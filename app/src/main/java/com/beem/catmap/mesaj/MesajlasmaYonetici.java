package com.beem.catmap.mesaj;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.data.model.UserModel;
import com.beem.catmap.R;
import com.beem.catmap.data.local.UserSession;
import com.beem.catmap.sohbet.SohbetYonetici;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MesajlasmaYonetici {
    private DatabaseReference mesajlar = FirebaseDatabase.getInstance().getReference("mesajlar");

    private String sohbetID;
    private UserModel gonderen = UserSession.INSTANCE.getUserModel();
    private UserModel alici;
    private static MesajlasmaYonetici yonetici;
    private ChildEventListener dinleyici;
    private ValueEventListener yaziyorDinleyici;
    private ValueEventListener cevrimiciDinleyici;
    private ChildEventListener silDinleyici;
    private ChildEventListener guncellemeDinleyici;
    private ValueEventListener engellenenDinleyici;
    private GenericTypeIndicator<ArrayList<String>> type = new GenericTypeIndicator<ArrayList<String>>() {};
    private Runnable geriDon;
    private boolean engelledim = false;
    private boolean engelledi = false;

    public static MesajlasmaYonetici getInstance() {
        if (yonetici == null) {
            yonetici = new MesajlasmaYonetici();
        }
        return yonetici;
    }

    public void MesajlasmaYoneticiStart(Runnable mesajlaricek) {
        if (alici == null || alici.id == null || gonderen == null || gonderen.id == null) {
            if (geriDon != null) geriDon.run();
            return;
        }
        sohbetIDOlustur(gonderen.id, alici.id, sohbetID1 -> {
            this.sohbetID = sohbetID1;
            System.out.println("çektim");
            if (mesajlaricek != null) mesajlaricek.run();
        });
    }

    public MesajlasmaYonetici() {
    }

    public void MesajGonder(String mesaj, MesajAdapter adapter) {
        if (sohbetID == null) return;
        String mesajID = mesajlar.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("gonderen", gonderen.id);
        veri.put("mesaj", mesaj);
        veri.put("zaman", System.currentTimeMillis());
        veri.put("goruldu", false);
        veri.put("tur", "metin");
        mesajlar.child(sohbetID).child("anaMesaj").child(mesajID).setValue(veri);
        mesajlar.child(sohbetID).child("yaziyorMu").child(gonderen.id).setValue(false);
        mesajMap.put(mesajID, null);
        Mesaj yeniMesaj = new Mesaj(gonderen.id, mesaj, System.currentTimeMillis(), mesajID, false);
        yeniMesaj.setTur("metin");
        adapter.getMesajArrayList().add(yeniMesaj);
        adapter.notifyItemInserted(adapter.getMesajArrayList().size() - 1);
    }

    public void MesajGonder(Mesaj yanitlananMesaj, String mesaj, MesajAdapter adapter) {
        if (sohbetID == null) return;
        String mesajID = mesajlar.push().getKey();
        YanitMesaj yanit = new YanitMesaj(gonderen.id, mesaj, System.currentTimeMillis(), mesajID, false, yanitlananMesaj);
        if ("foto".equals(yanitlananMesaj.getTur())) yanitlananMesaj.setMesaj("\uD83D\uDCF7  Fotoğraf");
        mesajMap.put(mesajID, null);
        mesajlar.child(sohbetID).child("anaMesaj").child(mesajID).setValue(yanit);
        adapter.getMesajArrayList().add(yanit);
        adapter.notifyItemInserted(adapter.getMesajArrayList().size() - 1);
    }

    public void MesajlariCek(MesajAdapter adapter, int adet, ProgressBar yukleniyor, RecyclerView mesajkutucuklari, Runnable dinleme) {
        if (sohbetID == null) return;
        mesajkutucuklari.setVisibility(View.GONE);
        yukleniyor.setVisibility(View.VISIBLE);
        System.out.println("ilk cekme");
        Query sonMesajlar = mesajlar.child(sohbetID).child("anaMesaj")
                .orderByChild("zaman")
                .limitToLast(adet);
        sonMesajlar.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Mesaj mesaj = MesajOlustur(msgSnap);
                    if (mesaj != null) {
                        Goruldu(mesaj, adapter);
                        adapter.getMesajArrayList().add(mesaj);
                        mesajMap.put(mesaj.getMesajID(), null);
                    }
                }
                adapter.notifyDataSetChanged();
                yukleniyor.setVisibility(View.GONE);
                mesajkutucuklari.setVisibility(View.VISIBLE);
                System.out.println("ilkçekme bitti");
                if (dinleme != null) dinleme.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                yukleniyor.setVisibility(View.GONE);
                mesajkutucuklari.setVisibility(View.VISIBLE);
            }
        });
    }

    public void MesajlariCek(long enEskiZaman, MesajAdapter adapter, int adet, Runnable tamamdir) {
        if (adapter.getMesajArrayList().size() < adet || sohbetID == null) return;
        System.out.println("aktif cekme");
        Query eskiMesajlar = mesajlar.child(sohbetID).child("anaMesaj")
                .orderByChild("zaman")
                .endAt(enEskiZaman - 1)
                .limitToLast(adet);

        eskiMesajlar.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<Mesaj> yeniMesajlar = new ArrayList<>();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Mesaj mesaj = MesajOlustur(msgSnap);
                    if (mesaj != null) {
                        yeniMesajlar.add(mesaj);
                        Goruldu(mesaj, adapter);
                    }
                }
                adapter.getMesajArrayList().addAll(0, yeniMesajlar);
                adapter.notifyItemRangeInserted(0, yeniMesajlar.size());
                if (tamamdir != null) tamamdir.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (tamamdir != null) tamamdir.run();
            }
        });
    }

    private HashMap<String, Object> mesajMap = new HashMap<>();

    public void MesajlariDinle(MesajAdapter adapter, Runnable tamamdir) {
        System.out.println("dinleyici");
        dinleyici = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String mesajID = snapshot.getKey();
                if (mesajMap.containsKey(mesajID)) return;
                Mesaj mesaj = MesajOlustur(snapshot);
                if (mesaj != null) {
                    mesajMap.put(mesajID, null);
                    adapter.getMesajArrayList().add(mesaj);
                    adapter.notifyItemInserted(adapter.getMesajArrayList().size() - 1);
                    Goruldu(mesaj, adapter);
                    if (tamamdir != null) tamamdir.run();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                String mesajID = snapshot.getKey();
                if ("yaziyorMu".equals(mesajID)) return;
                Boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);
                if (goruldu == null) return;
                for (int i = 0; i < adapter.getMesajArrayList().size(); i++) {
                    if (adapter.getMesajArrayList().get(i).getMesajID().equals(mesajID)) {
                        adapter.getMesajArrayList().get(i).setGoruldu(goruldu);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        if (sohbetID == null) {
            sohbetIDOlustur(gonderen.id, alici.id, sohbetID1 -> {
                this.sohbetID = sohbetID1;
                mesajlar.child(sohbetID).child("anaMesaj")
                        .orderByChild("zaman")
                        .limitToLast(20)
                        .addChildEventListener(dinleyici);
            });
        } else {
            mesajlar.child(sohbetID).child("anaMesaj")
                    .orderByChild("zaman")
                    .limitToLast(20)
                    .addChildEventListener(dinleyici);
        }
    }

    public interface SohbetIDCallback {
        void onResult(String sohbetID);
    }

    public void sohbetIDOlustur(String gonderenId, String aliciId, SohbetIDCallback callback) {
        String sohbetID1 = aliciId + "_" + gonderenId;
        String sohbetID2 = gonderenId + "_" + aliciId;

        DatabaseReference ref1 = mesajlar.child(sohbetID1);
        DatabaseReference ref2 = mesajlar.child(sohbetID2);

        ref1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    callback.onResult(sohbetID1);
                } else {
                    ref2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                callback.onResult(sohbetID2);
                            } else {
                                ref1.child("yaziyorMu").child(gonderenId).setValue(false);
                                ref1.child("yaziyorMu").child(aliciId).setValue(false);
                                callback.onResult(sohbetID1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void ProfilCubugunuDoldur(TextView kisiAdiText, ImageView kisiProfilFoto, TextView durum) {
        if (alici == null || alici.id == null) {
            if (geriDon != null) geriDon.run();
            return;
        }
        if (SohbetYonetici.getInstance().getKullanicilar().containsKey(alici.id)) {
            alici = (UserModel) SohbetYonetici.getInstance().getKullanicilar().get(alici.id);
        }
        if (alici.username != null && !alici.username.isEmpty()) {
            kisiAdiText.setText(alici.username);
            if (alici.isOnline) {
                durum.setText("Çevrimiçi");
            } else {
                durum.setText("Son Görülme: " + alici.lastSeen);
            }
            if (alici.photoBitmap != null) {
                kisiProfilFoto.setImageBitmap(alici.photoBitmap);
                return;
            } else if (alici.photoUrl == null || alici.photoUrl.isEmpty()) {
                kisiProfilFoto.setImageResource(R.drawable.kullanici);
                return;
            }
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(alici.id)
                .get()
                .addOnSuccessListener(veri -> {
                    if (veri.exists()) {
                        alici.name = veri.getString("Ad");
                        alici.photoUrl = veri.getString("profilFotoUrl");
                        alici.surname = veri.getString("Soyad");
                        alici.username = veri.getString("KullaniciAdi");
                        kisiAdiText.setText(alici.username);
                        if (engelledim || engelledi) return;

                        if (alici.photoUrl != null && !alici.photoUrl.isEmpty()) {
                            Picasso.get()
                                    .load(alici.photoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .error(R.drawable.kullanici)
                                    .into(kisiProfilFoto);
                        } else {
                            kisiProfilFoto.setImageResource(R.drawable.kullanici);
                        }
                    }
                });
    }

    public void YaziyorDinleyici(TextView kisiDurumText) {
        if (sohbetID == null) return;
        yaziyorDinleyici = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean yaziyor = snapshot.getValue(Boolean.class);
                if (yaziyor == null) return;
                if (yaziyor) {
                    kisiDurumText.setText("Yazıyor...");
                } else {
                    if (!alici.isOnline) {
                        kisiDurumText.setText("Son Görülme: " + alici.lastSeen);
                    } else {
                        kisiDurumText.setText("Çevrimiçi");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                kisiDurumText.setText("Son Görülme: " + alici.lastSeen);
            }
        };

        mesajlar.child(sohbetID)
                .child("yaziyorMu")
                .child(alici.id)
                .addValueEventListener(yaziyorDinleyici);
    }

    public void CevrimIciDinleyici(TextView kisiDurumText) {
        if (alici == null || alici.id == null) return;
        cevrimiciDinleyici = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean cevrimiciMi = snapshot.child("cevrimici").getValue(Boolean.class);
                Long sonGorulme = snapshot.child("sonGorulme").getValue(Long.class);
                alici.isOnline = cevrimiciMi != null && cevrimiciMi;
                alici.lastSeen = sonGorulme != null ? sonGorulme : 0L;

                if (alici.isOnline) {
                    kisiDurumText.setText("Çevrimiçi");
                } else {
                    kisiDurumText.setText("Son Görülme: " + alici.lastSeen);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseDatabase.getInstance().getReference("durumlar")
                .child(alici.id)
                .addValueEventListener(cevrimiciDinleyici);
    }

    public void SilDinleyici(MesajAdapter adapter) {
        if (sohbetID == null) return;
        silDinleyici = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String mesajID = snapshot.getValue(String.class);
                if (mesajID == null) return;
                for (int i = 0; i < adapter.getMesajArrayList().size(); i++) {
                    if (adapter.getMesajArrayList().get(i).getMesajID().equals(mesajID)) {
                        adapter.getMesajArrayList().remove(i);
                        adapter.notifyItemRemoved(i);
                        mesajlar.child(sohbetID).child("anaMesaj").child(mesajID).removeValue();
                        break;
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mesajlar.child(sohbetID).child("silMesaj")
                .limitToLast(10)
                .addChildEventListener(silDinleyici);
    }

    public void GuncelleDinleyici(MesajAdapter adapter) {
        if (sohbetID == null) return;
        guncellemeDinleyici = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String yeniMesajID = snapshot.child("ID").getValue(String.class);
                String yeniMesaj = snapshot.child("mesaj").getValue(String.class);
                if (yeniMesajID == null || yeniMesaj == null) return;

                for (int i = 0; i < adapter.getMesajArrayList().size(); i++) {
                    if (adapter.getMesajArrayList().get(i).getMesajID().equals(yeniMesajID)) {
                        adapter.getMesajArrayList().get(i).setMesaj(yeniMesaj);
                        adapter.notifyItemChanged(i);
                        yolla(yeniMesajID, yeniMesaj);
                        break;
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mesajlar.child(sohbetID).child("gunMesaj")
                .limitToLast(10)
                .addChildEventListener(guncellemeDinleyici);
    }

    private void Goruldu(Mesaj mesaj, MesajAdapter adapter) {
        if (mesaj == null || sohbetID == null) return;
        if (mesaj.isGoruldu()) {
            adapter.notifyItemChanged(adapter.getMesajArrayList().indexOf(mesaj));
            return;
        }
        if (!mesaj.getGonderici().equals(UserSession.INSTANCE.getUserId())) {
            mesajlar.child(sohbetID).child("anaMesaj").child(mesaj.getMesajID()).child("goruldu").setValue(true)
                    .addOnSuccessListener(basarili -> {
                        mesaj.setGoruldu(true);
                        int index = adapter.getMesajArrayList().indexOf(mesaj);
                        if (index != -1) {
                            adapter.notifyItemChanged(index);
                        }
                    });
        }
    }

    private HashMap<String, Mesaj> silinenMesajlar = new HashMap<>();

    public void MesajSil(String MesajID) {
        if (sohbetID == null) return;
        silinenMesajlar.put(MesajID, null);
        mesajlar.child(sohbetID).child("silMesaj").push().setValue(MesajID);
    }

    public void MesajGuncelle(String mesajID, String yeniMesaj) {
        if (sohbetID == null) return;
        Map<String, Object> veri = new HashMap<>();
        veri.put("mesaj", yeniMesaj);
        veri.put("ID", mesajID);
        mesajlar.child(sohbetID).child("gunMesaj").push().setValue(veri);
    }

    private Mesaj MesajOlustur(DataSnapshot snapshot) {
        String tur = snapshot.child("tur").getValue(String.class);
        if (tur == null) return null;

        if ("metin".equals(tur)) {
            String mesajID = snapshot.getKey();
            String gonderen = snapshot.child("gonderen").getValue(String.class);
            Long zaman = snapshot.child("zaman").getValue(Long.class);
            String mesajicerik = snapshot.child("mesaj").getValue(String.class);
            Boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);

            Mesaj mesaj = new Mesaj(gonderen, mesajicerik, zaman != null ? zaman : 0L, mesajID, false);
            mesaj.setTur(tur);
            mesaj.setGoruldu(goruldu != null && goruldu);
            return mesaj;
        } else if ("yanit".equals(tur)) {
            return snapshot.getValue(YanitMesaj.class);
        } else {
            String mesajID = snapshot.getKey();
            String gonderen = snapshot.child("gonderen").getValue(String.class);
            Long zaman = snapshot.child("zaman").getValue(Long.class);
            ArrayList<String> fotoUrl = snapshot.child("fotoUrlleri").getValue(type);
            Boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);

            Mesaj mesaj = new Mesaj(gonderen, fotoUrl, zaman != null ? zaman : 0L, mesajID, false);
            mesaj.setTur(tur);
            mesaj.setGoruldu(goruldu != null && goruldu);
            mesaj.setYuklendiMi(true);
            return mesaj;
        }
    }

    public void EngelleDinleyici(Runnable engellendin, Runnable engelAcildi) {
        if (sohbetID == null || alici == null || alici.id == null) return;
        engellenenDinleyici = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean engel = snapshot.getValue(Boolean.class);
                engelledi = engel != null && engel;

                if (engelledi && engellendin != null) {
                    engellendin.run();
                } else if (!(engelledim || engelledi) && engelAcildi != null) {
                    engelAcildi.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mesajlar.child(sohbetID)
                .child("engelliMi")
                .child(alici.id)
                .addValueEventListener(engellenenDinleyici);
    }

    public void DinleyiciKaldir() {
        if (sohbetID == null) return;
        if (yaziyorDinleyici != null && alici != null && alici.id != null) {
            mesajlar.child(sohbetID).child("yaziyorMu").child(alici.id).removeEventListener(yaziyorDinleyici);
            yaziyorDinleyici = null;
        }
        if (cevrimiciDinleyici != null && alici != null && alici.id != null) {
            FirebaseDatabase.getInstance().getReference("durumlar").child(alici.id).removeEventListener(cevrimiciDinleyici);
            cevrimiciDinleyici = null;
        }
        if (silDinleyici != null) {
            mesajlar.child(sohbetID).child("silMesaj").removeEventListener(silDinleyici);
            silDinleyici = null;
        }
        if (guncellemeDinleyici != null) {
            mesajlar.child(sohbetID).child("gunMesaj").removeEventListener(guncellemeDinleyici);
            guncellemeDinleyici = null;
        }
        if (engellenenDinleyici != null && alici != null && alici.id != null) {
            mesajlar.child(sohbetID).child("engelliMi").child(alici.id).removeEventListener(engellenenDinleyici);
            engellenenDinleyici = null;
        }
        if (dinleyici != null) {
            mesajlar.child(sohbetID).child("anaMesaj").removeEventListener(dinleyici);
            dinleyici = null;
        }
    }

    public void YaziyorMu(boolean yaziyor) {
        if (sohbetID == null || gonderen == null || gonderen.id == null) return;
        mesajlar.child(sohbetID).child("yaziyorMu").child(gonderen.id).setValue(yaziyor);
    }

    public String getSohbetID() {
        return sohbetID;
    }

    public void setSohbetID(String sohbetID) {
        this.sohbetID = sohbetID;
    }

    public UserModel getGonderen() {
        return gonderen;
    }

    public void setGonderen(UserModel gonderen) {
        this.gonderen = gonderen;
    }

    public UserModel getAlici() {
        return alici;
    }

    public void setAlici(UserModel alici) {
        this.alici = alici;
        engelledi = false;
        engelledim = false;
    }

    private void yolla(String mesajID, String yeniMesaj) {
        if (sohbetID == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("mesajlar/" + sohbetID + "/anaMesaj/" + mesajID + "/mesaj", yeniMesaj);
        ref.updateChildren(updateMap);
    }

    public HashMap<String, Object> getMesajMap() {
        return mesajMap;
    }

    public void setGeriDon(Runnable geriDon) {
        this.geriDon = geriDon;
    }

    public void MesajlasmaEngelle(String engellenenId) {
        sohbetIDOlustur(UserSession.INSTANCE.getUserId(), engellenenId, engellenensohbetID -> {
            EngelAlanKontrolu(engellenensohbetID, UserSession.INSTANCE.getUserId());
            EngelAlanKontrolu(engellenensohbetID, "anaMesaj");
            EngelAlanKontrolu(engellenensohbetID, "gunMesaj");
            EngelAlanKontrolu(engellenensohbetID, "silMesaj");
            mesajlar.child(engellenensohbetID).child("yaziyorMu").child(gonderen.id).setValue(false);
            mesajlar.child(engellenensohbetID).child("yaziyorMu").child(engellenenId).setValue(false);
            mesajlar.child(engellenensohbetID).child("engelliMi").child(gonderen.id).setValue(true);
        });
    }

    public void MesajlasmaEngellemeKaldir(String engellenenId) {
        sohbetIDOlustur(UserSession.INSTANCE.getUserId(), engellenenId, engellenensohbetID -> {
            mesajlar.child(engellenensohbetID).child("engelliMi").child(gonderen.id).setValue(false);
        });
    }

    private void EngelAlanKontrolu(String sohbetID, String alan) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("mesajlar").child(sohbetID).child(alan);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    ref.setValue(new HashMap<>());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    public void AyarlariYap() {
        if (sohbetID == null) return;
        mesajlar.child(sohbetID).child("engelliMi")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mesajlar.child(sohbetID).child("engelliMi").child(gonderen.id).setValue(false);
                            mesajlar.child(sohbetID).child("engelliMi").child(alici.id).setValue(false);
                            EngelAlanKontrolu(sohbetID, "anaMesaj");
                            EngelAlanKontrolu(sohbetID, "gunMesaj");
                            EngelAlanKontrolu(sohbetID, "silMesaj");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void EngelKaldir(Runnable kaldirildi) {
        if (gonderen == null || gonderen.id == null || alici == null || alici.id == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference kullaniciRef = db.collection("users").document(gonderen.id);
        kullaniciRef.update("blockedUsers", FieldValue.arrayRemove(alici.id))
                .addOnSuccessListener(aVoid -> {
                    kullaniciRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (kaldirildi != null) kaldirildi.run();
                            MesajlasmaEngellemeKaldir(alici.id);
                        }
                    });
                });
    }

    public void setEngelledi(boolean engelledi) {
        this.engelledi = engelledi;
    }

    public void setEngelledim(boolean engelledim) {
        this.engelledim = engelledim;
    }

    public void MesajlasmaYonetimiDurdur() {
        DinleyiciKaldir();
        yonetici = null;
    }
}