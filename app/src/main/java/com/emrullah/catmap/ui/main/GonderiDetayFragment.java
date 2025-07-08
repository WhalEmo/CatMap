package com.emrullah.catmap.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.MapsActivity;
import com.emrullah.catmap.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class GonderiDetayFragment extends Fragment {
    private static final String ARG_FOTO_LIST = "fotoListesi";
    private static final String ARG_KEDI_ADI = "kediAdi";
    private static final String ARG_ACIKLAMA = "aciklama";

    private ArrayList<String> fotoListesi;
    private String kediAdi;
    private String aciklama;

    public static GonderiDetayFragment newInstance(ArrayList<String> fotoListesi, String kediAdi, String aciklama) {
        GonderiDetayFragment fragment = new GonderiDetayFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_FOTO_LIST, fotoListesi);
        args.putString(ARG_KEDI_ADI, kediAdi);
        args.putString(ARG_ACIKLAMA, aciklama);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fotoListesi = getArguments().getStringArrayList(ARG_FOTO_LIST);
            kediAdi = getArguments().getString(ARG_KEDI_ADI);
            aciklama = getArguments().getString(ARG_ACIKLAMA);
        }
    }
    public void ToplamBegeniSayisi(TextView begeniBilgiTextView ){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference kediRef = db.collection("cats").document(MapsActivity.kediID);
        kediRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long begeniSayisi = documentSnapshot.getLong("begeniSayisi");
                if (begeniSayisi != null) {
                    String bilgi = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeniSayisi);
                    begeniBilgiTextView.setText(bilgi);
                } else {
                    begeniBilgiTextView.setText("Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!");
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("BegeniSayisi", "Beğeni sayısı çekilemedi: " + e.getMessage());
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false);

        ViewPager2 viewPager = view.findViewById(R.id.fotoPager);
        TextView kediAdiText = view.findViewById(R.id.kediAdiText);
        TextView aciklamaText = view.findViewById(R.id.kediAciklama);
        TextView haritadaGorText=view.findViewById(R.id.haritadaGorText);
        TextView begeniBilgiTextView=view.findViewById(R.id.begeniBilgiTextView);

        viewPager.setAdapter(new FotoAdapter(fotoListesi));
        kediAdiText.setText(kediAdi);
        aciklamaText.setText(aciklama);

        ToplamBegeniSayisi(begeniBilgiTextView);


        haritadaGorText.setOnClickListener(b->{

        });

        return view;
    }
}
