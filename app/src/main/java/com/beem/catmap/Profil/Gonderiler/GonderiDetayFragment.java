package com.beem.catmap.Profil.Gonderiler;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.beem.catmap.Maps.FotoYuklemeListener;
import com.beem.catmap.Maps.MapKedi.KediSilmeDurumu;
import com.beem.catmap.MainActivity;
import com.beem.catmap.Maps.MapViewModel;
import com.beem.catmap.Maps.MapsActivity;
import com.beem.catmap.R;
import com.beem.catmap.UyariMesaji;
import com.beem.catmap.Profil.MainViewModel;
import com.beem.catmap.data.repository.UserRepository;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GonderiDetayFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ARG_FOTO_LIST = "fotoListesi";
    private static final String ARG_KEDI_ADI = "kediAdi";
    private static final String ARG_ACIKLAMA = "aciklama";
    private static final String ARG_BEGENİ="begeni";
    private static final String ARG_KEDIID="kediid";
    UyariMesaji uyari;

    private MapViewModel mapViewModel;

    private ArrayList<String> fotoListesi;
    private String kediAdi;
    private String aciklama;
    private Long begeni;
    private String kediid;

    private ViewPager2 photoPager;
    private LinearLayout photoDotsContainer;
    private MaterialCardView photoIndicatorCapsule;

    private final List<View> photoIndicatorDots = new ArrayList<>();

    private ViewPager2.OnPageChangeCallback photoPageChangeCallback;

    public static Bundle newBundle(ArrayList<String> fotoListesi, String kediAdi, String aciklama, Long begeni,String kediid) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_FOTO_LIST, fotoListesi);
        args.putString(ARG_KEDI_ADI, kediAdi);
        args.putString(ARG_ACIKLAMA, aciklama);
        args.putLong(ARG_BEGENİ, begeni != null ? begeni : 0L);
        args.putString(ARG_KEDIID,kediid);
        return args;
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
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            Window window = getActivity().getWindow();

            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.catmap_surface_white));

            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            Window window = getActivity().getWindow();

            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.catmap_background));

            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
        }
    }

    @Override
    public void onDestroyView() {
        if (photoPager != null && photoPageChangeCallback != null) {
            photoPager.unregisterOnPageChangeCallback(photoPageChangeCallback);
        }

        photoPager = null;
        photoDotsContainer = null;
        photoIndicatorCapsule = null;
        photoPageChangeCallback = null;

        photoIndicatorDots.clear();

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false);
        MainViewModel mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        TextView kediAdiText = view.findViewById(R.id.kediAdiText);
        TextView aciklamaText = view.findViewById(R.id.kediAciklama);
        TextView begeniBilgiTextView=view.findViewById(R.id.begeniBilgiTextView);
        ImageView GonderiMenu=view.findViewById(R.id.GonderiMenu);

        photoPager = view.findViewById(R.id.fotoPager);
        photoDotsContainer = view.findViewById(R.id.fotoDotsContainer);
        photoIndicatorCapsule = view.findViewById(R.id.fotoIndicatorCapsule);

        UserRepository userRepository = UserRepository.Companion.getInstance(requireContext());

        kediAdiText.setText(kediAdi);
            aciklamaText.setText(aciklama);

            if (begeni != 0) {
                String bilgi = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni);
                begeniBilgiTextView.setText(bilgi);
            } else {
                begeniBilgiTextView.setText("Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!");
            }

        mViewModel.getYukleyenID().observe(getViewLifecycleOwner(), id -> {
            if(id.equals(userRepository.getCurrentUserId())){
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
                                        SmartNavigationEngine.navigateBack();
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
                                               SmartNavigationEngine.navigateBack();
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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        MaterialButton haritadaGorButton = view.findViewById(R.id.haritadaGorButon);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            SmartNavigationEngine.navigateBack();
        });

        haritadaGorButton.setOnClickListener(b -> {
            if (kediid != null && !kediid.trim().isEmpty()) {
                SmartNavigationEngine.navigateTo(Screen.MAP);
                mapViewModel.requestZoomToCat(kediid);
            }
        });



        if (fotoListesi == null) {
            fotoListesi = new ArrayList<>();
        }

        photoPager.setAdapter(new FotoAdapter(fotoListesi, new FotoYuklemeListener() {
            @Override
            public void onTumFotograflarYuklendi() {
            }
        }));

        setupPhotoIndicator(fotoListesi.size());

        photoPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePhotoIndicator(position);
            }
        };

        photoPager.registerOnPageChangeCallback(photoPageChangeCallback);

    }


    private void setupPhotoIndicator(int photoCount) {
        photoDotsContainer.removeAllViews();
        photoIndicatorDots.clear();

        if (photoCount <= 1) {
            photoIndicatorCapsule.setVisibility(View.GONE);
            return;
        }

        photoIndicatorCapsule.setVisibility(View.VISIBLE);

        for (int i = 0; i < photoCount; i++) {
            View dot = new View(requireContext());

            boolean isSelected = i == 0;

            int dotSize = dpToPx(isSelected ? 8 : 6);
            int dotMargin = dpToPx(3);

            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(dotSize, dotSize);

            layoutParams.setMargins(
                    dotMargin,
                    0,
                    dotMargin,
                    0
            );

            dot.setLayoutParams(layoutParams);

            dot.setBackground(
                    ContextCompat.getDrawable(
                            requireContext(),
                            isSelected
                                    ? R.drawable.dot_active
                                    : R.drawable.dot_inactive
                    )
            );

            photoDotsContainer.addView(dot);
            photoIndicatorDots.add(dot);
        }
    }

    private void updatePhotoIndicator(int selectedPosition) {
        for (int i = 0; i < photoIndicatorDots.size(); i++) {
            View dot = photoIndicatorDots.get(i);

            boolean isSelected = i == selectedPosition;

            int dotSize = dpToPx(isSelected ? 8 : 6);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) dot.getLayoutParams();

            layoutParams.width = dotSize;
            layoutParams.height = dotSize;

            dot.setLayoutParams(layoutParams);

            dot.setBackground(
                    ContextCompat.getDrawable(
                            requireContext(),
                            isSelected
                                    ? R.drawable.dot_active
                                    : R.drawable.dot_inactive
                    )
            );
        }
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

}
