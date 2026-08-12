package com.beem.catmap.sohbet;

import com.beem.catmap.data.model.UserModel;
import com.beem.catmap.mesaj.Mesaj;

public class Sohbet {
    private String SohbetID;
    private UserModel alici;
    private Mesaj mesaj;
    private boolean sohbetYuklendiMi = false;
    private int okunmamisMesajSayisi = 0;
    private boolean engelliSohbetMi = false;

    public Sohbet() {

    }

    public Sohbet(String sohbetID, UserModel alici, Mesaj mesaj) {
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

    public UserModel getAlici() {
        return alici;
    }

    public void setAlici(UserModel alici) {
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

    public void setEngelliSohbetMi(boolean engelliSohbetMi) {
        this.engelliSohbetMi = engelliSohbetMi;
    }

    public boolean isEngelliSohbetMi() {
        return engelliSohbetMi;
    }
}
