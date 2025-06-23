package com.emrullah.catmap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;

public class Profil_Sayfasi extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil__sayfasi);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ProfilSayfasiFragment.newInstance())
                    .commitNow();
        }
    }
}