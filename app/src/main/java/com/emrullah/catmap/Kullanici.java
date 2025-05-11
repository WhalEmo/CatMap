package com.emrullah.catmap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class Kullanici {
    private String ID;
    private String Ad;
    private String Soyad;
    private String Email;
    private String KullaniciAdi;
    private String Sifre;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

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

    public String getAd() {
        return Ad;
    }

    public void setAd(String ad) {
        Ad = StDuzenle(ad);
    }

    public String getSoyad() {
        return Soyad;
    }

    public void setSoyad(String soyad) {
        Soyad = StDuzenle(soyad);
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email.toLowerCase();
    }

    public boolean KullaniciIs(){
        if(this.Ad.isEmpty() || this.Soyad.isEmpty() || this.Email.isEmpty() || this.KullaniciAdi.isEmpty() || this.Sifre.isEmpty()){
            return false;
        }
        return true;
    }

    private String StDuzenle(String yazi){
        if(yazi==null || yazi.isEmpty()){
            return yazi;
        }
        return yazi.substring(0, 1).toUpperCase()+yazi.substring(1).toLowerCase();
    }

    public Map KullaniciData(){
        Map<String, Object> kullaniciData = new HashMap<>();
        kullaniciData.put("Ad",this.Ad);
        kullaniciData.put("Soyad",this.Soyad);
        kullaniciData.put("Email",this.Email);
        kullaniciData.put("KullaniciAdi",this.KullaniciAdi);
        kullaniciData.put("Sifre",this.Sifre);
        return kullaniciData;
    }

    public void GetYerelKullanici(Context baglanti){
        SharedPreferences kayit = baglanti.getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
        this.Ad = kayit.getString("Ad","");
        this.Soyad = kayit.getString("Soyad","");
        this.Email = kayit.getString("Email","");
        this.KullaniciAdi = kayit.getString("KullaniciAdi","");
        this.Sifre = kayit.getString("Sifre","");
    }

}
