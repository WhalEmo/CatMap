package com.beem.catmap.Maps.MapKedi;

public class KediSilmeDurumu {
    private static KediSilmeDurumu instance;
    private boolean silindiMi = false;

    private KediSilmeDurumu() {}

    public static KediSilmeDurumu getInstance() {
        if (instance == null) {
            instance = new KediSilmeDurumu();
        }
        return instance;
    }

    public boolean isSilindiMi() {
        return silindiMi;
    }

    public void setSilindiMi(boolean silindiMi) {
        this.silindiMi = silindiMi;
    }
}
