package com.emrullah.catmap;

import java.sql.Timestamp;

public class Yorumlar {
    private String yorum;
    private com.google.firebase.Timestamp zaman;
    private String kullanici_isim;

    public Yorumlar(){};
    public Yorumlar(String yorum, com.google.firebase.Timestamp zaman, String kullanici_isim) {
        this.yorum = yorum;
        this.zaman = zaman;
        this.kullanici_isim= kullanici_isim;
    }

    // Getter ve Setter'lar
    public com.google.firebase.Timestamp getTarih() {
        return zaman;
    }

    public void setTarih(com.google.firebase.Timestamp tarih) {
        this.zaman = tarih;
    }

    public String getYorum() {
        return yorum;
    }

    public void setYorum(String yorum) {
        this.yorum = yorum;
    }


    public String getKullanici_isim() {
        return kullanici_isim;
    }

    public void setKullanici_isim(String kullanici_isim) {
        this.kullanici_isim = kullanici_isim;
    }
}
