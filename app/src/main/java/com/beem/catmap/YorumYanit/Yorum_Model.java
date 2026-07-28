package com.beem.catmap.YorumYanit;

import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Yorum_Model {
    private String yorumID;
    private String kullaniciAdi;
    private String yorumicerik;
    private Date tarih;
    private String yukleyenId;

    // UI / State Durumları
    private ArrayList<Yanit_Model> yanitlar = new ArrayList<>();
    private DocumentSnapshot sonYanit;
    private boolean yanitlarGorunuyor = false;
    private boolean yanitlarYuklendi = false;
    private boolean yanitYokMu = false;
    private boolean dahafazlaGozukuyorMu = true;
    private boolean begenildiMi = false;
    private int begeniSayisi = 0;

    private int toplamYanitSayisi = 0;

    private boolean isSending = false;

    public Yorum_Model() {}

    public Yorum_Model(String yorumID, String kullaniciAdi, String yorumicerik, Date tarih, ArrayList<Yanit_Model> yanitlar, String yukleyenId, boolean isSending) {
        this.yorumID = yorumID;
        this.kullaniciAdi = kullaniciAdi;
        this.yorumicerik = yorumicerik;
        this.tarih = tarih;
        this.yanitlar = yanitlar != null ? yanitlar : new ArrayList<>();
        this.yukleyenId = yukleyenId;
        this.isSending = isSending;
    }



    public Yorum_Model copyWithUpdatedYanit(Yanit_Model guncelYanit) {

        ArrayList<Yanit_Model> yeniYanitListesi =
                this.yanitlar != null
                        ? new ArrayList<>(this.yanitlar)
                        : new ArrayList<>();

        for (int i = 0; i < yeniYanitListesi.size(); i++) {

            Yanit_Model mevcut = yeniYanitListesi.get(i);

            if (mevcut != null &&
                    Objects.equals(mevcut.getYanitId(), guncelYanit.getYanitId())) {

                yeniYanitListesi.set(i, guncelYanit);
                break;
            }
        }


        Yorum_Model newModel = new Yorum_Model(
                this.yorumID,
                this.kullaniciAdi,
                this.yorumicerik,
                this.tarih,
                yeniYanitListesi,
                this.yukleyenId,
                this.isSending
        );

        aktarOrtakAlanlar(newModel);

        return newModel;
    }

    public Yorum_Model copy() {
        ArrayList<Yanit_Model> yeniListe =
                this.yanitlar == null
                        ? new ArrayList<>()
                        : new ArrayList<>(this.yanitlar);

        Yorum_Model newModel = new Yorum_Model(
                this.yorumID,
                this.kullaniciAdi,
                this.yorumicerik,
                this.tarih,
                yeniListe,
                this.yukleyenId,
                this.isSending
        );

        aktarOrtakAlanlar(newModel);
        return newModel;
    }

    // Yardımcı metod: UI durum değişkenlerini yeni modele kopyalar
    private void aktarOrtakAlanlar(Yorum_Model target) {
        target.sonYanit = this.sonYanit;
        target.yanitlarGorunuyor = this.yanitlarGorunuyor;
        target.yanitlarYuklendi = this.yanitlarYuklendi;
        target.yanitYokMu = this.yanitYokMu;
        target.dahafazlaGozukuyorMu = this.dahafazlaGozukuyorMu;
        target.begenildiMi = this.begenildiMi;
        target.begeniSayisi = this.begeniSayisi;
        target.isSending = this.isSending;
        target.toplamYanitSayisi = this.toplamYanitSayisi;
    }

    public String duzenlenmisTarih() {
        if (tarih == null) return "şimdi";
        long fark = System.currentTimeMillis() - tarih.getTime();
        if (fark < 60000) return "şimdi";
        if (fark < 3600000) return (fark / 60000) + " dk önce";

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        return sdf.format(tarih);
    }

    // Getter ve Setter'lar
    public String getYorumID() { return yorumID; }
    public void setYorumID(String yorumID) { this.yorumID = yorumID; }

    public String getKullaniciAdi() { return kullaniciAdi; }
    public void setKullaniciAdi(String kullaniciAdi) { this.kullaniciAdi = kullaniciAdi; }

    public String getYorumicerik() { return yorumicerik; }
    public void setYorumicerik(String yorumicerik) { this.yorumicerik = yorumicerik; }

    public Date getTarih() { return tarih; }
    public void setTarih(Date tarih) { this.tarih = tarih; }

    public String getYukleyenId() { return yukleyenId; }
    public void setYukleyenId(String yukleyenId) { this.yukleyenId = yukleyenId; }

    public ArrayList<Yanit_Model> getYanitlar() { return yanitlar; }
    public void setYanitlar(ArrayList<Yanit_Model> yanitlar) { this.yanitlar = yanitlar; }

    public DocumentSnapshot getSonYanit() { return sonYanit; }
    public void setSonYanit(DocumentSnapshot sonYanit) { this.sonYanit = sonYanit; }

    public boolean isYanitlarGorunuyor() { return yanitlarGorunuyor; }
    public void setYanitlarGorunuyor(boolean yanitlarGorunuyor) { this.yanitlarGorunuyor = yanitlarGorunuyor; }

    public boolean isYanitlarYuklendi() { return yanitlarYuklendi; }
    public void setYanitlarYuklendi(boolean yanitlarYuklendi) { this.yanitlarYuklendi = yanitlarYuklendi; }

    public boolean isYanitYokMu() { return yanitYokMu; }
    public void setYanitYokMu(boolean yanitYokMu) { this.yanitYokMu = yanitYokMu; }

    public boolean isDahafazlaGozukuyorMu() { return dahafazlaGozukuyorMu; }
    public void setDahafazlaGozukuyorMu(boolean dahafazlaGozukuyorMu) { this.dahafazlaGozukuyorMu = dahafazlaGozukuyorMu; }

    public boolean isBegenildiMi() { return begenildiMi; }
    public void setBegenildiMi(boolean begenildiMi) { this.begenildiMi = begenildiMi; }

    public int getBegeniSayisi() { return begeniSayisi; }
    public void setBegeniSayisi(int begeniSayisi) { this.begeniSayisi = begeniSayisi; }

    public Boolean getSending() { return isSending; }
    public void setSending(Boolean sending) { isSending = sending; }

    public int getToplamYanitSayisi() {
        return toplamYanitSayisi;
    }

    public void setToplamYanitSayisi(int toplamYanitSayisi) {
        this.toplamYanitSayisi = toplamYanitSayisi;
    }

    public boolean isSending() {
        return isSending;
    }

    public void setSending(boolean sending) {
        isSending = sending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Yorum_Model that = (Yorum_Model) o;

        return yanitlarGorunuyor == that.yanitlarGorunuyor &&
                yanitlarYuklendi == that.yanitlarYuklendi &&
                yanitYokMu == that.yanitYokMu &&
                dahafazlaGozukuyorMu == that.dahafazlaGozukuyorMu &&
                begenildiMi == that.begenildiMi &&
                begeniSayisi == that.begeniSayisi &&
                isSending == that.isSending &&
                toplamYanitSayisi == that.toplamYanitSayisi &&
                Objects.equals(yorumID, that.yorumID) &&
                Objects.equals(kullaniciAdi, that.kullaniciAdi) &&
                Objects.equals(yorumicerik, that.yorumicerik) &&
                Objects.equals(tarih, that.tarih) &&
                Objects.equals(yukleyenId, that.yukleyenId) &&
                Objects.equals(yanitlar, that.yanitlar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yorumID, kullaniciAdi, yorumicerik, tarih, yukleyenId, yanitlar,
                yanitlarGorunuyor, yanitlarYuklendi, yanitYokMu,
                dahafazlaGozukuyorMu, begenildiMi, begeniSayisi, isSending,toplamYanitSayisi);
    }
}