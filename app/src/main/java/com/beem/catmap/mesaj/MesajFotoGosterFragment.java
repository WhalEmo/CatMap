package com.beem.catmap.mesaj;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.beem.catmap.R;

import java.util.ArrayList;

public class MesajFotoGosterFragment extends DialogFragment {

    private ViewPager2 fotoViewPager;
    private ImageButton btnKapat;
    private ArrayList<Bitmap> fotoListesi;
    private ArrayList<String> fotoUrlListesi;
    private MesajFotoGonderYonetici yonetici = MesajFotoGonderYonetici.getInstance();
    private TextView fotoSayaci;


    public MesajFotoGosterFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mesaj_coklu_foto_gosterim, container, false);
        fotoViewPager = view.findViewById(R.id.fotoViewPager);
        btnKapat = view.findViewById(R.id.btnKapat);

        btnKapat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        fotoUrlListesi = yonetici.getFotoUrlleri();
        MesajFotoAdapter adapter = new MesajFotoAdapter(getContext(),fotoUrlListesi);
        fotoViewPager.setAdapter(adapter);
        yonetici.CokluFotoIndir(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar);
    }

}
