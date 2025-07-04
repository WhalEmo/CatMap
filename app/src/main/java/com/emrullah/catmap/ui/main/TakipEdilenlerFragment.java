package com.emrullah.catmap.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TakipEdilenlerFragment extends Fragment {
    private RecyclerView recyclerView;
    private Kullanicilar_adapter adapter;
    private List<Kullanici> kullaniciList = new ArrayList<>();
    private FirebaseFirestore db;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_takipedilenler, container, false);
        recyclerView=view.findViewById(R.id.recyclerViewTakipedilenler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter=new Kullanicilar_adapter(requireContext(),kullaniciList);

        adapter.setKullaniciAdiTiklamaListener(kullaniciId -> {
            ProfilSayfasiFragment fragment = new ProfilSayfasiFragment();

            Bundle bundle = new Bundle();
            bundle.putString("yukleyenID", kullaniciId);
            fragment.setArguments(bundle);

            // Activity'deki fragment_container'a yönlendirme:
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        veriCekTakipedilenler();
        return view;
    }
    public void veriCekTakipedilenler(){
        db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipEdilenler")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots->{
                    kullaniciList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String ad = doc.getString("KullaniciAdi");
                        String url=doc.getString("profilFotoUrl");
                        Kullanici kullanici=new Kullanici();
                        kullanici.setKullaniciAdi(ad);
                        kullanici.setFotoUrl(url);
                        kullanici.setTakipEdiliyorMu(true);
                        kullaniciList.add(kullanici);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
