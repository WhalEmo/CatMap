package com.emrullah.catmap;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

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
    private Bitmap profilResim;
    private ImageView kisiProfilFoto;
    private TextView kisiAdiText;
    private TextView kisiDurumText;
    private LinearLayout kisiBilgiLayout;
    private MesajFragment fragment;


    public static MesajFragment newInstance(Context context){
        return new MesajFragment(context);
    }
    public MesajFragment(Context context){
        this.context = context;
        fragment = this;
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
        yukleniyorProgress.setVisibility(View.VISIBLE);
        KlavyeAyari(view);
        alici = new Kullanici();
        alici.setID("A8mt0DjcK1oulvcZFWtU");

        kisiProfilFoto = view.findViewById(R.id.kisiProfilFoto);
        kisiAdiText = view.findViewById(R.id.kisiAdiText);
        kisiDurumText = view.findViewById(R.id.kisiDurumText);
        kisiBilgiLayout = view.findViewById(R.id.kisi_bilgi_layout);

        //profil işlemleri
        ProfilCubugunuDoldur();


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

    private void ProfilCubugunuDoldur(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(alici.getID())
                .get()
                .addOnSuccessListener(veri ->{
                    if(veri.exists()){
                        alici.setAd(veri.getString("Ad"));
                        alici.setFotoUrl(veri.getString("profilFotoUrl"));
                        alici.setSoyad(veri.getString("Soyad"));
                        alici.setKullaniciAdi(veri.getString("KullaniciAdi"));
                        kisiAdiText.setText(alici.getKullaniciAdi());
                        Picasso.get()
                                .load(alici.getFotoUrl())
                                .placeholder(R.drawable.kullanici)
                                .error(R.drawable.kullanici)
                                .into(kisiProfilFoto);
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




}
