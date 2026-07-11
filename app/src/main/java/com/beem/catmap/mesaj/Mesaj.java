package com.beem.catmap.mesaj;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class Mesaj {
    private String gonderici;
    private String mesaj;
    private long zaman;
    private String mesajID;
    private boolean goruldu;
    private String tur;
    private ArrayList<String> urller;
    private boolean yaniyorMu = false;
    private boolean yuklendiMi = false;


    public Mesaj(String gonderici, String mesaj, long zaman, String mesajID, boolean goruldu) {
        this.gonderici = gonderici;
        this.mesaj = mesaj;
        this.zaman = zaman;
        this.mesajID = mesajID;
        this.goruldu = goruldu;
    }
    public Mesaj(String gonderici, ArrayList<String> urller, long zaman, String mesajID, boolean goruldu) {
        this.gonderici = gonderici;
        this.urller = urller;
        this.zaman = zaman;
        this.mesajID = mesajID;
        this.goruldu = goruldu;
    }
    public Mesaj(String gonderici, long zaman, String mesajID, boolean goruldu) {
        this.gonderici = gonderici;
        this.urller = urller;
        this.zaman = zaman;
        this.mesajID = mesajID;
        this.goruldu = goruldu;
    }
    public Mesaj() {
    }

    public void setTur(String tur) {
        this.tur = tur;
    }
    public String getTur() {
        return tur;
    }

    public ArrayList<String> getUrller() {
        return urller;
    }


    public String getGonderici() {
        return gonderici;
    }

    public void setGonderici(String gonderici) {
        this.gonderici = gonderici;
    }


    public String getMesaj() {
        return mesaj;
    }

    public void setMesaj(String mesaj) {
        this.mesaj = mesaj;
    }

    public String getStringZaman() {
        Date tarih = new Date(zaman);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(tarih);
    }

    public Long getZaman(){
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

    public boolean isYaniyorMu() {
        return yaniyorMu;
    }
    public void setYaniyorMu(boolean yaniyorMu) {
        this.yaniyorMu = yaniyorMu;
    }

    public void setYuklendiMi(boolean yuklendiMi) {
        this.yuklendiMi = yuklendiMi;
    }

    public boolean isYuklendiMi() {
        return yuklendiMi;
    }

    public void setUrller(ArrayList<String> urller) {
        this.urller = urller;
    }
}
