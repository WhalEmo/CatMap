package com.beem.catmap.Profil.engellenenler;




import static com.beem.catmap.ui.navigation.NavigationExtensionsKt.handleBackPressWithEngine;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import com.beem.catmap.MainActivity;


import com.beem.catmap.R;
import com.beem.catmap.UyariMesaji;
import com.beem.catmap.Profil.MainViewModel;
import com.beem.catmap.Profil.ProfilSayfasiFragment;
import com.beem.catmap.data.repository.UserRepository;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;


import java.util.ArrayList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleBackPressWithEngine(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.engellenenler, container, false);

        recycler=view.findViewById(R.id.engellenenRecyclerView);
        adapter = new engellenenlerAdapter(requireContext(),new ArrayList<>());
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        mViewModel.benimEngellediklerimiiGetir();
        mViewModel.BenimEngellediklerimLiveData().observe(getViewLifecycleOwner(),liste->{
            mViewModel.kullaniiclariGetir(liste);
        });

        mViewModel.BenimEngellediklerimKullaniciLiveData().observe(getViewLifecycleOwner(),list->{
            adapter.setListe(list);
            adapter.notifyDataSetChanged();
        });

        UserRepository userRepository = UserRepository.Companion.getInstance(requireContext());

        adapter.setOnEngelClickListener(kullanici -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(kullanici.getKullaniciAdi());
            builder.setMessage("Bu kullanıcının engelini kaldırmak istiyor musunuz?");

            builder.setPositiveButton("Evet", (dialog, which) -> {
                mViewModel.engelKaldir(kullanici.getID(), userRepository.getCurrentUserId(), uyari);
                adapter.remove(kullanici);
            });

            builder.setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        });

        adapter.setOnadClickListener(kullaniciId -> {
            Bundle args = ProfilSayfasiFragment.newArgs(kullaniciId);
            SmartNavigationEngine.navigateTo(
                    Screen.OTHER_PROFILE,
                    args,
                    kullaniciId
            );
        });
        return view;
    }

}