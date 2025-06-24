package com.emrullah.catmap;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MesajFragment extends Fragment {

    private RecyclerView mesajRecyclerView;
    private LinearLayout mesaj_gonder_layout;
    private EditText mesajEditText;
    private ImageView gonderButton;
    private ArrayList<Mesaj> mesajArrayList;
    private MesajAdapter adapter;
    private MesajlasmaYonetici mesajlasmaYonetici;
    private Kullanici alici;
    private Kullanici gonderici = MainActivity.kullanici;
    private Context context;

    public static MesajFragment newInstance(Context context){
        return new MesajFragment(context);
    }
    public MesajFragment(Context context){
        this.context = context;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mesajlasma, container, false);

        mesajRecyclerView = view.findViewById(R.id.mesajRecyclerView);
        mesaj_gonder_layout = view.findViewById(R.id.mesaj_gonder_layout);
        mesajEditText = view.findViewById(R.id.mesajEditText);
        gonderButton = view.findViewById(R.id.gonderButton);
        alici = new Kullanici();
        alici.setID("oVtMwJS69picHgRTqEYR");

        mesajArrayList = new ArrayList<>();
        adapter = new MesajAdapter(mesajArrayList, getActivity()); // burası sıkıntı çıkartabilir
        mesajRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(false);
        mesajRecyclerView.setLayoutManager(layoutManager);

        mesajlasmaYonetici = new MesajlasmaYonetici(gonderici.getID(), alici.getID());

        gonderButton.setOnClickListener(v->{ MesajGondermeButonu(); });


        // Burada RecyclerView kur, mesajları çek
        return view;
    }

    private void MesajGondermeButonu(){
        if (mesajEditText.getText().toString().trim().isEmpty()) return;
        if (mesajlasmaYonetici == null) return;
        mesajlasmaYonetici.MesajGonder(gonderici.getID(),mesajEditText.getText().toString().trim(),adapter);
        mesajEditText.getText().clear();
        mesajRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

}
