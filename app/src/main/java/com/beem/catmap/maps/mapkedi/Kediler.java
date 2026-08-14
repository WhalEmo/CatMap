package com.beem.catmap.maps.mapkedi;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

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
    private Date createdAt;

    // 📍 Adres Bilgileri
    private String city = "";
    private String district = "";
    private String neighborhood = "";

    // 1. Firebase'in nesneyi hatasız oluşturabilmesi için BOŞ constructor ŞARTTIR!
    public Kediler() {
    }

    public Kediler(String ID, String isim, String hakkindasi, double latitude, double longitude, String URL, ArrayList<String> URLler, String YukleyenId, Date createdAt){
        this.ID = ID;
        this.isim = isim;
        this.hakkindasi = hakkindasi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.URL = URL;
        this.URLler = URLler;
        this.YukleyenId = YukleyenId;
        this.createdAt = createdAt;
    }

    // 📍 Adres Alanları İçeren Tam Constructor
    public Kediler(String ID, String isim, String hakkindasi, double latitude, double longitude, String URL, ArrayList<String> URLler, String YukleyenId, Date createdAt, String city, String district, String neighborhood){
        this.ID = ID;
        this.isim = isim;
        this.hakkindasi = hakkindasi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.URL = URL;
        this.URLler = URLler;
        this.YukleyenId = YukleyenId;
        this.createdAt = createdAt;
        this.city = city;
        this.district = district;
        this.neighborhood = neighborhood;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("YukleyenKullaniciID")
    public String getYukleyenId() {
        return YukleyenId;
    }

    @PropertyName("YukleyenKullaniciID")
    public void setYukleyenId(String yukleyenId) {
        YukleyenId = yukleyenId;
    }

    // 📍 Adres Getter & Setter Metotları
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }
}