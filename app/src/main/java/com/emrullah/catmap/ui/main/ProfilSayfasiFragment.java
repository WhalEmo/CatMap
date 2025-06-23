package com.emrullah.catmap.ui.main;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.emrullah.catmap.R;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import android.Manifest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;


public class ProfilSayfasiFragment extends Fragment {
    private MainViewModel mViewModel;
    private TextView kullaniciadi;
    private ActivityResultLauncher<Intent> galeriLauncher;
    private ActivityResultLauncher<Intent> kameraLauncher;
    private ImageView profilResmiImageView;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            kameraCek();
        }
        if (requestCode == 103 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            galeriSec();
        }
    }

    private void kameraIzinIste() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            kameraCek();
        }
    }

    private void galeriIzinIste() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 103);
            } else {
                galeriSec();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 103);
            } else {
                galeriSec();
            }
        }
    }


    private Uri bitmapToUri(Bitmap bitmap) {
        try {
            File tempFile = new File(requireContext().getCacheDir(), "camera_image.jpg");
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            return FileProvider.getUriForFile(
                    requireContext(),
                    "com.emrullah.catmap.fileprovider",
                    tempFile
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }




    public static ProfilSayfasiFragment newInstance() {
        return new ProfilSayfasiFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        kullaniciadi= view.findViewById(R.id.KullaniciAdi);
        profilResmiImageView = view.findViewById(R.id.profilFotoImageView);

        SharedPreferences sp=requireContext().getSharedPreferences("ProfilPrefs", Context.MODE_PRIVATE);
        String cacheURL=sp.getString("profil_url", null);
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
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilResmiImageView);
                        }
                    });
        } else {
            // Eğer cacheURL yoksa placeholder göster
            profilResmiImageView.setImageResource(R.drawable.kullanici);
        }

        mViewModel.profilFotoUrlGetirVeCachele(requireContext());

        mViewModel.getProfilFotoUrl().observe(getViewLifecycleOwner(), url -> {//otomatik tetiklenir
            if (url != null && !url.isEmpty()) {
                Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.kullanici)
                        .into(profilResmiImageView);
            }
        });


        galeriLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri secilenFoto = data.getData();
                            Picasso.get()
                                    .load(secilenFoto)
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilResmiImageView);
                            mViewModel.profilFotoUrlKaydetFirebaseVeCachele(secilenFoto,requireContext());
                        }
                    }
                }
        );

        // Kamera Çekimi Launcher
        kameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getExtras() != null) {
                            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                            profilResmiImageView.setImageBitmap(bitmap);
                            Uri uri = bitmapToUri(bitmap);
                            if (uri != null) {
                                // Firebase'e kaydet + SharedPreferences'e yaz
                                mViewModel.profilFotoUrlKaydetFirebaseVeCachele(uri, requireContext());
                            }
                        }
                    }
                }
        );
        // Tıklama
        profilResmiImageView.setOnClickListener(v -> {
            fotoSecimDialoguGoster();
        });

        return view;
    }
    private void fotoSecimDialoguGoster() {
        String[] secenekler = {"Galeriden Seç", "Kamerayla Çek"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profil Fotoğrafı Seç")
                .setItems(secenekler, (dialog, which) -> {
                    if (which == 0) {
                        galeriIzinIste();
                    } else {
                        kameraIzinIste();
                    }
                })
                .show();
    }

    private void galeriSec() {
        Intent galeriIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriIntent.setType("image/*");
        galeriLauncher.launch(galeriIntent);
    }

    private void kameraCek() {
        Intent kameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        kameraLauncher.launch(kameraIntent);
    }


}