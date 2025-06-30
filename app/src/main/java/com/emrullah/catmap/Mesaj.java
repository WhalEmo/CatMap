package com.emrullah.catmap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Mesaj {
    private String gonderici;
    private String alici;
    private String mesaj;
    private long zaman;
    private String mesajID;
    private boolean goruldu;

    public Mesaj(String gonderici, String mesaj, long zaman, String mesajID, boolean goruldu) {
        this.gonderici = gonderici;
        this.mesaj = mesaj;
        this.zaman = zaman;
        this.mesajID = mesajID;
        this.goruldu = goruldu;
    }
    public Mesaj() {
    }

    public String getGonderici() {
        return gonderici;
    }

    public void setGonderici(String gonderici) {
        this.gonderici = gonderici;
    }

    public String getAlici() {
        return alici;
    }

    public void setAlici(String alici) {
        this.alici = alici;
    }

    public String getMesaj() {
        return mesaj;
    }

    public void setMesaj(String mesaj) {
        this.mesaj = mesaj;
    }

    public String getZaman() {
        Date tarih = new Date(zaman);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(tarih);
    }

    public Long getLongZaman(){
        return zaman;
    }
    public void setZaman(long zaman) {
        this.zaman = zaman;
    }

    public String getMesajID() {
        return mesajID;
    }

    public void setMesajID(String mesajID) {
        this.mesajID = mesajID;
    }

    public boolean isGoruldu() {
        return goruldu;
    }

    public void setGoruldu(boolean goruldu) {
        this.goruldu = goruldu;
    }
}
