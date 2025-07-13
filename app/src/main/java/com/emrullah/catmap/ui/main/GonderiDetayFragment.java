package com.emrullah.catmap.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.FotoYuklemeListener;
import com.emrullah.catmap.KediSilmeDurumu;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.MapsActivity;
import com.emrullah.catmap.R;
import com.emrullah.catmap.UyariMesaji;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class GonderiDetayFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private View overlay;
    private ProgressBar progressBar;
    private static final String ARG_FOTO_LIST = "fotoListesi";
    private static final String ARG_KEDI_ADI = "kediAdi";
    private static final String ARG_ACIKLAMA = "aciklama";
    private static final String ARG_BEGENİ="begeni";
    private static final String ARG_KEDIID="kediid";
    UyariMesaji uyari;

    private ArrayList<String> fotoListesi;
    private String kediAdi;
    private String aciklama;
    private Long begeni;
    private String kediid;

    public static GonderiDetayFragment newInstance(ArrayList<String> fotoListesi, String kediAdi, String aciklama, Long begeni,String kediid) {
        GonderiDetayFragment fragment = new GonderiDetayFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_FOTO_LIST, fotoListesi);
        args.putString(ARG_KEDI_ADI, kediAdi);
        args.putString(ARG_ACIKLAMA, aciklama);
        args.putLong(ARG_BEGENİ, begeni != null ? begeni : 0L);
        args.putString(ARG_KEDIID,kediid);
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
            kediid=getArguments().getString(ARG_KEDIID);
        }
        uyari=new UyariMesaji(requireContext(),true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false);
        MainViewModel mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        progressBar = view.findViewById(R.id.progressBar);
        overlay = view.findViewById(R.id.overlayView);
        ViewPager2 viewPager = view.findViewById(R.id.fotoPager);
        TextView kediAdiText = view.findViewById(R.id.kediAdiText);
        TextView aciklamaText = view.findViewById(R.id.kediAciklama);
        TextView haritadaGorText=view.findViewById(R.id.haritadaGorText);
        TextView begeniBilgiTextView=view.findViewById(R.id.begeniBilgiTextView);
        ImageView GonderiMenu=view.findViewById(R.id.GonderiMenu);
        showLoading(true);

        viewPager.setAdapter(new FotoAdapter(fotoListesi, new FotoYuklemeListener() {
            @Override
            public void onTumFotograflarYuklendi() {
                showLoading(false); // ProgressBar burada kapanır
            }
        }));

        kediAdiText.setText(kediAdi);
            aciklamaText.setText(aciklama);

            if (begeni != 0) {
                String bilgi = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni);
                begeniBilgiTextView.setText(bilgi);
            } else {
                begeniBilgiTextView.setText("Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!");
            }

        mViewModel.getYukleyenID().observe(getViewLifecycleOwner(), id -> {
            if(id.equals(MainActivity.kullanici.getID())){
                GonderiMenu.setVisibility(View.VISIBLE);
                GonderiMenu.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), v);
                    popupMenu.getMenuInflater().inflate(R.menu.gonderi_uc_nokta, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int idsi = item.getItemId();
                        if (idsi == R.id.gonderi_sil) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Silme")
                                    .setMessage("Bu gönderiyi silmek istiyor musunuz?")
                                    .setPositiveButton("Evet", (dialog, which) -> {
                                        mViewModel.kullaniciyaGonderiSil(kediid,uyari);
                                        mViewModel.gonderiSil(kediid);
                                        requireActivity().getSupportFragmentManager().popBackStack();
                                        popupMenu.dismiss();
                                    })
                                    .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                    .show();
                            return true;
                        }else if(idsi==R.id.gonderiharita_sil){
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Silme")
                                    .setMessage("Kediyi haritadan silmek istiyor musunuz? Bu işlemi yaptığınızda, kediye ait gönderiler de silinecektir.")
                                    .setPositiveButton("Evet", (dialog, which) -> {
                                           mViewModel.HaritadanSilme(kediid, () -> {
                                            KediSilmeDurumu.getInstance().setSilindiMi(true);
                                            mViewModel.kullaniciyaGonderiSil(kediid,uyari);
                                            mViewModel.gonderiSil(kediid);
                                               if (getActivity() instanceof MapsActivity) {
                                                   ((MapsActivity) getActivity()).sonTiklananMarkeriSil();
                                               }
                                               requireActivity().getSupportFragmentManager().popBackStack();
                                        });
                                        popupMenu.dismiss();
                                    })
                                    .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                    .show();
                            return true;
                        }
                        return false;
                    });

                    popupMenu.show();
                });
            }else{
                GonderiMenu.setVisibility(View.GONE);
            }
        });
        haritadaGorText.setOnClickListener(b -> {
            if (getActivity() instanceof MapsActivity) {
                ((MapsActivity) getActivity()).HaritadaGor(kediid);
                new Handler().postDelayed(() -> {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }, 100);
            } else {
                Intent intent = new Intent(requireContext(), MapsActivity.class);
                intent.putExtra("kediId", kediid);
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
        return view;
    }
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
            // Arka planı da interaktif yapma (dokunulmaz yap)
            getActivity().getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            progressBar.setVisibility(View.GONE);
            overlay.setVisibility(View.GONE);
            getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
}
