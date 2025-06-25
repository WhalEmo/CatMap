package com.emrullah.catmap;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UyariMesaji {
    private Dialog yuklemeDialog;
    private FrameLayout YuklemeEkrani;
    private TextView Durum;
    private ImageView BasariliTik, BasarisizCarpi;
    private ProgressBar YuklemeBar;
    public Boolean DahaOnceAlinmisMi=false;

    public UyariMesaji(Context context, boolean SefafMi){
        AyarlarıYap(context, SefafMi);
    }

    private void AyarlarıYap(Context context, boolean SefafMi) {
        yuklemeDialog = new Dialog(context);
        yuklemeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        yuklemeDialog.setContentView(R.layout.giris_yukleme);

        if (yuklemeDialog.getWindow() != null) {
            yuklemeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            yuklemeDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            yuklemeDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        yuklemeDialog.setCancelable(false);

        YuklemeEkrani = yuklemeDialog.findViewById(R.id.progressOverlay);
        Durum = yuklemeDialog.findViewById(R.id.progressMessage);
        BasariliTik = yuklemeDialog.findViewById(R.id.successIcon);
        BasarisizCarpi = yuklemeDialog.findViewById(R.id.basariDegilIcon);
        YuklemeBar = yuklemeDialog.findViewById(R.id.progressBar);
        if (SefafMi) {
            YuklemeEkrani.setBackground(null);
        }
    }

    private void uyariGoster() {
        if(!yuklemeDialog.isShowing()){
            yuklemeDialog.show();
        }
    }
    private void uyariGizle() {
        if (yuklemeDialog != null && yuklemeDialog.isShowing()) {
            yuklemeDialog.dismiss();
        }
    }

    public void BasariliDurum(String mesaj,int kacSalise){
        uyariGoster();
        BasariliTik.setVisibility(View.VISIBLE);
        YuklemeBar.setVisibility(View.GONE);
        Durum.setText(mesaj);
        new Handler().postDelayed(() -> {
            YuklemeEkrani.setVisibility(View.GONE);
            uyariGizle();
        }, kacSalise);
    }
    public void BasarisizDurum(String mesaj,int kacSalise){
        uyariGoster();
        BasarisizCarpi.setVisibility(View.VISIBLE);
        YuklemeBar.setVisibility(View.GONE);
        Durum.setText(mesaj);
        new Handler().postDelayed(() -> {
            YuklemeEkrani.setVisibility(View.GONE);
            uyariGizle();
        }, kacSalise);
    }
    public void YuklemeDurum(String mesaj){
        uyariGoster();
        BasarisizCarpi.setVisibility(View.GONE);
        BasariliTik.setVisibility(View.GONE);
        YuklemeBar.setVisibility(View.VISIBLE);
        Durum.setText(mesaj);
        YuklemeEkrani.setVisibility(View.VISIBLE);
    }

}
