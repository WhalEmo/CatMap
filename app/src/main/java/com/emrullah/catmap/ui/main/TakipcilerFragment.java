package com.emrullah.catmap.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.emrullah.catmap.UyariMesaji;
import com.emrullah.catmap.Yorum_Model;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TakipcilerFragment extends Fragment {
    private RecyclerView recyclerView;
    private Kullanicilar_adapter adapter;
    private List<Kullanici> kullaniciList = new ArrayList<>();
    private FirebaseFirestore db;
    private String id;
   private ProgressBar progressBar;

    public static TakipcilerFragment newInstance(String profilID) {
        TakipcilerFragment fragment = new TakipcilerFragment();
        Bundle args = new Bundle();
        args.putString("profilID", profilID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        View view = inflater.inflate(R.layout.fragment_takipciler, container, false);
        progressBar = view.findViewById(R.id.progressBarTakipciler);
        recyclerView=view.findViewById(R.id.recyclerViewTakipciler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        // ViewModelProvider Android'de bir ViewModel nesnesi üretmeye ve yönetmeye yarayan bir sınıftır.
        adapter=new Kullanicilar_adapter(requireContext(),kullaniciList,viewModel);

        recyclerView.setAdapter(adapter);
        // ID'yi alıyoruz
        if (getArguments() != null) {
            id = getArguments().getString("profilID");
        } else {
            id = MainActivity.kullanici.getID(); // Yedek olarak kendi ID'miz
        }

        veriCekTakipciler(id,viewModel);

        adapter.setKullaniciAdiTiklamaListener(kullaniciId -> {
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
    public void veriCekTakipciler(String Id, MainViewModel viewModel){
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .document(Id)
                .collection("takipciler")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots->{
                    kullaniciList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String ad = doc.getString("KullaniciAdi");
                        String url=doc.getString("profilFotoUrl");
                        String Idsi=doc.getString("ID");
                       Kullanici kullanici=new Kullanici();
                       kullanici.setKullaniciAdi(ad);
                       viewModel.takipciMi=true;
                       kullanici.setFotoUrl(url);
                       kullanici.setID(Idsi);
                       kullaniciList.add(kullanici);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                });
    }
}

