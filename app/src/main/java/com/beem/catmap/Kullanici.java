package com.beem.catmap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Kullanici {
    private String ID;
    private String Ad;
    private String Soyad;
    private String Email;
    private String KullaniciAdi;
    private String Sifre;
    private double latitude;
    private boolean girisBasarili;
    private String FotoUrl;

    private Bitmap fotoBitmap;
    private long sonGorulme;
    private boolean cevrimiciMi;

    public int TakipEdiyorMuyum=0;
    public int TakipciMi=0;

    public boolean isCevrimiciMi() {
        return cevrimiciMi;
    }
    public void setCevrimiciMi(boolean cevrimiciMi) {
        this.cevrimiciMi = cevrimiciMi;
    }

    public String getSonGorulme() {
        Date tarih = new Date(sonGorulme);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(tarih);
    }
    public void setSonGorulme(long sonGorulme) {
        this.sonGorulme = sonGorulme;
    }

    public Bitmap getFotoBitmap() {
        return fotoBitmap;
    }
    public void setFotoBitmap(Bitmap fotoBitmap) {
        this.fotoBitmap = fotoBitmap;
    }


    public String getFotoUrl() {
        return FotoUrl;
    }
    public void setFotoUrl(String fotoUrl) {
        FotoUrl = fotoUrl;
    }
    public boolean isGirisBasarili() {
        return girisBasarili;
    }

    public void setGirisBasarili(boolean girisBasarili) {
        this.girisBasarili = girisBasarili;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    private double longitude;

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
        KullaniciAdi = kullaniciAdi.trim();
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
        Email = email.toLowerCase().trim();
    }


    public boolean KullaniciIs(){
        if(this.Ad.isEmpty() || this.Soyad.isEmpty() || this.Email.isEmpty() || this.KullaniciAdi.isEmpty() || this.Sifre.isEmpty()){
            return false;
        }
        return true;
    }

    private String StDuzenle(String yazi){
        yazi = yazi.trim();
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
        return kullaniciData;
    }

    public void GetYerelKullanici(Context baglanti){
        SharedPreferences kayit = baglanti.getSharedPreferences("KullaniciKayit",MODE_PRIVATE);
        this.ID = kayit.getString("ID","");
        this.Ad = kayit.getString("Ad","");
        this.Soyad = kayit.getString("Soyad","");
        this.Email = kayit.getString("Email","");
        this.KullaniciAdi = kayit.getString("KullaniciAdi","");
        this.Sifre = kayit.getString("Sifre","");
    }

}
