package com.emrullah.catmap;

import java.util.Date;

public class Yanit_Model {
    private String yanitId;
    private String Adi;
    private String yaniticerik;
    private Date tarih;

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
}
