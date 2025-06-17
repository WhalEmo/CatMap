package com.emrullah.catmap;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Yorum_Model {
    private String yorumID;
    private String KullaniciAdi;
    private String Yorumicerik;
    private Date Tarih;
    private ArrayList<Yanit_Model>yanitlar;
    private DocumentSnapshot sonYanit;
    private boolean yanitlarGorunuyor = false;
    private boolean yanitlarYuklendi = false;
    private boolean yanitYokMu = false;
    private boolean dahafazlaGozukuyorMu=true;
    private Yanit_Adapter yanitAdapter;

    public Yorum_Model(){}

    public Yorum_Model(String yorumID, String kullaniciAdi,String yorumicerik, Date tarih, ArrayList<Yanit_Model> yanitlar) {
        this.yorumID = yorumID;
        KullaniciAdi = kullaniciAdi;
        Yorumicerik = yorumicerik;
        Tarih = tarih;
        this.yanitlar = yanitlar;
    }

    public String getYorumID() {
        return yorumID;
    }

    public void setYorumID(String yorumID) {
        this.yorumID = yorumID;
    }

    public String getKullaniciAdi() {
        return KullaniciAdi;
    }

    public void setKullaniciAdi(String kullaniciAdi) {
        KullaniciAdi = kullaniciAdi;
    }

    public String getYorumicerik() {
        return Yorumicerik;
    }

    public void setYorumicerik(String yorumicerik) {
        Yorumicerik = yorumicerik;
    }

    public Date getTarih() {

        return Tarih;
    }
    public String duzenlenmisTarih() {
        if (Tarih == null) {
            return "şimdi";  // Ya da "Bilinmiyor"
        }

        long simdi = System.currentTimeMillis();
        long fark = simdi - Tarih.getTime();

        if (fark < 60000) {  // 1 dakika
            return "şimdi";
        } else if (fark < 3600000) {  // 1 saat
            return (fark / 60000) + " dakika önce";
        }else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return sdf.format(Tarih);
        }
    }

    public void setTarih(Date tarih) {
        Tarih = tarih;
    }

    public ArrayList<Yanit_Model> getYanitlar() {
        return yanitlar;
    }

    public void setYanitlar(ArrayList<Yanit_Model> yanitlar) {
        this.yanitlar = yanitlar;
    }

    public DocumentSnapshot getSonYanit() {
        return sonYanit;
    }

    public void setSonYanit(DocumentSnapshot sonYanit) {
        this.sonYanit = sonYanit;
    }

    public boolean isYanitlarGorunuyor() {
        return yanitlarGorunuyor;
    }

    public void setYanitlarGorunuyor(boolean yanitlarGorunuyor) {
        this.yanitlarGorunuyor = yanitlarGorunuyor;
    }

    public boolean isYanitlarYuklendi() {
        return yanitlarYuklendi;
    }

    public void setYanitlarYuklendi(boolean yanitlarYuklendi) {
        this.yanitlarYuklendi = yanitlarYuklendi;
    }

    public boolean isYanitYokMu() {
        return yanitYokMu;
    }

    public void setYanitYokMu(boolean yanitYokMu) {
        this.yanitYokMu = yanitYokMu;
    }

    public boolean isDahafazlaGozukuyorMu() {
        return dahafazlaGozukuyorMu;
    }

    public void setDahafazlaGozukuyorMu(boolean dahafazlaGozukuyorMu) {
        this.dahafazlaGozukuyorMu = dahafazlaGozukuyorMu;
    }

    public Yanit_Adapter getYanitAdapter() {
        return yanitAdapter;
    }

    public void setYanitAdapter(Yanit_Adapter yanitAdapter) {
        this.yanitAdapter = yanitAdapter;
    }

}
