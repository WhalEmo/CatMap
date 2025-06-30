package com.emrullah.catmap.sohbet;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.Mesaj;

public class Sohbet {
    private String SohbetID;
    private Kullanici alici;
    private Mesaj mesaj;

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
}
