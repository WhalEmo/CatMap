package com.beem.catmap.Maps.MapKedi;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;

public class Kediler implements Serializable {
    private String isim;
    private String hakkindasi;
    private double latitude;
    private double longitude;
    private String URL;
    private String ID;
    private boolean MarkerOlustuMu = false;
    private ArrayList<String> URLler;
    private String YukleyenId;

    // 1. Firebase'in nesneyi hatasız oluşturabilmesi için BOŞ constructor ŞARTTIR!
    public Kediler() {
    }

    public Kediler(String ID, String isim, String hakkindasi, double latitude, double longitude, String URL, ArrayList<String> URLler, String YukleyenId){
        this.ID = ID;
        this.isim = isim;
        this.hakkindasi = hakkindasi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.URL = URL;
        this.URLler = URLler;
        this.YukleyenId = YukleyenId;
    }

    public ArrayList<String> getURLler() {
        return URLler;
    }

    public void setURLler(ArrayList<String> URLler) {
        this.URLler = URLler;
    }

    public boolean isMarkerOlustuMu() {
        return MarkerOlustuMu;
    }

    public void setMarkerOlustuMu(boolean markerOlustuMu) {
        MarkerOlustuMu = markerOlustuMu;
    }

    public String getIsim() {
        return isim;
    }

    public void setIsim(String isim) {
        this.isim = isim;
    }

    public String getHakkindasi() {
        return hakkindasi;
    }

    public void setHakkindasi(String hakkindasi) {
        this.hakkindasi = hakkindasi;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    @PropertyName("YukleyenKullaniciID")
    public String getYukleyenId() {
        return YukleyenId;
    }

    @PropertyName("YukleyenKullaniciID")
    public void setYukleyenId(String yukleyenId) {
        YukleyenId = yukleyenId;
    }
}