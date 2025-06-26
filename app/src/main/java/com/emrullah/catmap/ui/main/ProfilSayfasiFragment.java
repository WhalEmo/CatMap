package com.emrullah.catmap.ui.main;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
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
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.ObserveDataSınıfı;
import com.emrullah.catmap.R;
import com.emrullah.catmap.UyariMesaji;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import android.Manifest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;


public class ProfilSayfasiFragment extends Fragment {
    private MainViewModel mViewModel;
    private TextView kullaniciadi;
    private ImageView profilResmiImageView;
    private TextView takipciSayisiTextView;
    private TextView takipEdilenSayisiTextView;
    private TextView bioTextView;
    private Uri photoUri;
    private File photoFile;
    private UyariMesaji uyariMesaji;
    private Button profiliDuzenleTiklandi;
    private ImageView profilFotoDuzenle;
    private Bitmap bitmap = null;
    private Button kaydetButonu;
    private String yukleyenID;;
    private LinearLayout ProfilDuzenleme;
    private Button takipEtButonu;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }
    private Uri getPhotoFileUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + ".jpg";

        File storageDir = requireContext().getExternalFilesDir("ProfilFoto"); // PROFİL klasörü (özelleştirilmiş)

        try {
            photoFile = File.createTempFile(fileName, ".jpg", storageDir);
            return FileProvider.getUriForFile(requireContext(),"com.emrullah.catmap.fileprovider", photoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Galeriye gitmek ve secmek için ActivityResultContracts kullanalım
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        kaydetButonu.setEnabled(false);
                        kaydetButonu.setAlpha(0.5f);
                        kaydetButonu.setText("Yükleniyor");
                        photoUri  = result.getData().getData();  // Seçilen fotoğrafın URI'si
                        Picasso.get()
                                .load(photoUri)
                                .placeholder(R.drawable.kullanici)
                                .fit()
                                .centerCrop()
                                .into(profilFotoDuzenle,new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        // resim gerçekten yüklendiğinde çalışır
                                         bitmap = ((BitmapDrawable) profilFotoDuzenle.getDrawable()).getBitmap();
                                         kaydetButonu.setEnabled(true);
                                         kaydetButonu.setText("Kaydet");
                                         kaydetButonu.setAlpha(1.f);
                                    }

                                    @Override
                                    public void onError(Exception e) { }
                                });
                    }
                }
            });


    // 📌 Kameraya gotur ve sonucu bas
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        kaydetButonu.setEnabled(false);
                        kaydetButonu.setAlpha(0.5f);
                        kaydetButonu.setText("Yükleniyor");
                        if (photoUri != null) {
                            // 📸 Çekilen yüksek kaliteli fotoğrafı ImageView'da göster
                            profilFotoDuzenle.setImageURI(photoUri);
                            // Resim yüklendikten sonra işlemi tetikle
                            profilFotoDuzenle.post(() -> {
                                Drawable drawable = profilFotoDuzenle.getDrawable();
                                if (drawable != null) {
                                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                                    kaydetButonu.setEnabled(true);
                                    kaydetButonu.setAlpha(1.0f);
                                    kaydetButonu.setText("Kaydet");

                                }
                            });
                        }
                    }
                }
            });



    // 📌 Kamerayı açma metodu
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            photoUri = getPhotoFileUri();
            if (photoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);//put extra kamera aktivitesinde cektigimiz fotoyu
                //yukleme aktivitisine gonderir
                cameraLauncher.launch(takePictureIntent);
            } else {
                Log.e("CameraError", "Dosya oluşturulamadı!");
            }
        } else {
            Log.e("CameraIntent", "Kamera uygulaması bulunamadı!");
        }
    }


    private void TakipTakipciSayilariUI() {
        SharedPreferences sp = requireContext().getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
        Long cacheTakipedilen = sp.getLong("cache_takip", 0L);
        Long cacheTakipci = sp.getLong("cache_takipci", 0L);

        takipEdilenSayisiTextView.setText(cacheTakipedilen.toString());
        takipciSayisiTextView.setText(cacheTakipci.toString());

        mViewModel.TakipTakipciSayisi(MainActivity.kullanici.getID(), requireContext());

        mViewModel.takipEdilenSayisiLiveData().observe(getViewLifecycleOwner(), takipEdilenSayisi -> {
            if (!takipEdilenSayisi.equals(cacheTakipedilen)) {
                takipEdilenSayisiTextView.setText(takipEdilenSayisi.toString());
            }
        });

        mViewModel.takipciSayisiLiveData().observe(getViewLifecycleOwner(), takipciSayisi -> {
            if (!takipciSayisi.equals(cacheTakipci)) {
                takipciSayisiTextView.setText(takipciSayisi.toString());
            }
        });
    }
    private void HakkindaUI(){
        if(yukleyenID==MainActivity.kullanici.getID()) {
            SharedPreferences sp = requireContext().getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
            String cacheHakkinda = sp.getString("Hakkinda", null);
            if (cacheHakkinda != null) {
                bioTextView.setText(cacheHakkinda.trim());
            } else {
                mViewModel.HakkindaGetir(MainActivity.kullanici.getID());
                ObserveDataSınıfı.observeOnce(mViewModel.hakkinda(), getViewLifecycleOwner(), guncel -> {
                    bioTextView.setText(guncel);
                });
            }
        }else{
            mViewModel.HakkindaGetir(yukleyenID);
            ObserveDataSınıfı.observeOnce(mViewModel.hakkinda(), getViewLifecycleOwner(), guncel -> {
                bioTextView.setText(guncel);
            });
        }
    }
    private void KullaniciAdiUI(){
      if(yukleyenID==MainActivity.kullanici.getID()) {
          SharedPreferences sp = requireContext().getSharedPreferences("KullaniciKayit", MODE_PRIVATE);
          String cacheKAd = sp.getString("KullaniciAdi", null);
          kullaniciadi.setText(cacheKAd.trim());
      }else{
















      }

    }
    public static ProfilSayfasiFragment newInstance(String yukleyenID) {
        ProfilSayfasiFragment fragment = new ProfilSayfasiFragment();
        Bundle args = new Bundle();
        args.putString("yukleyenID", yukleyenID);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            yukleyenID = getArguments().getString("yukleyenID");
        }
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        kullaniciadi= view.findViewById(R.id.KullaniciAdi);
        bioTextView=view.findViewById(R.id.bioTextView);
        profilResmiImageView = view.findViewById(R.id.profilFotoImageView);
        takipciSayisiTextView=view.findViewById(R.id.takipciSayisiTextView);
        takipEdilenSayisiTextView=view.findViewById(R.id.takipEdilenSayisiTextView);
        profiliDuzenleTiklandi=view.findViewById(R.id.profiliDuzenleTiklandi);
        takipEtButonu=view.findViewById(R.id.takipEtButonu);
        ProfilDuzenleme=view.findViewById(R.id.ProfilDuzenleme);
        uyariMesaji=new UyariMesaji(requireContext(),true);


       if(yukleyenID==MainActivity.kullanici.getID()) {
           ProfilDuzenleme.setVisibility(View.VISIBLE);
           takipEtButonu.setVisibility(View.GONE);
           SharedPreferences sp = requireContext().getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
           String cacheURL = sp.getString("profil_url", null);
           if (cacheURL != null) {
               Picasso.get()
                       .load(cacheURL)
                       .networkPolicy(NetworkPolicy.OFFLINE)
                       .placeholder(R.drawable.kullanici)
                       .into(profilResmiImageView, new com.squareup.picasso.Callback() {
                           @Override
                           public void onSuccess() {
                               // Cache’den başarıyla yüklendi, başka bir şey yapmaya gerek yok
                           }

                           @Override
                           public void onError(Exception e) {
                               // Cache’den yüklenemezseinternetten yükle
                               Picasso.get()
                                       .load(cacheURL)
                                       .fit()
                                       .centerCrop()
                                       .placeholder(R.drawable.kullanici)
                                       .into(profilResmiImageView);
                           }
                       });
           } else {
               mViewModel.profilFotoUrlGetirVeCachele(requireContext(), MainActivity.kullanici.getID());
               ObserveDataSınıfı.observeOnce(mViewModel.UrlLiveData(), getViewLifecycleOwner(), guncelPP -> {
                   if (guncelPP != null) {
                       Picasso.get()
                               .load(guncelPP)
                               .fit()
                               .centerCrop()
                               .placeholder(R.drawable.kullanici)
                               .into(profilResmiImageView);
                   } else {
                       profilResmiImageView.setImageResource(R.drawable.kullanici);
                   }
               });
           }
           TakipTakipciSayilariUI();
           HakkindaUI();
           KullaniciAdiUI();

           profiliDuzenleTiklandi.setOnClickListener(p -> {
               BottomSheetAc();
           });
       }else if(yukleyenID!=MainActivity.kullanici.getID()){
           ProfilDuzenleme.setVisibility(View.GONE);
           takipEtButonu.setVisibility(View.VISIBLE);

           mViewModel.profilFotoUrlGetirVeCachele(requireContext(),yukleyenID);
           ObserveDataSınıfı.observeOnce(mViewModel.UrlLiveData(), getViewLifecycleOwner(), guncelPP -> {
               if (guncelPP != null) {
                   Picasso.get()
                           .load(guncelPP)
                           .fit()
                           .centerCrop()
                           .placeholder(R.drawable.kullanici)
                           .into(profilResmiImageView);
               } else {
                   profilResmiImageView.setImageResource(R.drawable.kullanici);
               }
           });

           HakkindaUI();

       }
        return view;
    }

    private void fotoSecimDialoguGoster() {
        String[] secenekler = {"Galeriden Seç", "Kamerayla Çek"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profil Fotoğrafı Seç")
                .setItems(secenekler, (dialog, which) -> {
                    if (which == 0) {
                        // Galeriye gitmek için Intent başlatıyoruz
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);  // Intent'i ActivityResultLauncher ile başlatıyoruz
                    } else {
                        // Uygulamanın kamera iznine sahip olup olmadığını kontrol eder.
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
                        } else {
                            openCamera();
                        }
                    }
                })
                .show();
    }
    private void BottomSheetAc(){
        BottomSheetDialog bottom= new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_duzenle, null);

        bottom.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // 1. Behavior al
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                // 2. Yüksekliği tam ekran yap
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(layoutParams);

                // 3. BottomSheet'i expanded moda al (tam ekran gibi)
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        EditText KullaniciAdi = sheetView.findViewById(R.id.editKullaniciAdi);
        EditText Hakkinda = sheetView.findViewById(R.id.editBio);
        kaydetButonu=sheetView.findViewById(R.id.kaydetButonu);
        TextView duzenleYazisi=sheetView.findViewById(R.id.fotoDegistirText);
        profilFotoDuzenle=sheetView.findViewById(R.id.profilFotoImageViewDuzenle);

       String mevcutKullaniciAdi=kullaniciadi.getText().toString();
       String mevcutHakkinda=bioTextView.getText().toString();

       Hakkinda.setText(mevcutHakkinda.trim());
       Hakkinda.setSelection(mevcutHakkinda.length());
       KullaniciAdi.setText(mevcutKullaniciAdi.trim());
       KullaniciAdi.setSelection(mevcutKullaniciAdi.length());

        SharedPreferences sp=requireContext().getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
        String cacheURL=sp.getString("profil_url", null);
        if (cacheURL != null) {
            Picasso.get()
                    .load(cacheURL)
                    .fit()
                    .centerCrop()
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.kullanici)
                    .into(profilFotoDuzenle, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            // Cache’den başarıyla yüklendi, başka bir şey yapmaya gerek yok
                        }

                        @Override
                        public void onError(Exception e) {
                            // Cache’den yüklenemezseinternetten yükle
                            Picasso.get()
                                    .load(cacheURL)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilFotoDuzenle);
                        }
                    });
        } else {
            // Eğer cacheURL yoksa placeholder göster
            profilFotoDuzenle.setImageResource(R.drawable.kullanici);
        }

        duzenleYazisi.setOnClickListener(v->{
            fotoSecimDialoguGoster();
            });

      kaydetButonu.setOnClickListener(k->{
          if (bitmap != null) {
              profilResmiImageView.setImageBitmap(bitmap);
              mViewModel.profilFotoUrlKaydetFirebaseVeCachele(photoUri,requireContext());
          }

          String kAdi=KullaniciAdi.getText().toString().trim();
          String hakkinda=Hakkinda.getText().toString().trim();

          if (!hakkinda.equals(mevcutHakkinda)) {
              mViewModel.HakkindaDBEkle(hakkinda, requireContext());
              ObserveDataSınıfı.observeOnce(mViewModel.hakkinda(), getViewLifecycleOwner(), guncelHakkinda -> {
                  if (guncelHakkinda != null) {
                      bioTextView.setText(guncelHakkinda);
                  }
              });
          }

          if(!kAdi.equals(mevcutKullaniciAdi)){
              mViewModel.KAdiDBekle(kAdi,requireContext(),uyariMesaji);
             if(uyariMesaji.DahaOnceAlinmisMi==false) {
                 ObserveDataSınıfı.observeOnce(mViewModel.kullaniciAdi, getViewLifecycleOwner(), kAdii -> {
                     kullaniciadi.setText(kAdii);
                 });

             }
          }
          bottom.dismiss();
      });
        bottom.setContentView(sheetView);
        bottom.show();
    }

}