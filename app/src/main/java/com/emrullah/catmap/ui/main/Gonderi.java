package com.emrullah.catmap.ui.main;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class Gonderi {
    private ArrayList<String> fotoUrlListesi;
    private String aciklama;
    private String kediAdi;
    private Timestamp tarih;
    private Long BegeniSayisi;
    private String kediID;

    public Gonderi(ArrayList<String> fotoUrlListesi, String aciklama, String kediAdi,Timestamp  tarih,Long BegeniSayisi,String kediID) {
        this.fotoUrlListesi = fotoUrlListesi;
        this.aciklama = aciklama;
        this.kediAdi=kediAdi;
        this.tarih = tarih;
        this.BegeniSayisi=BegeniSayisi;
        this.kediID=kediID;
    }

    public String getKediID() {
        return kediID;
    }

    public void setKediID(String kediID) {
        this.kediID = kediID;
    }

    public Long getBegeniSayisi() {
        return BegeniSayisi;
    }

    public void setBegeniSayisi(Long begeniSayisi) {
        BegeniSayisi = begeniSayisi;
    }

    public Timestamp  getTarih() {
        return tarih;
    }

    public void setTarih(Timestamp  tarih) {
        this.tarih = tarih;
    }

    public void setFotoUrlListesi(ArrayList<String> fotoUrlListesi) {
        this.fotoUrlListesi = fotoUrlListesi;
    }

    public ArrayList<String> getFotoUrlListesi() {
        return fotoUrlListesi;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getKediAdi() {
        return kediAdi;
    }

    public void setKediAdi(String kediAdi) {
        this.kediAdi = kediAdi;
    }
}
