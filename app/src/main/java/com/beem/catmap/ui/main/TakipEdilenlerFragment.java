package com.beem.catmap.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.lifecycle.ViewModelProvider;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.Kullanici;
import com.beem.catmap.MainActivity;
import com.beem.catmap.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TakipEdilenlerFragment extends Fragment {
    private RecyclerView recyclerView;
    private Kullanicilar_adapter adapter;
    private List<Kullanici> kullaniciList = new ArrayList<>();
    private FirebaseFirestore db;
    private String id;
    private ProgressBar progressBar;
    public static TakipEdilenlerFragment newInstance(String profilID) {
        TakipEdilenlerFragment fragment = new TakipEdilenlerFragment();
        Bundle args = new Bundle();
        args.putString("profilID", profilID);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        View view = inflater.inflate(R.layout.fragment_takipedilenler, container, false);
        progressBar = view.findViewById(R.id.progressBarTakipedilenler);
        recyclerView=view.findViewById(R.id.recyclerViewTakipedilenler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        adapter=new Kullanicilar_adapter(requireContext(),kullaniciList,viewModel);

        recyclerView.setAdapter(adapter);
        // ID'yi alıyoruz
        if (getArguments() != null) {
            id = getArguments().getString("profilID");
        } else {
            id = MainActivity.kullanici.getID(); // Yedek olarak kendi ID'miz
        }

        veriCekTakipedilenler(id);

        adapter.setKullaniciAdiTiklamaListener(kullaniciId -> {
            ProfilSayfasiFragment fragment = ProfilSayfasiFragment.newInstance(kullaniciId); // 👈 burada kullandık
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();

        });


        return view;
    }
    public void veriCekTakipedilenler(String Id){
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .document(Id)
                .collection("takipEdilenler")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots->{
                    kullaniciList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String ad = doc.getString("KullaniciAdi");
                        String url=doc.getString("profilFotoUrl");
                        String Idsi=doc.getString("ID");
                        Kullanici kullanici=new Kullanici();
                        kullanici.setKullaniciAdi(ad);
                        kullanici.TakipEdiyorMuyum=2;
                        kullanici.setFotoUrl(url);
                        kullanici.setID(Idsi);
                        kullaniciList.add(kullanici);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE); // yükleniyor gizle
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE); // hata olsa da gizle
                    // İstersen hata mesajı gösterebilirsin
                });
    }
}
