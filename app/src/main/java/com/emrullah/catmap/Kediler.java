package com.emrullah.catmap;

public class Kediler {
    private String isim;
    private String hakkindasi;
    private double latitude;
    private double longitude;
    private String URL;
    private String ID;

    public Kediler(String ID,String isim ,String hakkindasi,double latitude,double longitude,String URL){
        this.ID=ID;
        this.isim=isim;
        this.hakkindasi=hakkindasi;
        this.latitude=latitude;
        this.longitude=longitude;
        this.URL=URL;
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
}
