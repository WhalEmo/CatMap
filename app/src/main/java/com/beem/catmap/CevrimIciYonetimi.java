package com.beem.catmap;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CevrimIciYonetimi {
    private static CevrimIciYonetimi yonetici;

    private boolean yuklemeEkraniGorunuyor = false;
    private boolean anasayfaGorunuyor = false;
    private boolean haritaEkraniGorunuyor = false;
    private CevrimIciYonetimi() {
    }
    public static CevrimIciYonetimi getInstance() {
        if (yonetici == null) {
            yonetici = new CevrimIciYonetimi();
        }
        return yonetici;
    }

    public void CevrimIciCalistir(Kullanici kullanici){
        if(!(yuklemeEkraniGorunuyor || anasayfaGorunuyor || haritaEkraniGorunuyor)){
            CevrimIciOl(false,kullanici);
        }
        else {
            CevrimIciOl(true,kullanici);
        }
    }

    public void YuklemeArayuzAktivitiyeGecildi(){
        this.yuklemeEkraniGorunuyor = true;
        this.anasayfaGorunuyor = false;
        this.haritaEkraniGorunuyor = false;
    }
    public void AnasayfaArayuzAktivitiyeGecildi(){
        this.yuklemeEkraniGorunuyor = false;
        this.anasayfaGorunuyor = true;
        this.haritaEkraniGorunuyor = false;
    }
    public void HaritaArayuzAktivitiyeGecildi(){
        this.yuklemeEkraniGorunuyor = false;
        this.anasayfaGorunuyor = false;
        this.haritaEkraniGorunuyor = true;
    }

    private void CevrimIciOl(boolean durumu, Kullanici kullanici){
        if(durumu == kullanici.isCevrimiciMi()) return;
        if(kullanici.getID()==null) return;
        DatabaseReference durum = FirebaseDatabase.getInstance().getReference("durumlar");
        durum.child(kullanici.getID()).child("cevrimici").setValue(durumu);
        kullanici.setCevrimiciMi(durumu);
        if(!durumu){
            durum.child(kullanici.getID()).child("sonGorulme").setValue(System.currentTimeMillis());
            kullanici.setSonGorulme(System.currentTimeMillis());
        }
    }

    public void setYuklemeEkraniGorunuyor(boolean yuklemeEkraniGorunuyor) {
        this.yuklemeEkraniGorunuyor = yuklemeEkraniGorunuyor;
    }

    public void setAnasayfaGorunuyor(boolean anasayfaGorunuyor) {
        this.anasayfaGorunuyor = anasayfaGorunuyor;
    }

    public void setHaritaEkraniGorunuyor(boolean hataEkraniGorunuyor) {
        this.haritaEkraniGorunuyor = hataEkraniGorunuyor;
    }
}
