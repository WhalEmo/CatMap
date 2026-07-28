package com.beem.catmap.YorumYanit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Yanit_Model {
    private String yanitId;
    private String yorumId;
    private String adi;
    private String yaniticerik;
    private Date tarih;
    private String yanitiYukleyen;
    private int begeniSayisiYanit = 0;
    private boolean begenildiMi = false;
    private boolean isSending = false;
    private boolean localOnly = false; // Yeni eklenen profesyonel alan

    public Yanit_Model() {
        this.yanitId = "";
    }

    public Yanit_Model(String yanitId, String adi, String yaniticerik, Date tarih, String yanitiYukleyen, int begeniSayisiYanit, boolean isSending) {
        this.yanitId = yanitId != null ? yanitId : "";
        this.adi = adi;
        this.yaniticerik = yaniticerik;
        this.tarih = tarih;
        this.yanitiYukleyen = yanitiYukleyen;
        this.begeniSayisiYanit = begeniSayisiYanit;
        this.isSending = isSending;
    }

    public Yanit_Model copy() {
        Yanit_Model kopya = new Yanit_Model(this.yanitId, this.adi, this.yaniticerik, this.tarih, this.yanitiYukleyen, this.begeniSayisiYanit, this.isSending);
        kopya.setYorumId(this.yorumId);
        kopya.setBegenildiMi(this.begenildiMi);
        kopya.setLocalOnly(this.localOnly);
        return kopya;
    }

    public String getYanitId() { return yanitId; }
    public void setYanitId(String yanitId) { this.yanitId = yanitId; }

    public String getYorumId() { return yorumId; }
    public void setYorumId(String yorumId) { this.yorumId = yorumId; }

    public String getAdi() { return adi; }
    public void setAdi(String adi) { this.adi = adi; }

    public String getYaniticerik() { return yaniticerik; }
    public void setYaniticerik(String yaniticerik) { this.yaniticerik = yaniticerik; }

    public Date getTarih() { return tarih; }
    public void setTarih(Date tarih) { this.tarih = tarih; }

    public String getYanitiYukleyen() { return yanitiYukleyen; }
    public void setYanitiYukleyen(String yanitiYukleyen) { this.yanitiYukleyen = yanitiYukleyen; }

    public int getBegeniSayisiYanit() { return begeniSayisiYanit; }
    public void setBegeniSayisiYanit(int begeniSayisiYanit) { this.begeniSayisiYanit = begeniSayisiYanit; }

    public boolean isBegenildiMi() { return begenildiMi; }
    public void setBegenildiMi(boolean begenildiMi) { this.begenildiMi = begenildiMi; }

    public boolean isSending() { return isSending; }
    public void setSending(boolean sending) { isSending = sending; }

    public boolean isLocalOnly() { return localOnly; }
    public void setLocalOnly(boolean localOnly) { this.localOnly = localOnly; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Yanit_Model that = (Yanit_Model) o;
        return begeniSayisiYanit == that.begeniSayisiYanit &&
                begenildiMi == that.begenildiMi &&
                isSending == that.isSending &&
                localOnly == that.localOnly &&
                Objects.equals(yanitId, that.yanitId) &&
                Objects.equals(yorumId, that.yorumId) &&
                Objects.equals(adi, that.adi) &&
                Objects.equals(yaniticerik, that.yaniticerik) &&
                Objects.equals(tarih, that.tarih) &&
                Objects.equals(yanitiYukleyen, that.yanitiYukleyen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yanitId, yorumId, adi, yaniticerik, tarih, yanitiYukleyen, begeniSayisiYanit, begenildiMi, isSending, localOnly);
    }

    public String duzenlenmisTarih() {
        if (tarih == null) return "şimdi";

        long simdi = System.currentTimeMillis();
        long fark = simdi - tarih.getTime();

        if (fark < 60000) return "şimdi";
        if (fark < 3600000) return (fark / 60000) + " dakika önce";

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        return sdf.format(tarih);
    }
}