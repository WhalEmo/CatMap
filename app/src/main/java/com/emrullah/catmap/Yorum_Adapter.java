package com.emrullah.catmap;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class Yorum_Adapter extends RecyclerView.Adapter<Yorum_Adapter.YorumViewHolder>  {


    private ArrayList<Yorum_Model>yorumList;
    public ArrayList<Yorum_Model> getYorumList() {
        return yorumList;
    }
    public boolean yorumMuGeldi=false;
    private Set<String> begenilenYorumIDSeti = new HashSet<>();
    private Map<String, Integer> begeniSayisiMap = new HashMap<>();
    public KullaniciAdiTiklamaListener kullaniciAdiTiklamaListener;

    public void setKullaniciAdiTiklamaListener(KullaniciAdiTiklamaListener listener) {
        this.kullaniciAdiTiklamaListener = listener;
    }

    private Context context;
    private final Handler zamanHandler = new Handler();
    private final Runnable zamanRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < yorumList.size(); i++) {
                Yorum_Model yorum = yorumList.get(i);
                if (yorum.getTarih() == null) continue;
                long fark = System.currentTimeMillis() - yorum.getTarih().getTime();

                // Sadece 1 saatten küçük yorumlar için yenileme yap
                if (fark < 3600000 && fark >= 60000) {
                    notifyItemChanged(i);
                }
            }
            zamanHandler.postDelayed(this, 60000);
        }

    };
    private void kalpAnimasyonuYap(ImageView kalpView) {
        ScaleAnimation büyütKücült = new ScaleAnimation(
                0.7f, 1.2f,  // X ekseni
                0.7f, 1.2f,  // Y ekseni
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        büyütKücült.setDuration(200);  // milisaniye
        büyütKücült.setRepeatCount(1);
        büyütKücült.setRepeatMode(Animation.REVERSE); // tersine oynat

        kalpView.startAnimation(büyütKücült);
    }

    public void setBegenilenYorumIDSeti(Set<String> set) {
        this.begenilenYorumIDSeti = set;
    }

    public void setBegeniSayisiMap(Map<String, Integer> begeniMap) {
        this.begeniSayisiMap = begeniMap;
    }
    public void baslatZamanlayici() {
        zamanHandler.post(zamanRunnable);
    }

    public void durdurZamanlayici() {
        zamanHandler.removeCallbacks(zamanRunnable);
    }

    public Yorum_Adapter(ArrayList<Yorum_Model> yorumList, Context context) {
        this.yorumList = yorumList;
        this.context = context;
    }
    public static int yorumindeks;
    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yorum_icin,parent,false);
        return new YorumViewHolder(view);
    }
    int pozisyon=-1;

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {
        Yorum_Model yorum=yorumList.get(position);
        holder.kullaniciAditext.setText(yorum.getKullaniciAdi());
        holder.yorumText.setText(yorum.getYorumicerik());
        holder.yorumTarihiText.setText(yorum.duzenlenmisTarih());
        URLye_Ulasma ulasma=new URLye_Ulasma();
        ulasma.IDdenUrlyeUlasma(yorum.getYukleyenId(),holder.YorumFotoImageView);

        holder.kullaniciAditext.setOnClickListener(v -> {
            if (kullaniciAdiTiklamaListener != null) {
                kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(yorum.getYukleyenId());
            }
        });

        int begeniSayisi = begeniSayisiMap.getOrDefault(yorum.getYorumID(), 0);
        if (begeniSayisi >= 1_000_000) {
            double milyon = begeniSayisi / 1_000_000.0;
            holder.begeniSayisiTextView.setText(String.format("%.1f m", milyon));
        } else if (begeniSayisi >= 1_000) {
            double bin = begeniSayisi / 1_000.0;
            holder.begeniSayisiTextView.setText(String.format("%.1f bin", bin).replace('.', ','));
        } else {
            holder.begeniSayisiTextView.setText(String.valueOf(begeniSayisi));
        }

        if (begenilenYorumIDSeti.contains(yorum.getYorumID())) {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_24); // dolu kalp
            holder.kalpImageView.setTag("begenildi");
        } else {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24); // boş kalp
            holder.kalpImageView.setTag("begeniYok");
        }
        Begeni_Kod_Yoneticisi_Yorum begeniKodYoneticisi=new Begeni_Kod_Yoneticisi_Yorum();
        holder.kalpImageView.setOnClickListener(v->{
            if ("begeniYok".equals(holder.kalpImageView.getTag())) {
                begeniKodYoneticisi.YorumBegenme(yorum,MainActivity.kullanici.getID(),context,this);
                holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_24);
                kalpAnimasyonuYap(holder.kalpImageView);
                holder.kalpImageView.setTag("begenildi");
                begenilenYorumIDSeti.add(yorum.getYorumID());

            }else{
                begeniKodYoneticisi.YorumBegeniKladirma(yorum,MainActivity.kullanici.getID(),context,this);
                holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24);
                holder.kalpImageView.setTag("begeniYok");
                begenilenYorumIDSeti.remove(yorum.getYorumID());
            }
        });

        if (MainActivity.kullanici.getKullaniciAdi().equals(yorum.getKullaniciAdi())) {
            if(yorumMuGeldi==true){
                holder.menuButonu.setVisibility(View.GONE);
                holder.getYanitlarYukleniyorLayout2.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.getYanitlarYukleniyorLayout2.setVisibility(View.GONE);
                        holder.menuButonu.setVisibility(View.VISIBLE);
                        yorumMuGeldi = false;
                    }
                }, 3000);
            }else {
                holder.menuButonu.setVisibility(View.VISIBLE);
                holder.getYanitlarYukleniyorLayout2.setVisibility(View.GONE);
            }
        } else {
            holder.menuButonu.setVisibility(View.GONE);
        }

        if (yorum.isYanitlarGorunuyor()) {
            holder.container.setVisibility(View.VISIBLE);
            holder.yanitlariGor.setText("Yanıtları Gizle");

            ArrayList<Yanit_Model> yanitlar = yorum.getYanitlar();
            if (yanitlar == null) {
                yanitlar = new ArrayList<>();
                yorum.setYanitlar(yanitlar);
            }

            // ✨ SADECE BİR KERE ADAPTER OLUŞTUR
            if (yorum.getYanitAdapter() == null) {
                Yanit_Adapter yntadapter = new Yanit_Adapter(yanitlar, context,position,yorum.getYorumID());
                yorum.setYanitAdapter(yntadapter);
                yntadapter.baslatZamanlayici();
                yntadapter.hazirliklariYapBegenme(context, MainActivity.kullanici.getID(), yorum);

                yntadapter.setKullaniciAdiTiklamaListener(kullaniciAdiTiklamaListener);
            }

            // Adapter'i bağla
            holder.recyclerViewyanitlar.setLayoutManager(new LinearLayoutManager(context));
            holder.recyclerViewyanitlar.setAdapter(yorum.getYanitAdapter());



            if (yorum.isYanitYokMu()) {
                holder.dahafazla.setVisibility(View.GONE);
                if (yorum.getYanitlar().size() > 0) {
                    holder.yanityoksa.setVisibility(View.GONE);
                } else {
                    holder.yanityoksa.setVisibility(View.VISIBLE);
                }
            } else {
                holder.yanityoksa.setVisibility(View.GONE);
                if (yorum.isDahafazlaGozukuyorMu()) {
                    holder.dahafazla.setVisibility(View.VISIBLE);
                } else {
                    holder.dahafazla.setVisibility(View.GONE);
                }
            }


            // 🔄 SADECE 1 KERE VERİ ÇEK
            if (!yorum.isYanitlarYuklendi()) {
                holder.dahafazla.setVisibility(View.GONE);
                holder.yanitlarYukleniyorLayout.setVisibility(View.VISIBLE);
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum, yanitlar, yorum.getYanitAdapter(), 5, true, new Yanitlari_Cekme.YanitlarCallback() {
                            @Override
                            public void onComplete() {
                                yorum.setYanitlarYuklendi(true);
                                yorum.setYanitYokMu(yorum.getYanitlar().isEmpty());
                                holder.yanitlarYukleniyorLayout.setVisibility(View.GONE);
                                notifyDataSetChanged();
                            }
                        });
            }

            ArrayList<Yanit_Model> finalYanitlar = yanitlar;
            holder.dahafazla.setOnClickListener(dahafz -> {
                holder.yanitlarYukleniyorLayout.setVisibility(View.VISIBLE);
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum, finalYanitlar, yorum.getYanitAdapter(), 5, false, new Yanitlari_Cekme.YanitlarCallback() {
                    @Override
                    public void onComplete() {
                        yorum.getYanitAdapter().notifyDataSetChanged(); // yeni gelen veriler için
                        holder.yanitlarYukleniyorLayout.setVisibility(View.GONE);
                        notifyItemChanged(position);
                    }
                });
            });

        } else {
            holder.container.setVisibility(View.GONE);
            holder.yanitlariGor.setText("Yanıtları Gör");
            holder.dahafazla.setVisibility(View.GONE);
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            boolean gorunuyorMu = yorum.isYanitlarGorunuyor();
            yorum.setYanitlarGorunuyor(!gorunuyorMu);
            notifyItemChanged(position); // 🔄 Bu onBindViewHolder'ı tetikler
        });


        holder.yanitlamayiGetir.setOnClickListener(cvp -> {
            String kullaniciAdi=yorum.getKullaniciAdi();
            String metin="@"+kullaniciAdi + " ";

            SpannableString spannableString=new SpannableString(metin);
            // 1) Mavi renk kalıcı olsun diye ForegroundColorSpan uygula
            spannableString.setSpan(
                    new ForegroundColorSpan(Color.BLUE),
                    0,
                    metin.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            ClickableSpan clickableSpan=new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    if (kullaniciAdiTiklamaListener != null) {
                        kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(yorum.getYukleyenId());
                    }
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.BLUE);
                    ds.setUnderlineText(false);
                }
            };
            spannableString.setSpan(clickableSpan, 0, metin.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            MapsActivity.textt.setText(spannableString);
            MapsActivity.textt.setMovementMethod(LinkMovementMethod.getInstance());

            MapsActivity.textt.setSelection(MapsActivity.textt.getText().length());
            MapsActivity.kimeyanit.setHint(yorum.getKullaniciAdi() + " 'e yanıt veriyorsun");
            Klavye klavye=new Klavye(context);
            int eskiPozisyon = pozisyon;
            if (pozisyon == position) {
                // Aynı butona tıklandıysa: kapat
                pozisyon = -1;
                MapsActivity.yorumicin.setVisibility(View.GONE);
                MapsActivity.carpiicin.setVisibility(View.VISIBLE);
                MapsActivity.ynticin.setVisibility(View.VISIBLE);
            } else {
                // Yeni yorum seçildi: göster
                pozisyon = position;
                yorumindeks = position;
                MapsActivity.yorumicin.setVisibility(View.GONE);
                MapsActivity.carpiicin.setVisibility(View.VISIBLE);
                MapsActivity.ynticin.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    klavye.klavyeAc(MapsActivity.textt);
                }, 250);
            }

            // notifyItemChanged pozisyonları güncelle
            if (eskiPozisyon != -1) {
                notifyItemChanged(eskiPozisyon);
            }
            notifyItemChanged(position);
            yorumList.get(position).setYanitlarGorunuyor(true);

        });


        holder.menuButonu.setOnClickListener(menu->{
            Yorum_Silme_Guncelleme islem=new Yorum_Silme_Guncelleme();
            PopupMenu popupmenu=new PopupMenu(context,holder.menuButonu);
            popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu,popupmenu.getMenu());

            popupmenu.setOnMenuItemClickListener(item->{
                int id = item.getItemId();
                if (id == R.id.menu_guncelle) {
                    islem.yorumGuncelleme(yorum,context,yorumList,this);
                    return true;
                } else if (id == R.id.menu_sil) {
                    islem.yorumSil(yorum.getYorumID(),yorumList,this);
                    return true;
                }
                return false;
            });
            popupmenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return yorumList.size();
    }
    public static class YorumViewHolder extends RecyclerView.ViewHolder{
        TextView kullaniciAditext;
        TextView yorumText;
        TextView yorumTarihiText;
        TextView yanitlariGor;
        TextView yanitlamayiGetir;
        RecyclerView recyclerViewyanitlar;
        TextView dahafazla;
        LinearLayout container;
        TextView yanityoksa;
        LinearLayout yanitlarYukleniyorLayout;
        ImageView menuButonu;
        LinearLayout getYanitlarYukleniyorLayout2;
        ImageView kalpImageView;
        TextView begeniSayisiTextView;
        ImageView YorumFotoImageView;

        public YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextView);
            yorumText=itemView.findViewById(R.id.yorumTextView);
            yorumTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlariGor=itemView.findViewById(R.id.yanitlariGorTextView);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            recyclerViewyanitlar=itemView.findViewById(R.id.yanitlarRecyclerView);
            dahafazla=itemView.findViewById(R.id.dahaFazlaYanitText);
            container=itemView.findViewById(R.id.yanitlarContainer);
            yanityoksa=itemView.findViewById(R.id.yanityok);
            yanitlarYukleniyorLayout = itemView.findViewById(R.id.yanitlarYukleniyorLayout);
            menuButonu=itemView.findViewById(R.id.menuButton);
            getYanitlarYukleniyorLayout2=itemView.findViewById(R.id.yanitlarYukleniyorLayout2);
            kalpImageView=itemView.findViewById(R.id.kalpImageView);
            begeniSayisiTextView=itemView.findViewById(R.id.begeniSayisiTextView);
            YorumFotoImageView=itemView.findViewById(R.id.YorumFotoImageView);

        }
    }


}