package com.emrullah.catmap.ui.main;

import java.util.ArrayList;

public class Gonderi {
    private ArrayList<String> fotoUrlListesi;
    private String aciklama;
    private String kediAdi;

    public Gonderi(ArrayList<String> fotoUrlListesi, String aciklama,String kediAdi) {
        this.fotoUrlListesi = fotoUrlListesi;
        this.aciklama = aciklama;
        this.kediAdi=kediAdi;
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
