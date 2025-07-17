package com.beem.catmap.sohbet;

import com.beem.catmap.Kullanici;
import com.beem.catmap.mesaj.Mesaj;

public class Sohbet {
    private String SohbetID;
    private Kullanici alici;
    private Mesaj mesaj;
    private boolean sohbetYuklendiMi = false;
    private int okunmamisMesajSayisi = 0;

    public Sohbet() {

    }

    public Sohbet(String sohbetID, Kullanici alici, Mesaj mesaj) {
        this.SohbetID = sohbetID;
        this.alici = alici;
        this.mesaj = mesaj;
    }

    public Mesaj getMesaj() {
        return mesaj;
    }

    public void setMesaj(Mesaj mesaj) {
        this.mesaj = mesaj;
    }

    public Kullanici getAlici() {
        return alici;
    }

    public void setAlici(Kullanici alici) {
        this.alici = alici;
    }

    public String getSohbetID() {
        return SohbetID;
    }

    public void setSohbetID(String sohbetID) {
        SohbetID = sohbetID;
    }
    public int getOkunmamisMesajSayisi() {
        return okunmamisMesajSayisi;
    }
    public void setOkunmamisMesajSayisi(int okunmamisMesajSayisi) {
        this.okunmamisMesajSayisi = okunmamisMesajSayisi;
    }
    public boolean isSohbetYuklendiMi() {
        return sohbetYuklendiMi;
    }
    public void setSohbetYuklendiMi(boolean sohbetYuklendiMi) {
        this.sohbetYuklendiMi = sohbetYuklendiMi;
    }
}
