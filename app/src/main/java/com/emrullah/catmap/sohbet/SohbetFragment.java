package com.emrullah.catmap.sohbet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.Mesaj;
import com.emrullah.catmap.MesajAdapter;
import com.emrullah.catmap.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SohbetFragment extends Fragment {

    private RecyclerView kisilerRecyclerView;
    private SohbetAdapter adapter;
    private ArrayList<Sohbet> sohbetler;
    private Runnable MesajFragment;


    public SohbetFragment(Runnable MesajFragment) {
        this.MesajFragment = MesajFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sohbetler, container, false);
        sohbetler = new ArrayList<>();
        adapter = new SohbetAdapter(sohbetler, getActivity(),MesajFragment);
        kisilerRecyclerView = view.findViewById(R.id.kisilerRecyclerView);
        kisilerRecyclerView.setAdapter(adapter);
        SohbetYonetici sohbetYonetici = SohbetYonetici.getInstance();
        sohbetYonetici.SohbetleriCek(sohbetler, ()->{
            adapter.notifyDataSetChanged();
        });

        return view;
    }


  /*private DatabaseReference mesajlar = FirebaseDatabase.getInstance().getReference("mesajlar");
  public void MesajGonder(String mesaj){
        String mesajID = mesajlar.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("gonderen", MainActivity.kullanici.getID());
        veri.put("mesaj",mesaj);
        veri.put("zaman",System.currentTimeMillis());
        veri.put("goruldu",false);
        String sohbetID = "y1l5bbCjazN5fBCaz7nv_"+MainActivity.kullanici.getID();
        mesajlar.child(sohbetID).child(mesajID).setValue(veri);
    }*/
}
