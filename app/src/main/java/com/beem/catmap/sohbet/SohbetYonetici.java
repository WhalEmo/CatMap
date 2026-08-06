package com.beem.catmap.sohbet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.beem.catmap.KullaniciAuth.Kullanici;
import com.beem.catmap.data.local.UserSession;
import com.beem.catmap.mesaj.Mesaj;
import com.beem.catmap.R;
import com.beem.catmap.mesaj.YanitMesaj;
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
    private HashMap<String, Object> ProfilFotolari = new HashMap<>();
    private HashMap<String, Kullanici> Kullanicilar = new HashMap<>();
    private HashMap<String, Mesaj> SonMesajlar = new HashMap<>();
    private static SohbetYonetici yonetici;
    private HashMap<String, Target> FotolariCek = new HashMap<>();
    private HashMap<String, ChildEventListener> dinleyiciler = new HashMap<>();
    private HashMap<String, Mesaj> gorulmemisMesajlar = new HashMap<>();

    public static SohbetYonetici getInstance() {
        if (yonetici == null) {
            yonetici = new SohbetYonetici();
        }
        return yonetici;
    }

    public SohbetYonetici() {}

    public void SohbetleriCek(ArrayList<Sohbet> sohbetArrayList, Runnable tamamdir) {
        sohbetDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot sohbet : snapshot.getChildren()) {
                    String sohbetID = sohbet.getKey();
                    if (sohbetID == null) continue;

                    String[] idler = sohbetID.split("_");
                    if (idler.length < 2) continue;

                    String currentUserId = UserSession.INSTANCE.getUserId();

                    if (idler[0].equals(currentUserId)) {
                        Kullanici alici = new Kullanici();
                        alici.id = idler[1];
                        Sohbet sohbet1 = new Sohbet(sohbetID, alici, new Mesaj());
                        sohbetArrayList.add(sohbet1);
                    }
                    if (idler[1].equals(currentUserId)) {
                        Kullanici alici = new Kullanici();
                        alici.id = idler[0];
                        Sohbet sohbet1 = new Sohbet(sohbetID, alici, null);
                        sohbetArrayList.add(sohbet1);
                    }
                }
                SohbetNesneleriniOlustur(sohbetArrayList, tamamdir);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void SohbetNesneleriniOlustur(ArrayList<Sohbet> sohbetArrayList, Runnable tamamdir) {
        if (Kullanicilar == null) Kullanicilar = new HashMap<>();
        if (SonMesajlar == null) SonMesajlar = new HashMap<>();
        if (ProfilFotolari == null) ProfilFotolari = new HashMap<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Sohbet sohbet : sohbetArrayList) {
            EngelKontrol(sohbet);

            String aliciId = sohbet.getAlici() != null ? sohbet.getAlici().id : null;
            if (aliciId == null) continue;

            if (Kullanicilar.containsKey(aliciId)) {
                sohbet.setAlici(Kullanicilar.get(aliciId));
                FotolariCek(sohbet, tamamdir);
                sohbet.setMesaj(SonMesajlar.get(aliciId));
                if (tamamdir != null) tamamdir.run();
            } else {
                db.collection("users")
                        .document(aliciId)
                        .get()
                        .addOnSuccessListener(veri -> {
                            if (veri.exists()) {
                                sohbet.getAlici().ad = veri.getString("Ad") != null ? veri.getString("Ad") : "";
                                sohbet.getAlici().fotoUrl = veri.getString("profilFotoUrl") != null ? veri.getString("profilFotoUrl") : "";
                                sohbet.getAlici().soyad = veri.getString("Soyad") != null ? veri.getString("Soyad") : "";
                                sohbet.getAlici().kullaniciAdi = veri.getString("KullaniciAdi") != null ? veri.getString("KullaniciAdi") : "";

                                Kullanicilar.put(sohbet.getAlici().id, sohbet.getAlici());
                                FotolariCek(sohbet, tamamdir);
                                if (tamamdir != null) tamamdir.run();
                            }
                        });
            }
            SonGorulmeCevrimIci(sohbet);

            if (dinleyiciler.containsKey(sohbet.getSohbetID())) {
                sohbetDB.child(sohbet.getSohbetID()).child("anaMesaj").removeEventListener(dinleyiciler.get(sohbet.getSohbetID()));
            }

            ChildEventListener dinleyici = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    Mesaj mesaj = MesajOlustur(snapshot);
                    if (mesaj == null) return;

                    sohbet.setMesaj(mesaj);
                    yeniGelenGorulmemisMesajlarSayisi(sohbet);

                    SonMesajlar.put(sohbet.getAlici().id, mesaj);
                    if (sohbet.isSohbetYuklendiMi()) {
                        Sirala(sohbetArrayList);
                        if (tamamdir != null) tamamdir.run();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {}

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onCancelled(DatabaseError error) {}
            };

            dinleyiciler.put(sohbet.getSohbetID(), dinleyici);
            sohbetDB.child(sohbet.getSohbetID())
                    .child("anaMesaj")
                    .orderByChild("zaman")
                    .limitToLast(20)
                    .addChildEventListener(dinleyici);
        }
    }

    private void FotolariCek(Sohbet sohbet, Runnable tamamdir) {
        if (sohbet.isEngelliSohbetMi() || sohbet.getAlici() == null) return;

        String fotoUrl = sohbet.getAlici().fotoUrl;

        if (fotoUrl == null || fotoUrl.isEmpty()) {
            if (tamamdir != null) tamamdir.run();
            sohbet.setSohbetYuklendiMi(true);
            return;
        }

        if (ProfilFotolari.containsKey(fotoUrl)) {
            sohbet.getAlici().fotoBitmap = (Bitmap) ProfilFotolari.get(fotoUrl);
            if (tamamdir != null) tamamdir.run();
            sohbet.setSohbetYuklendiMi(true);
            return;
        }

        if (sohbet.getAlici().fotoBitmap != null) {
            ProfilFotolari.put(fotoUrl, sohbet.getAlici().fotoBitmap);
            sohbet.setSohbetYuklendiMi(true);
            if (tamamdir != null) tamamdir.run();
            return;
        }

        Picasso.get()
                .load(fotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(FotografTargetHaziriligi(sohbet, tamamdir));
    }

    private void SonGorulmeCevrimIci(Sohbet sohbet) {
        if (sohbet.getAlici() == null || sohbet.getAlici().id == null) return;

        FirebaseDatabase.getInstance().getReference("durumlar")
                .child(sohbet.getAlici().id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean cevrimici = snapshot.child("cevrimici").getValue(Boolean.class);
                        Long sonGorulme = snapshot.child("sonGorulme").getValue(Long.class);

                        sohbet.getAlici().isCevrimiciMi = cevrimici != null && cevrimici;
                        sohbet.getAlici().sonGorulme = sonGorulme != null ? sonGorulme : 0L;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void yeniGelenGorulmemisMesajlarSayisi(Sohbet sohbet) {
        if (sohbet.getMesaj() == null || sohbet.getAlici() == null) return;

        if (sohbet.getMesaj().getGonderici().equals(sohbet.getAlici().id)) {
            if (sohbet.getMesaj().isGoruldu()) {
                sohbet.setOkunmamisMesajSayisi(0);
            } else {
                sohbet.setOkunmamisMesajSayisi(sohbet.getOkunmamisMesajSayisi() + 1);
            }
        }
    }

    private Target FotografTargetHaziriligi(Sohbet sohbet, Runnable tamamdir) {
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (sohbet.getAlici() != null) {
                    sohbet.getAlici().fotoBitmap = bitmap;
                    if (sohbet.getAlici().fotoUrl != null) {
                        ProfilFotolari.put(sohbet.getAlici().fotoUrl, bitmap);
                        FotolariCek.remove(sohbet.getAlici().fotoUrl);
                    }
                }
                sohbet.setSohbetYuklendiMi(true);
                if (tamamdir != null) tamamdir.run();
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                if (sohbet.getAlici() != null) {
                    sohbet.getAlici().fotoBitmap = null;
                }
                sohbet.setSohbetYuklendiMi(true);
                if (tamamdir != null) tamamdir.run();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                if (sohbet.getAlici() != null) {
                    sohbet.getAlici().fotoBitmap = null;
                }
                sohbet.setSohbetYuklendiMi(true);
                if (tamamdir != null) tamamdir.run();
            }
        };

        if (sohbet.getAlici() != null && sohbet.getAlici().fotoUrl != null) {
            FotolariCek.put(sohbet.getAlici().fotoUrl, target);
        }
        return target;
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

    public HashMap<String, Kullanici> getKullanicilar() {
        return Kullanicilar;
    }

    private void Sirala(ArrayList<Sohbet> sohbetler) {
        for (int i = 0; i < sohbetler.size(); i++) {
            if (sohbetler.get(i).getMesaj() == null) continue;
            long zaman = sohbetler.get(i).getMesaj().getZaman();
            Sohbet sohbet = sohbetler.get(i);
            int ink = i;
            for (int j = i + 1; j < sohbetler.size(); j++) {
                if (sohbetler.get(j).getMesaj() == null) continue;
                if (zaman < sohbetler.get(j).getMesaj().getZaman()) {
                    zaman = sohbetler.get(j).getMesaj().getZaman();
                    ink = j;
                }
            }
            if (ink != i) {
                sohbetler.set(i, sohbetler.get(ink));
                sohbetler.set(ink, sohbet);
            }
        }
    }

    public void DinleyicileriKaldir(ArrayList<Sohbet> sohbetArrayList) {
        for (Sohbet sohbet : sohbetArrayList) {
            if (dinleyiciler.containsKey(sohbet.getSohbetID())) {
                sohbetDB.child(sohbet.getSohbetID()).child("anaMesaj").removeEventListener(dinleyiciler.get(sohbet.getSohbetID()));
                dinleyiciler.remove(sohbet.getSohbetID());
            }
        }
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
            Boolean goruldu = snapshot.child("goruldu").getValue(Boolean.class);

            Mesaj mesaj = new Mesaj(gonderen, "\uD83D\uDCF7  Fotoğraf", zaman != null ? zaman : 0L, mesajID, false);
            mesaj.setTur(tur);
            mesaj.setGoruldu(goruldu != null && goruldu);
            return mesaj;
        }
    }

    private void EngelKontrol(Sohbet sohbet) {
        if (sohbet == null || sohbet.getAlici() == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("mesajlar")
                .child(sohbet.getSohbetID())
                .child("engelliMi");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean engellendinMiVal = snapshot.child(sohbet.getAlici().id).getValue(Boolean.class);
                    Boolean engelledinMiVal = snapshot.child(UserSession.INSTANCE.getUserId()).getValue(Boolean.class);

                    boolean engellendinMi = engellendinMiVal != null && engellendinMiVal;
                    boolean engelledinMi = engelledinMiVal != null && engelledinMiVal;

                    sohbet.setEngelliSohbetMi(engelledinMi || engellendinMi);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}