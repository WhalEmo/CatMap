package com.beem.catmap.Profil;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import static com.beem.catmap.ui.navigation.NavigationExtensionsKt.handleBackPressWithEngine;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beem.catmap.CevrimIciYonetimi;
import com.beem.catmap.Profil.Gonderiler.GonderiYuklemeListener;
import com.beem.catmap.KullaniciAuth.HesapSil;
import com.beem.catmap.KullaniciAuth.Kullanici;
import com.beem.catmap.Profil.Gonderiler.GonderiAdapter;
import com.beem.catmap.data.session.CurrentUserManager;
import com.beem.catmap.mesaj.MesajlasmaYonetici;

import com.beem.catmap.BottomSheetController;
import com.beem.catmap.R;
import com.beem.catmap.UyariMesaji;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import android.Manifest;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


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
    private Button sohbetButon;
    private Button takipEdiliyorButonu;
    private ConstraintLayout myConstraintLayout;
    private ImageView PPmenuButton;
    private Button engelButonu;
    private LinearLayout engelLayout;
    private TextView KullaniciAdiEngel;
    private String isim;
    private RecyclerView gonderiRecyclerView;
    private GonderiAdapter gonderiAdapter;
    private TextView emptyTextView;
    private boolean takiptenDonuldu = false;
    private ShimmerFrameLayout shimmerLayout;
    private Boolean gonderiGeri=true;
    private TextView gonderiSayisiTextView;
    private TextView gonderilerBaslikTextView;
    private View overlay;
    private ProgressBar progressBar;
   private SwipeRefreshLayout swipeRefreshLayout;

    private static final String KEY_YUKLEYEN_ID = "yukleyenID";

    private CurrentUserManager currentUserManager;


       if(yukleyenID.equals(currentUserManager.getCurrentUserId())) {
           Log.d("NAV_BACK_DEDEKTOR",yukleyenID + "yükleyen id benim");
           PPmenuButton.setVisibility(View.VISIBLE);
           ProfilDuzenleme.setVisibility(View.VISIBLE);
           gonderilerBaslikTextView.setVisibility(View.VISIBLE);
           takipEtButonu.setVisibility(View.GONE);
           sohbetButon.setVisibility(View.GONE); // -> burası ben ekledim aşkım kendi profilimize bakarken sohbet butonunu gizledim<3
           String cacheURL = ProfileCacheManager.INSTANCE.getProfileUrl(requireContext(), yukleyenID);
           if (cacheURL != null) {
               print("if (cacheURL != null) { 521");
               logProfileFlow(cacheURL, yukleyenID, cacheURL, "main ama ifde", yukleyenID);
               print(cacheURL);
               Picasso.get()
                       .load(cacheURL)
                       .networkPolicy(NetworkPolicy.OFFLINE)
                       .placeholder(R.drawable.kullanici)
                       .into(profilResmiImageView, new com.squareup.picasso.Callback() {
                           @Override
                           public void onSuccess() {
                               // Cache’den başarıyla yüklendi, başka bir şey yapmaya gerek yok
                               print("onSuccess() 531");
                           }
                           @Override
                           public void onError(Exception e) {
                               // Cache’den yüklenemezseinternetten yükle
                               print("onError(Exception e) { 537");
                               Picasso.get()
                                       .load(cacheURL)
                                       .fit()
                                       .centerCrop()
                                       .placeholder(R.drawable.kullanici)
                                       .into(profilResmiImageView);
                           }
                       });
           } else {
               mViewModel.UrlLiveData().removeObservers(getViewLifecycleOwner());
               mViewModel.profilFotoUrlGetirVeCachele(requireContext(), currentUserManager.getCurrentUserId());
               mViewModel.UrlLiveData().observe(getViewLifecycleOwner(), guncelPP -> {
                   print(guncelPP);
                   print("551");
                   if (guncelPP != null) {
                       logProfileFlow(cacheURL, yukleyenID, guncelPP, "main kullanıcı ama elsede", yukleyenID);
                       if (!guncelPP.trim().isEmpty() && !guncelPP.equals(ProfileCacheManager.VALUE_EMPTY)) {
                           Picasso.get()
                                   .load(guncelPP)
                                   .fit()
                                   .centerCrop()
                                   .placeholder(R.drawable.kullanici)
                                   .into(profilResmiImageView);
                           mViewModel.UrlLiveData().removeObservers(getViewLifecycleOwner());
                       } else {
                           print("Firestore'da resim yok, varsayılan görsel set edildi.");
                           profilResmiImageView.setImageResource(R.drawable.kullanici);
                       }
                       mViewModel.UrlLiveData().removeObservers(getViewLifecycleOwner());
                   }
               });
           }
           HakkindaUI();
           KullaniciAdiUI();
           ucNokta();
           takipciGorme(currentUserManager.getCurrentUserId());
           takipleriGorme(currentUserManager.getCurrentUserId());
           mViewModel.GonderiSayisiniCek(currentUserManager.getCurrentUserId());
           mViewModel.GonderiSayisi().observe(getViewLifecycleOwner(), sayi -> {
               if (sayi != null) {
                   gonderiSayisiTextView.setText(sayi.toString());
               }
           });

           mViewModel.GonderiCekme(currentUserManager.getCurrentUserId(),uyariMesaji,new GonderiYuklemeListener() {
               @Override
               public void onTumGonderilerYuklendi() {
                   showLoading(false);
               }
           });

           mViewModel.kediGonderi().observe(getViewLifecycleOwner(), gonderilist -> {
               if (gonderilist == null || gonderilist.isEmpty()) {
                   emptyTextView.setVisibility(View.VISIBLE);
                   gonderiRecyclerView.setVisibility(View.GONE);
                   showLoading(false);
               } else {
                   emptyTextView.setVisibility(View.GONE);
                   gonderiRecyclerView.setVisibility(View.VISIBLE);
                   if (gonderiAdapter == null) {
                       gonderiAdapter = new GonderiAdapter(gonderilist,true);
                       gonderiGeri=gonderiAdapter.gerigitti;
                       gonderiRecyclerView.setAdapter(gonderiAdapter);
                   } else {
                       gonderiRecyclerView.setAdapter(gonderiAdapter);
                       gonderiAdapter.guncelleList(gonderilist);
                       gonderiGeri=gonderiAdapter.gerigitti;
                   }
                   showLoading(false);
               }
               if (shimmerLayout.getVisibility() == View.VISIBLE) {
                   shimmerLayout.stopShimmer();
                   shimmerLayout.setVisibility(View.GONE);
                   myConstraintLayout.setVisibility(View.VISIBLE);
               }
           });

           profiliDuzenleTiklandi.setOnClickListener(p -> {
               BottomSheetAc();
           });
       }else {
           Log.d("NAV_BACK_DEDEKTOR",yukleyenID + "yükleyen id başkasına ait");
           mViewModel.benimEngellediklerimiiGetir();
           ObserveDataSınıfı.observeOnce(mViewModel.BenimEngellediklerimLiveData(), getViewLifecycleOwner(), engelliler -> {
               if (engelliler.contains(yukleyenID)) {
                   takipEtButonu.setVisibility(View.GONE);
                   takipEdiliyorButonu.setVisibility(View.GONE);
                   engelButonu.setVisibility(View.VISIBLE);
               }
           });

           mViewModel.EngellileriGetir(yukleyenID);
           ObserveDataSınıfı.observeOnce(mViewModel.BeniEngelleyenlerLiveData(), getViewLifecycleOwner(), engelliler -> {
               if (engelliler.contains(currentUserManager.getCurrentUserId())) {
                   myConstraintLayout.setVisibility(View.GONE);
                   engelLayout.setVisibility(View.VISIBLE);
                   PPmenuButton.setVisibility(View.VISIBLE);
                   KullaniciAdiEngel.setText(isim);
               }
           });

           PPmenuButton.setVisibility(View.VISIBLE);
           ProfilDuzenleme.setVisibility(View.GONE);

           btnBack.setVisibility(View.VISIBLE);

           SohbetButonCalistir(); // -> burda butonun onClick listenırını  aktifleştirdim aşkım

           mViewModel.GonderiSayisiniCek(yukleyenID);
           mViewModel.GonderiSayisi().observe(getViewLifecycleOwner(), sayi -> {
               if (sayi != null) {
                   gonderiSayisiTextView.setText(sayi.toString());
               }
           });

           mViewModel.beniTakipEdiyorMu(yukleyenID);
           mViewModel.takipEdiliyorMu(yukleyenID);
           mViewModel.getTakipDurumuCift().observe(getViewLifecycleOwner(), pair -> {
               Boolean benTakipEdiyorum = pair.first != null && pair.first;
               Boolean oBeniTakipEdiyor = pair.second != null && pair.second;

               if (benTakipEdiyorum) {
                   sohbetButon.setVisibility(View.VISIBLE);
                   takipEtButonu.setVisibility(View.GONE);
                   takipEdiliyorButonu.setVisibility(View.VISIBLE);
                   gonderilerBaslikTextView.setVisibility(View.VISIBLE);
                   mViewModel.GonderiCekme(yukleyenID,uyariMesaji,new GonderiYuklemeListener() {
                       @Override
                       public void onTumGonderilerYuklendi() {
                           showLoading(false); // örneğin progressBar'ı kapat
                       }
                   });
                   mViewModel.kediGonderi().observe(getViewLifecycleOwner(), gonderilist -> {
                       if (gonderilist == null || gonderilist.isEmpty()) {
                           emptyTextView.setVisibility(View.VISIBLE);
                           gonderiRecyclerView.setVisibility(View.GONE);
                           showLoading(false);
                       } else {
                           emptyTextView.setVisibility(View.GONE);
                           gonderiRecyclerView.setVisibility(View.VISIBLE);
                           gonderiAdapter = new GonderiAdapter(gonderilist, true);
                           gonderiRecyclerView.setAdapter(gonderiAdapter);
                       }

                   });

               } else {
                   takipEdiliyorButonu.setVisibility(View.GONE);
                   gonderilerBaslikTextView.setVisibility(View.GONE);
                   takipEtButonu.setVisibility(View.VISIBLE);
                   gonderiRecyclerView.setVisibility(View.GONE);
                   sohbetButon.setVisibility(View.GONE);

                   if (oBeniTakipEdiyor) {
                       takipEtButonu.setText("Sende takip et");
                   } else {
                       takipEtButonu.setText("Takip Et");
                   }
               }
               if (shimmerLayout.getVisibility() == View.VISIBLE) {
                   shimmerLayout.stopShimmer();
                   shimmerLayout.setVisibility(View.GONE);
                   myConstraintLayout.setVisibility(View.VISIBLE);
               }
               showLoading(false);
           });
           mViewModel.UrlLiveData().removeObservers(getViewLifecycleOwner());
           mViewModel.profilFotoUrlGetirVeCachele(requireContext(),yukleyenID);
           mViewModel.UrlLiveData().observe(getViewLifecycleOwner(), guncelPP -> {
               ProfileCacheManager.INSTANCE.saveProfileUrl(requireContext(), yukleyenID, guncelPP);
               if (guncelPP != null) {
                   if (!guncelPP.equals(ProfileCacheManager.VALUE_EMPTY)) {
                       Picasso.get()
                               .load(guncelPP)
                               .fit()
                               .centerCrop()
                               .placeholder(R.drawable.kullanici)
                               .into(profilResmiImageView);
                   } else {
                       profilResmiImageView.setImageResource(R.drawable.kullanici);
                   }
                   mViewModel.UrlLiveData().removeObservers(getViewLifecycleOwner());
               }
           });

           HakkindaUI();
           KullaniciAdiUI();
           mViewModel.takipEdiliyorMu(yukleyenID);
           ObserveDataSınıfı.observeOnce(mViewModel.getTakipDurumu(), getViewLifecycleOwner(), durum -> {
               if (durum == true) {
                   takipciSayisiTextView.setClickable(true);
                   takipEdilenSayisiTextView.setClickable(true);
                   takipciGorme(yukleyenID);
                   takipleriGorme(yukleyenID);
               } else {
                   takipciSayisiTextView.setClickable(false);
                   takipEdilenSayisiTextView.setClickable(false);
               }
           });
           takipEtme();
           cikarma();
           Engelliduzen(uyariMesaji);
       }
        return view;
    }
    public void Engelliduzen(UyariMesaji uyari) {
        PPmenuButton.setOnClickListener(b -> {
            mViewModel.beniTakipEdiyorMu(yukleyenID);
            ObserveDataSınıfı.observeOnce(mViewModel.getBeniTakipEdiyor(), getViewLifecycleOwner(), takipEdiyor -> {
                PopupMenu popupmenu = new PopupMenu(requireContext(), PPmenuButton);
                popupmenu.getMenuInflater().inflate(R.menu.profil_uc_nokta_menu, popupmenu.getMenu());

                MenuItem engelleItem = popupmenu.getMenu().findItem(R.id.profilmenu_engelle);
                ArrayList<String> engelliler = mViewModel.BenimEngellediklerimLiveData().getValue();
                if (engelliler != null && engelliler.contains(yukleyenID)) {
                    engelleItem.setTitle("Engeli Kaldır");
                } else {
                    engelleItem.setTitle("Engelle");
                }

                MenuItem takipciCikarItem = popupmenu.getMenu().findItem(R.id.profiltakipciCikar);
                if (Boolean.TRUE.equals(takipEdiyor)) {
                    takipciCikarItem.setVisible(true);
                } else {
                    takipciCikarItem.setVisible(false);
                }

                popupmenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    String mevcutBaslik = item.getTitle().toString();

                    if (id == R.id.profilmenu_engelle && mevcutBaslik.equals("Engelle")) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(kullaniciadi.getText().toString())
                                .setMessage("Bu kullanıcıyı engellemek istiyor musunuz?")
                                .setPositiveButton("Evet", (dialog, which) -> {
                                    uyari.YuklemeDurum("Engelleniyor...");
                                    mViewModel.engelle(yukleyenID, currentUserManager.getCurrentUserId(),uyariMesaji);
                                    mViewModel.TakiptenCikarma(yukleyenID);
                                    mViewModel.TakipcidenCikarma(yukleyenID);
                                    mViewModel.TakipTakipciSayisi(yukleyenID, requireContext());
                                    MesajlasmaYonetici.getInstance().MesajlasmaEngelle(yukleyenID);
                                    String text = takipciSayisiTextView.getText().toString().trim();
                                    int takipci = Integer.parseInt(text) - 1;
                                    takipciSayisiTextView.setText(String.valueOf(takipci));
                                    String text2= takipEdilenSayisiTextView.getText().toString().trim();
                                    int takipci2 = Integer.parseInt(text2) - 1;
                                    takipEdilenSayisiTextView.setText(String.valueOf(takipci2));
                                    takipciSayisiTextView.setClickable(false);
                                    takipEdilenSayisiTextView.setClickable(false);
                                    uyari.BasariliDurum("Engellendi",1000);
                                    item.setTitle("Engeli Kaldır");
                                    popupmenu.dismiss();
                                })
                                .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                .show();
                        return true;
                    } else if (id == R.id.profilmenu_engelle && mevcutBaslik.equals("Engeli Kaldır")) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(kullaniciadi.getText().toString())
                                .setMessage("Bu kullanıcının engelini kaldırmak istiyor musunuz?")
                                .setPositiveButton("Evet", (dialog, which) -> {
                                    uyari.YuklemeDurum("Engel kaldırılıyor...");
                                    mViewModel.engelKaldir(yukleyenID, currentUserManager.getCurrentUserId(),uyariMesaji);
                                    mViewModel.TakipTakipciSayisi(yukleyenID,requireContext());
                                    takipciSayisiTextView.setClickable(true);
                                    takipEdilenSayisiTextView.setClickable(true);
                                    MesajlasmaYonetici.getInstance().MesajlasmaEngellemeKaldir(yukleyenID);
                                    takipciGorme(yukleyenID);
                                    takipleriGorme(yukleyenID);
                                    item.setTitle("Engelle");
                                    uyari.BasariliDurum("Engel kaldırıldı...",1000);
                                    popupmenu.dismiss();
                                })
                                .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                .show();
                        return true;
                    } else if (id == R.id.profiltakipciCikar) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(kullaniciadi.getText().toString())
                                .setMessage("Bu takipçiyi çıkarmak istiyor musunuz?")
                                .setPositiveButton("Evet", (dialog, which) -> {
                                    mViewModel.TakipcidenCikarma(yukleyenID);
                                    String text = takipEdilenSayisiTextView.getText().toString().trim();
                                    int takip_edilen = Integer.parseInt(text) - 1;
                                    takipEdilenSayisiTextView.setText(String.valueOf(takip_edilen));
                                    popupmenu.dismiss();
                                })
                                .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                                .show();
                        return true;
                    }else if(id==R.id.mesajGonder){
                        SohbetGecis();
                    }


                    return false;
                });

                popupmenu.show();
            });
        });
    }
    public void ucNokta(){
        PPmenuButton.setOnClickListener(b->{
            PopupMenu popupMenu=new PopupMenu(requireContext(),b);
            popupMenu.getMenuInflater().inflate(R.menu.kendippucnokta, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                if(item.getItemId()==R.id.Engellenenler){
                    SmartNavigationEngine.navigateTo(Screen.BLOCKED_USERS);
                    return true;
                }
                else if(item.getItemId()==R.id.HesabiKapat){
                    HesabiKapat();
                }
                else if(item.getItemId()==R.id.CikisYap){
                    CikisYap();
                }

                return false;
            });

            popupMenu.show();

        });
    }

    public void takipciGorme(String Id) {
        takipciSayisiTextView.setOnClickListener(b -> {
            takiptenDonuldu=true;
            myConstraintLayout.setVisibility(View.GONE);

            if (requireActivity() instanceof BottomSheetController) {
                ((BottomSheetController) requireActivity()).hideBottomSheet();
            }

            Bundle bundle = new Bundle();
            bundle.putString(KEY_YUKLEYEN_ID, Id);
            bundle.putInt("startPage", 0); // 0 = Takipçiler

            SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, bundle, "FOLLOWERS" + Id);
        });
    }

    public void takipleriGorme(String Id) {
        takipEdilenSayisiTextView.setOnClickListener(t -> {
            takiptenDonuldu=true;
            myConstraintLayout.setVisibility(View.GONE);

            if (requireActivity() instanceof BottomSheetController) {
                ((BottomSheetController) requireActivity()).hideBottomSheet();
            }

            Bundle bundle = new Bundle();
            bundle.putString(KEY_YUKLEYEN_ID, Id);
            bundle.putInt("startPage", 1); // 1 = Takipler (Takip edilenler)
            SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, bundle, "FOLLOWING" + Id);
        });
    }

    public void takipEtme() {
        takipEtButonu.setOnClickListener(t -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(kullaniciadi.getText().toString())
                    .setMessage("Bu kullanıcıyı takip etmek istiyor musunuz?")
                    .setPositiveButton("Evet", (dialog, which) -> {
                        String text = takipciSayisiTextView.getText().toString().trim();
                        int takip_edilen = Integer.parseInt(text) + 1;
                        takipciSayisiTextView.setText(String.valueOf(takip_edilen));
                        mViewModel.TakipEt(yukleyenID);
                        mViewModel.beniTakipEdiyorMu(yukleyenID);
                        takipciSayisiTextView.setClickable(true);
                        takipEdilenSayisiTextView.setClickable(true);
                        takipciGorme(yukleyenID);
                        takipleriGorme(yukleyenID);
                    })
                    .setNegativeButton("Hayır", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        });
    }

   public void cikarma(){
       takipEdiliyorButonu.setOnClickListener(c -> {
           new AlertDialog.Builder(requireContext())
                   .setTitle(kullaniciadi.getText().toString())
                   .setMessage("Bu kullanıcıyı takip etmeyi bırakmak istiyor musunuz?")
                   .setPositiveButton("Evet", (dialog, which) -> {
                       String text = takipciSayisiTextView.getText().toString().trim();
                       int takip_edilen = Integer.parseInt(text) - 1;
                       takipciSayisiTextView.setText(String.valueOf(takip_edilen));
                       mViewModel.TakiptenCikarma(yukleyenID);
                       mViewModel.takipEdiliyorMu(yukleyenID);
                       takipciSayisiTextView.setClickable(false);
                       takipEdilenSayisiTextView.setClickable(false);
                   })
                   .setNegativeButton("Hayır", (dialog, which) -> {
                       dialog.dismiss();
                   })
                   .show();
       });
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
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_profili_duzenle, null);

        bottom.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {

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

        String cacheURL= ProfileCacheManager.INSTANCE.getProfileUrl(requireContext(), currentUserManager.getCurrentUserId());
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
            profilFotoDuzenle.setImageResource(R.drawable.kullanici);
        }

        duzenleYazisi.setOnClickListener(v->{
            fotoSecimDialoguGoster();
            });

      kaydetButonu.setOnClickListener(k->{
          uyariMesaji.YuklemeDurum("Kaydediliyor");
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
                 ObserveDataSınıfı.observeOnce(mViewModel.kullaniciAdi(), getViewLifecycleOwner(), kAdii -> {
                     kullaniciadi.setText(kAdii);
                 });

             }
          }
          if(kAdi.equals(mevcutKullaniciAdi)) {
              uyariMesaji.BasariliDurum("Kaydedildi", 1000);
          }
          bottom.dismiss();
      });
        bottom.setContentView(sheetView);
        bottom.show();
    }

    private void SohbetButonCalistir(){ // -> burda buton ile mesajlaşma fragmentı çalıştırdım aşkım
        sohbetButon.setOnClickListener(v->{
            SohbetGecis();
        });
    }
    private void SohbetGecis(){// -> burda buton ile mesajlaşma fragmentı çalıştırdım aşkım
        MesajlasmaYonetici.getInstance().DinleyiciKaldir();

        Kullanici alici = new Kullanici();
        alici.setID(yukleyenID);
        MesajlasmaYonetici.getInstance().setAlici(alici);

        SmartNavigationEngine.navigateTo(
                Screen.MESSAGE,
                null,
                yukleyenID
        );
    }

    private void CikisYap(){
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Çıkış Onayı");
            builder.setMessage("Uygulamadan çıkış yapmak istediğinize emin misiniz?");
            builder.setCancelable(false);

            builder.setPositiveButton("EVET", (dialog, which) -> {

                if (currentUserManager.isUserLoggedIn()) {
                    CevrimIciYonetimi.getInstance().CevrimIciYonetimiDurdur(currentUserManager.getCurrentUser());
                }
                MesajlasmaYonetici.getInstance().MesajlasmaYonetimiDurdur();
                currentUserManager.logout();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                ShreadSil("KullaniciKayit");
                ShreadSil("ProfilPrefs");
                ShreadSil("begenilenYanitCache");
                ShreadSil("begenilenSetYanit");
                ShreadSil("begeniSayilariMapYanit");
                ShreadSil("begenilenYorumCache");
                ShreadSil("begenilenSet");
                ShreadSil("begeniSayilariMap");

                SmartNavigationEngine.navigateTo(Screen.AUTH);

            });

            builder.setNegativeButton("HAYIR", (dialog, which) -> {
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
    }

    private void ShreadSil(String shIsim){
        SharedPreferences kayit = getActivity().getSharedPreferences(shIsim, MODE_PRIVATE);
        SharedPreferences.Editor editor2 = kayit.edit();
        editor2.clear();
        editor2.apply();
    }

    private void HesabiKapat(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Kapatma Onayı");
        builder.setMessage("Hesabınızı silmek istediğinize emin misiniz?");
        builder.setCancelable(false);
        builder.setPositiveButton("EVET", (dialog, which) -> {
            UyariMesaji uyari = new UyariMesaji(requireContext(),false);
            uyari.YuklemeDurum("Hesap siliniyor...");
            HesapSil hesapSil = new HesapSil(requireContext());
            hesapSil.HesapSilmeBaslat(()->{
                if (currentUserManager.isUserLoggedIn()) {
                    CevrimIciYonetimi.getInstance().CevrimIciYonetimiDurdur(currentUserManager.getCurrentUser());
                }
                MesajlasmaYonetici.getInstance().MesajlasmaYonetimiDurdur();
                currentUserManager.logout();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                ShreadSil("KullaniciKayit");
                ShreadSil("ProfilPrefs");
                ShreadSil("begenilenYanitCache");
                ShreadSil("begenilenSetYanit");
                ShreadSil("begeniSayilariMapYanit");
                ShreadSil("begenilenYorumCache");
                ShreadSil("begenilenSet");
                ShreadSil("begeniSayilariMap");
                uyari.BasariliDurum("Hesap silindi",1000);
                SmartNavigationEngine.navigateTo(Screen.AUTH);
            });

        });

        builder.setNegativeButton("HAYIR", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void logProfileFlow(String cacheUrl, String userId, String url, String tag, String cacheKey) {
        boolean isCacheUrlNull = cacheUrl == null;
        boolean isLiveUrlNull = url == null;
        boolean isUserIdNull = userId == null;
        boolean isCacheKeyNull = cacheKey == null;

        String cleanTag = tag != null ? tag.toUpperCase() : "UNKNOWN";

        Log.d("PROFILE_PHOTO_FLOW", "=========================== " + cleanTag + " ============================");
        Log.d("PROFILE_PHOTO_FLOW", "🔍 [PROFIL AKIŞI] -> Hedef Kullanıcı ID: " + (isUserIdNull ? "NULL!" : userId));
        Log.d("PROFILE_PHOTO_FLOW", "🔑 SharedPreferences Key: " + (isCacheKeyNull ? "❌ NULL İSTİSNASI!" : cacheKey));

        // 📦 Cihaz Hafızası (Cache) Durumu
        if (isCacheUrlNull) {
            Log.d("PROFILE_PHOTO_FLOW", "💾 Cihaz Hafızası (Cache): ❌ BOŞ (Null) -> Veri internetten istenecek.");
        } else {
            Log.d("PROFILE_PHOTO_FLOW", "💾 Cihaz Hafızası (Cache): 🟢 DOLU (Mevcut) -> Link: " + cacheUrl);
        }

        // 🌐 Canlı Veri / Firebase Durumu
        if (isLiveUrlNull) {
            Log.d("PROFILE_PHOTO_FLOW", "🌐 Firestore / Canlı Veri: ⏳ Henüz veri dönmedi veya URL yok.");
        } else {
            Log.d("PROFILE_PHOTO_FLOW", "🌐 Firestore / Canlı Veri: ✅ YENİ VERİ GELDİ -> Link: " + url);

            // Sinsi bir durum: Cache'teki veri ile internetten gelen veri aynı mı?
            if (!isCacheUrlNull && url.equals(cacheUrl)) {
                Log.d("PROFILE_PHOTO_FLOW", "🔄 Durum Analizi: Hafızadaki resim ile gelen resim AYNI. Ekran titremeyecek.");
            } else if (!isCacheUrlNull) {
                Log.d("PROFILE_PHOTO_FLOW", "⚡ Durum Analizi: ⚠️ RESİM DEĞİŞMİŞ! Eski cache güncellenecek.");
            }
        }
        Log.d("PROFILE_PHOTO_FLOW", "=======================================================");
    }

    private void print(String message){
        String safeMessage = (message != null) ? message : "null_value";
        Log.d("PROFILE_PRINT", safeMessage);
    }
}

 */