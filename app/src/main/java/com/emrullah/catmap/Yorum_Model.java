package com.emrullah.catmap;

import java.util.ArrayList;
import java.util.Date;

public class Yorum_Model {
    private String yorumID;
    private String KullaniciAdi;
    private String Yorumicerik;
    private Date Tarih;
    private ArrayList<Yanit_Model>yanitlar;

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

    public void setTarih(Date tarih) {
        Tarih = tarih;
    }

    public ArrayList<Yanit_Model> getYanitlar() {
        return yanitlar;
    }

    public void setYanitlar(ArrayList<Yanit_Model> yanitlar) {
        this.yanitlar = yanitlar;
    }
}
