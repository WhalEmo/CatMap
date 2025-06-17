package com.emrullah.catmap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Yanit_Model {
    private String yanitId;
    private String Adi;
    private String yaniticerik;
    private Date tarih;
    public boolean yanitMiGeldi=false;

    public Yanit_Model(){}

    public Yanit_Model(String yanitId, String adi, String yaniticerik, Date tarih) {
        this.yanitId = yanitId;
        Adi = adi;
        this.yaniticerik = yaniticerik;
        this.tarih = tarih;
    }

    public String getYanitId() {
        return yanitId;
    }

    public void setYanitId(String yanitId) {
        this.yanitId = yanitId;
    }

    public String getAdi() {
        return Adi;
    }

    public void setAdi(String adi) {
        Adi = adi;
    }

    public String getYaniticerik() {
        return yaniticerik;
    }

    public void setYaniticerik(String yaniticerik) {
        this.yaniticerik = yaniticerik;
    }

    public Date getTarih() {
        return tarih;
    }

    public void setTarih(Date tarih) {
        this.tarih = tarih;
    }
    public String duzenlenmisTarih() {
        if (tarih == null) {
            return "şimdi";  // Ya da "Bilinmiyor"
        }

        long simdi = System.currentTimeMillis();
        long fark = simdi - tarih.getTime();

        if (fark < 60000) {  // 1 dakika
            return "şimdi";
        } else if (fark < 3600000) {  // 1 saat
            return (fark / 60000) + " dakika önce";
        }  else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return sdf.format(tarih);
        }
    }

    public boolean isYanitMiGeldi() {
        return yanitMiGeldi;
    }

    public void setYanitMiGeldi(boolean yanitMiGeldi) {
        this.yanitMiGeldi = yanitMiGeldi;
    }
}
