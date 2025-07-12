package com.emrullah.catmap.mesaj;

public class YanitMesaj extends Mesaj{

    private Mesaj yanitlananMesaj;


    public YanitMesaj(String gonderici, String mesaj, long zaman, String mesajID, boolean goruldu, Mesaj yanitlananMesaj) {
        super(gonderici, mesaj, zaman, mesajID, goruldu);
        this.yanitlananMesaj = yanitlananMesaj;
        this.setTur("yanit");
    }

    public Mesaj getYanitlananMesaj() {
        return yanitlananMesaj;
    }
    public void setYanitlananMesaj(Mesaj yanitlananMesaj) {
        this.yanitlananMesaj = yanitlananMesaj;
    }

}
