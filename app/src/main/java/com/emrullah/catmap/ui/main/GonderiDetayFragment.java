package com.emrullah.catmap.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.MapsActivity;
import com.emrullah.catmap.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class GonderiDetayFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ARG_FOTO_LIST = "fotoListesi";
    private static final String ARG_KEDI_ADI = "kediAdi";
    private static final String ARG_ACIKLAMA = "aciklama";
    private static final String ARG_BEGENİ="begeni";

    private ArrayList<String> fotoListesi;
    private String kediAdi;
    private String aciklama;
    private Long begeni;

    public static GonderiDetayFragment newInstance(ArrayList<String> fotoListesi, String kediAdi, String aciklama, Long begeni) {
        GonderiDetayFragment fragment = new GonderiDetayFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_FOTO_LIST, fotoListesi);
        args.putString(ARG_KEDI_ADI, kediAdi);
        args.putString(ARG_ACIKLAMA, aciklama);
        args.putLong(ARG_BEGENİ, begeni != null ? begeni : 0L);
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
            begeni = getArguments().getLong(ARG_BEGENİ, 0L); // default 0L
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false);
        ShimmerFrameLayout shimmerLayout = view.findViewById(R.id.shimmerLayout);
        View icerikLayout = view.findViewById(R.id.gercekIcerikLayout);
        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        icerikLayout.setVisibility(View.GONE);

        ViewPager2 viewPager = view.findViewById(R.id.fotoPager);
        TextView kediAdiText = view.findViewById(R.id.kediAdiText);
        TextView aciklamaText = view.findViewById(R.id.kediAciklama);
        TextView haritadaGorText=view.findViewById(R.id.haritadaGorText);
        TextView begeniBilgiTextView=view.findViewById(R.id.begeniBilgiTextView);

        // Simüle veri yüklemesi: örnek olarak 500ms sonra içeriği göster
        view.postDelayed(() -> {
            // Gerçek verileri bağla
            viewPager.setAdapter(new FotoAdapter(fotoListesi));
            kediAdiText.setText(kediAdi);
            aciklamaText.setText(aciklama);

            if (begeni != 0) {
                String bilgi = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni);
                begeniBilgiTextView.setText(bilgi);
            } else {
                begeniBilgiTextView.setText("Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!");
            }
            haritadaGorText.setOnClickListener(b->{

            });

            if (shimmerLayout.getVisibility() == View.VISIBLE) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                icerikLayout.setVisibility(View.VISIBLE);
            }
        }, 500);
        return view;
    }
}
