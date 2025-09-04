package com.emrullah.catmap.engellenenler;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.emrullah.catmap.FotoYuklemeListener;
import com.emrullah.catmap.GonderiYuklemeListener;
import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.mesaj.MesajFragment;
import com.emrullah.catmap.mesaj.MesajlasmaYonetici;

import com.emrullah.catmap.BottomSheetController;
import com.emrullah.catmap.MapsActivity;
import com.emrullah.catmap.ObserveDataSınıfı;
import com.emrullah.catmap.R;
import com.emrullah.catmap.UyariMesaji;
import com.emrullah.catmap.YuklemeArayuzuActivity;
import com.emrullah.catmap.sohbet.SohbetYonetici;
import com.emrullah.catmap.ui.main.MainViewModel;
import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import android.Manifest;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


public class engellenenlerFragmnet extends Fragment {
    private MainViewModel mViewModel;
    private RecyclerView recycler;
    private engellenenlerAdapter adapter;
    private UyariMesaji uyari;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        uyari=new UyariMesaji(requireContext(),true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.engellenenler, container, false);

        recycler=view.findViewById(R.id.engellenenRecyclerView);
         adapter = new engellenenlerAdapter(requireContext(),new ArrayList<>());
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        mViewModel.benimEngellediklerimiiGetir();
        mViewModel.BenimEngellediklerimLiveData().observe(getViewLifecycleOwner(),liste->{
            mViewModel.kullaniiclariGetir(liste);
        });

        mViewModel.BenimEngellediklerimKullaniciLiveData().observe(getViewLifecycleOwner(),list->{
            adapter.setListe(list);
            adapter.notifyDataSetChanged();
        });

        adapter.setOnEngelClickListener(kullanici -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(kullanici.getKullaniciAdi())
                    .setMessage("Bu kullanıcının engelini kaldırmak istiyor musunuz?")
                    .setPositiveButton("Evet", (dialog, which) -> {
                        mViewModel.engelKaldir(kullanici.getID(), MainActivity.kullanici.getID(),uyari);
                    })
                    .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                    .show();
            adapter.remove(kullanici);
        });
        adapter.setOnadClickListener(kullaniciId -> {
            ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(kullaniciId);
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        return view;
    }

}