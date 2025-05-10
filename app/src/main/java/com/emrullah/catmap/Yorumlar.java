package com.emrullah.catmap;

import java.sql.Timestamp;

public class Yorumlar {
    private String yorum;
    private Timestamp zaman;
    private String kullanici_isim;

    public Yorumlar(){};
    public Yorumlar(String yorum, Timestamp zaman,String kullanici_isim) {
        this.yorum = yorum;
        this.zaman = zaman;
        this.kullanici_isim=kullanici_isim;
    }

    public String getYorum() {
        return yorum;
    }

    public void setYorum(String yorum) {
        this.yorum = yorum;
    }

    public Timestamp getZaman() {
        return zaman;
    }

    public void setZaman(Timestamp zaman) {
        this.zaman = zaman;
    }

    public String getKullanici_isim() {
        return kullanici_isim;
    }

    public void setKullanici_isim(String kullanici_isim) {
        this.kullanici_isim = kullanici_isim;
    }
}
