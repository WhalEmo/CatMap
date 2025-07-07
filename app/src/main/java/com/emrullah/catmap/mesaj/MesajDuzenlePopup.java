package com.emrullah.catmap.mesaj;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.emrullah.catmap.R;

public class MesajDuzenlePopup {
    private PopupWindow popupWindow;
    private EditText editYeniMesaj;
    private Button btnIptal, btnMesajiKaydet;
    private LinearLayout popupMesajDuzenle;

    public void Olustur(Context context){
        LayoutInflater inflater = LayoutInflater.from(context);
        View popupView = inflater.inflate(R.layout.mesaj_duzenle, null);
        editYeniMesaj = popupView.findViewById(R.id.editYeniMesaj);
        btnIptal = popupView.findViewById(R.id.btnIptal);
        btnMesajiKaydet = popupView.findViewById(R.id.btnMesajiKaydet);
        popupMesajDuzenle = popupView.findViewById(R.id.popupMesajDuzenle);

        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        KlavyeAyari(popupView);
    }
    public void Goster(View anchorView, Mesaj mesaj){

        editYeniMesaj.setText(mesaj.getMesaj());
        editYeniMesaj.setSelection(mesaj.getMesaj().length());

        btnMesajiKaydet.setOnClickListener(v -> {
            if(!editYeniMesaj.getText().toString().trim().isEmpty()) {
                mesaj.setMesaj(editYeniMesaj.getText().toString().trim());
                MesajlasmaYonetici.getInstance().MesajGuncelle(mesaj.getMesajID(),editYeniMesaj.getText().toString().trim());
            }
            popupWindow.dismiss();
        });

        btnIptal.setOnClickListener(v -> popupWindow.dismiss());

        popupWindow.setElevation(10f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }
    public void Gizle() {
        popupWindow.dismiss();
    }

    private void KlavyeAyari(View rootView){
        LinearLayout mesajLayout = popupMesajDuzenle;

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()); // Klavye boyutu
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Sistem barları
            boolean klavyeAcik = imeInsets.bottom > 0;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mesajLayout.getLayoutParams();

            int klavyeYuksekligi = imeInsets.bottom;
            int sistemCubuğuYuksekligi = navInsets.bottom;

            int netYukseklik = klavyeYuksekligi - sistemCubuğuYuksekligi;
            if (netYukseklik < 0) netYukseklik = 0;

            params.bottomMargin = klavyeAcik ? netYukseklik + dpToPx(4) : dpToPx(8);
            mesajLayout.setLayoutParams(params);
            return insets;
        });

    }
    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
