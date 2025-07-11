package com.emrullah.catmap.mesaj;

import static android.app.Activity.RESULT_OK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MesajFragment extends Fragment {

    private final int REQUEST_CODE_GALERI = 100;

    private RecyclerView mesajRecyclerView;
    private LinearLayout mesaj_gonder_layout;
    private EditText mesajEditText;
    private ImageView gonderButton;
    private ProgressBar yukleniyorProgress;
    private ArrayList<Mesaj> mesajArrayList;
    private MesajAdapter adapter;
    private MesajlasmaYonetici mesajlasmaYonetici = MesajlasmaYonetici.getInstance();
    private Context context;
    private boolean yukleniyorMu = false;
    private ImageView kisiProfilFoto;
    private TextView kisiAdiText;
    private TextView kisiDurumText;
    private LinearLayout kisiBilgiLayout;
    private ImageButton fotoEkleButton;
    private ActivityResultLauncher<Intent> galeriLauncher;

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

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true){
                    @Override
                    public void handleOnBackPressed() {
                        mesajlasmaYonetici.DinleyiciKaldir();
                        mesajlasmaYonetici.setAlici(null);
                        mesajlasmaYonetici.setSohbetID(null);
                      //  requireActivity().getSupportFragmentManager().popBackStack();
                        requireActivity().getSupportFragmentManager().beginTransaction().remove(MesajFragment.this).commit();
                    }
                });


        mesajRecyclerView = view.findViewById(R.id.mesajRecyclerView);
        mesaj_gonder_layout = view.findViewById(R.id.mesaj_gonder_layout);
        mesajEditText = view.findViewById(R.id.mesajEditText);
        gonderButton = view.findViewById(R.id.gonderButton);
        fotoEkleButton = view.findViewById(R.id.fotoEkleButton);
        yukleniyorProgress = view.findViewById(R.id.yukleniyorProgress);
        yukleniyorProgress.setVisibility(View.VISIBLE);
        KlavyeAyari(view);
        kisiProfilFoto = view.findViewById(R.id.kisiProfilFoto);
        kisiAdiText = view.findViewById(R.id.kisiAdiText);
        kisiDurumText = view.findViewById(R.id.kisiDurumText);
        kisiBilgiLayout = view.findViewById(R.id.kisi_bilgi_layout);

        //profil işlemleri
        mesajlasmaYonetici.ProfilCubugunuDoldur(kisiAdiText,kisiProfilFoto,kisiDurumText);


        mesajArrayList = new ArrayList<>();
        adapter = new MesajAdapter(mesajArrayList, getActivity()); // burası sıkıntı çıkartabilir
        mesajRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(false);
        mesajRecyclerView.setLayoutManager(layoutManager);

        adapter.setGoster(new Runnable() {      /// burası MesajFotoGosterFragment ı çalıştırıyor
            @Override
            public void run() {
                MesajFotoGosterFragment fragment = new MesajFotoGosterFragment();
                fragment.show(requireActivity().getSupportFragmentManager(), "mesajFotoGoster");
            }
        });

        mesajlasmaYonetici.MesajlasmaYoneticiStart(()->{
            mesajlasmaYonetici.MesajlariCek(adapter,20,yukleniyorProgress,mesajRecyclerView,()->{
                mesajlasmaYonetici.MesajlariDinle(adapter,()->{
                    mesajRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                });
                mesajlasmaYonetici.SilDinleyici(adapter);
                mesajlasmaYonetici.GuncelleDinleyici(adapter);
            });
            YaziyorMuCalistir();
            mesajlasmaYonetici.YaziyorDinleyici(kisiDurumText);
            mesajlasmaYonetici.CevrimIciDinleyici(kisiDurumText);
        });

        gonderButton.setOnClickListener(v->{ MesajGondermeButonu(); });

        kisiProfilFoto.setOnClickListener(v->{ ProfilSayfasinaYonlendir(); });

        GaleriLauncheriHazirla();
        fotoEkleButton.setOnClickListener(v->{ GaleriyeYonlendir(); });

        ScrollDinleyici();
        // Burada RecyclerView kur, mesajları çek
        return view;
    }

    private void MesajGondermeButonu(){
        if (mesajEditText.getText().toString().trim().isEmpty()) return;
        if (mesajlasmaYonetici == null) return;
        mesajlasmaYonetici.MesajGonder(mesajEditText.getText().toString().trim(),adapter);
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


    private void KlavyeAyari(View rootView){
        LinearLayout mesajLayout = mesaj_gonder_layout;

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()); // Klavye boyutu
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Sistem barları
            boolean klavyeAcik = imeInsets.bottom > 0;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mesajLayout.getLayoutParams();

            int klavyeYuksekligi = imeInsets.bottom;
            int sistemCubuğuYuksekligi = navInsets.bottom;

            int netYukseklik = klavyeYuksekligi - sistemCubuğuYuksekligi;
            if (netYukseklik < 0) netYukseklik = 0;

            params.bottomMargin = klavyeAcik ? netYukseklik + dpToPx(4) : dpToPx(8);
            mesajLayout.setLayoutParams(params);
            return insets;
        });

    }

    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }



    private void YaziyorMuCalistir(){
        mesajEditText.addTextChangedListener(new TextWatcher() {
            Timer timer = new Timer();
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(mesajEditText.getText().toString().trim().isEmpty()) return;
                mesajlasmaYonetici.YaziyorMu(true);
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mesajlasmaYonetici.YaziyorMu(false);
                    }
                },1000);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }


    private void ProfilSayfasinaYonlendir(){
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, ProfilSayfasiFragment.newInstance(mesajlasmaYonetici.getAlici().getID()));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void GaleriyeYonlendir(){
        Intent galeriIntent = new Intent(Intent.ACTION_PICK);
        galeriIntent.setType("image/*");
        galeriIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        galeriLauncher.launch(Intent.createChooser(galeriIntent, "Fotoğraf Seç"));
    }

    private void GaleriLauncheriHazirla(){
        galeriLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result ->{
                    if(result.getResultCode() == RESULT_OK){
                        Intent data = result.getData();
                        if(data.getClipData() != null){
                            for(int i=0; i<data.getClipData().getItemCount(); i++){
                                MesajFotoGonderYonetici.getInstance().UriEkle(data.getClipData().getItemAt(i).getUri());
                            }
                        }
                        else if(data.getData() != null){
                            MesajFotoGonderYonetici.getInstance().UriEkle(data.getData());
                        }
                        MesajFotoGonderYonetici.getInstance().GondericiStart(adapter);
                    }
                }
        );
    }



}
