package com.emrullah.catmap;

public class Kullanici {
    private String KullaniciAdi;
    private String Sifre;

    public Kullanici(){}

    public Kullanici(String kullaniciAdi, String sifre){
        this.KullaniciAdi = kullaniciAdi;
        this.Sifre = sifre;
    }

    public String getKullaniciAdi() {
        return KullaniciAdi;
    }

    public void setKullaniciAdi(String kullaniciAdi) {
        KullaniciAdi = kullaniciAdi;
    }

    public String getSifre() {
        return Sifre;
    }

    public void setSifre(String sifre) {
        Sifre = sifre;
    }
}
