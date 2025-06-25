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
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;

public class MesajFragment extends Fragment {

    private RecyclerView mesajRecyclerView;
    private LinearLayout mesaj_gonder_layout;
    private EditText mesajEditText;
    private ImageView gonderButton;
    private ProgressBar yukleniyorProgress;
    private ArrayList<Mesaj> mesajArrayList;
    private MesajAdapter adapter;
    private MesajlasmaYonetici mesajlasmaYonetici;
    private Kullanici alici;
    private Kullanici gonderici = MainActivity.kullanici;
    private Context context;
    private boolean yukleniyorMu = false;

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
        yukleniyorProgress = view.findViewById(R.id.yukleniyorProgress);
        alici = new Kullanici();
        alici.setID("A8mt0DjcK1oulvcZFWtU");

        mesajArrayList = new ArrayList<>();
        adapter = new MesajAdapter(mesajArrayList, getActivity()); // burası sıkıntı çıkartabilir
        mesajRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(false);
        mesajRecyclerView.setLayoutManager(layoutManager);

        mesajlasmaYonetici = new MesajlasmaYonetici(gonderici.getID(), alici.getID(), ()->{
            mesajlasmaYonetici.MesajlariCek(adapter,20,yukleniyorProgress,mesajRecyclerView,()->{
                mesajlasmaYonetici.MesajlariDinle(adapter,()->{
                    mesajRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                });
            }); //ilk mesajları çek
        });

        gonderButton.setOnClickListener(v->{ MesajGondermeButonu(); });

        ScrollDinleyici();
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

    private void ScrollDinleyici(){
        mesajRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager menajer = (LinearLayoutManager) recyclerView.getLayoutManager();
                int sonPozisyon = menajer.findFirstVisibleItemPosition();
                if (sonPozisyon == 0 && !yukleniyorMu) {
                    yukleniyorMu = true;
                    mesajlasmaYonetici.MesajlariCek(mesajArrayList.get(0).getLongZaman(),adapter,20,()->{
                        yukleniyorMu = false;
                    });
                }

            }

        });
    }

}
