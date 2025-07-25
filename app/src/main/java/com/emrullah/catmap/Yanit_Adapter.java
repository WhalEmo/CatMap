package com.emrullah.catmap;

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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Yanit_Adapter extends RecyclerView.Adapter<Yanit_Adapter.YanitViewHolder> {

    private ArrayList<Yanit_Model>yanitListe;
    public ArrayList<Yanit_Model> getYanitList() {
        return yanitListe;
    }
    private Context context;
    private int aitOlduguYorumIndeks;
    private String yorumID;
    private Set<String>begenilenYanitIdSeti=new HashSet<>();
    private Map<String, Integer> begeniSayisiYanitMap = new HashMap<>();
    public KullaniciAdiTiklamaListener kullaniciAdiTiklamaListener;

    public Yanit_Adapter(ArrayList<Yanit_Model> yanitListe, Context context, int yorumIndeks,String yorumID) {
        this.yanitListe = yanitListe;
        this.context = context;
        this.aitOlduguYorumIndeks = yorumIndeks;
        this.yorumID=yorumID;
    }
    public void setKullaniciAdiTiklamaListener(KullaniciAdiTiklamaListener listener) {
        this.kullaniciAdiTiklamaListener = listener;
    }

    public void setBegenilenYanitIdSeti(Set<String> begenilenYanitIdSeti) {
        this.begenilenYanitIdSeti = begenilenYanitIdSeti;
    }

    public void setBegeniSayisiYanitMap(Map<String, Integer> begeniSayisiYanitMap) {
        this.begeniSayisiYanitMap = begeniSayisiYanitMap;
    }

    private final Handler zamanHandler = new Handler();
    private final Runnable zamanRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < yanitListe.size(); i++) {
                Yanit_Model yanit = yanitListe.get(i);
                if (yanit.getTarih() == null) continue;
                long fark = System.currentTimeMillis() - yanit.getTarih().getTime();

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

    public void hazirliklariYapBegenme(Context context, String kullaniciId, Yorum_Model yorum) {
        Set<String> cachedSet = CacheHelperYanit.loadBegenilenSet(context);
        this.setBegenilenYanitIdSeti(cachedSet);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            notifyDataSetChanged();
        }, 100); // 100ms gecikme: UI'ın hazır olmasına zaman verir
        Begeni_Kod_Yoneticisi_Yanit bgynt = new Begeni_Kod_Yoneticisi_Yanit();
        bgynt.KullanicininBegendigiYanitlar(context, kullaniciId, this, yorum);
    }



    public void baslatZamanlayici() {
        zamanHandler.post(zamanRunnable);
    }

    public void durdurZamanlayici() {
        zamanHandler.removeCallbacks(zamanRunnable);
    }

    @NonNull
    @Override
    public Yanit_Adapter.YanitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yanit_icin,parent,false);
        return new Yanit_Adapter.YanitViewHolder(view);
    }
    int pozisyon=-1;


    @Override
    public void onBindViewHolder(@NonNull Yanit_Adapter.YanitViewHolder holder, int position) {
        Yanit_Model yanit=yanitListe.get(position);
        holder.kullaniciAditext.setText(yanit.getAdi());
        holder.yanitText.setText(yanit.getYaniticerik());
        holder.yanitTarihiText.setText(yanit.duzenlenmisTarih());

        URLye_Ulasma ulasma=new URLye_Ulasma();
        ulasma.IDdenUrlyeUlasma(yanit.getYanitiYukleyen(),holder.YorumFotoImageViewYnt);

        holder.kullaniciAditext.setOnClickListener(v -> {
            if (kullaniciAdiTiklamaListener != null) {
                kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(yanit.getYanitiYukleyen());
            }
        });

        int begeniSayisi = begeniSayisiYanitMap.getOrDefault(yanit.getYanitId(), 0);
        holder.begeniSayisiTextViewYnt.setText(String.valueOf(begeniSayisi));

        if (begenilenYanitIdSeti.contains(yanit.getYanitId())) {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_24);
            holder.kalpImageViewYnt.setTag("begenildi");
        } else {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_border_24);
            holder.kalpImageViewYnt.setTag("begeniYok");
        }
        Begeni_Kod_Yoneticisi_Yanit begeniKodYoneticisi=new Begeni_Kod_Yoneticisi_Yanit();
        holder.kalpImageViewYnt.setOnClickListener(v->{
            if ("begeniYok".equals(holder.kalpImageViewYnt.getTag())) {
                begeniKodYoneticisi.YanitBegenme(yorumID,yanit,MainActivity.kullanici.getID(),context,this);
                holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_24);
                kalpAnimasyonuYap(holder.kalpImageViewYnt);
                holder.kalpImageViewYnt.setTag("begenildi");
                begenilenYanitIdSeti.add(yanit.getYanitId());

            }else{
                begeniKodYoneticisi.YanitBegeniKaldirma(yorumID,yanit,MainActivity.kullanici.getID(),context,this);
                holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_border_24);
                holder.kalpImageViewYnt.setTag("begeniYok");
                begenilenYanitIdSeti.remove(yanit.getYanitId());
            }
        });


        if (MainActivity.kullanici.getKullaniciAdi().equals(yanit.getAdi())) {
            if(yanit.yanitMiGeldi==true){
                holder.menuButonu.setVisibility(View.GONE);
                holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.GONE);
                        holder.menuButonu.setVisibility(View.VISIBLE);
                        yanit.yanitMiGeldi = false;
                    }
                }, 3000);
            }else {
                holder.menuButonu.setVisibility(View.VISIBLE);
                holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.GONE);
            }
        } else {
            holder.menuButonu.setVisibility(View.GONE);
        }


        holder.yanitlabutonu.setOnClickListener(cvp->{
            String kullaniciAdi=yanit.getAdi();
            String metin="@"+kullaniciAdi+ " ";

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
                        kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(yanit.getYanitiYukleyen());

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
            MapsActivity.kimeyanit.setHint(yanit.getAdi() + " 'e yanıt veriyorsun");
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
                Yorum_Adapter.yorumindeks = aitOlduguYorumIndeks;
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
            if (position >= 0 && position < getItemCount()) {
                notifyItemChanged(position);
            }
        });

        holder.menuButonu.setOnClickListener(menu->{
            Yorum_Silme_Guncelleme islem=new Yorum_Silme_Guncelleme();
            PopupMenu popupmenu=new PopupMenu(context,holder.menuButonu);
            popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu,popupmenu.getMenu());

            popupmenu.setOnMenuItemClickListener(item->{
                int id = item.getItemId();
                if (id == R.id.menu_guncelle) {
                    islem.yorumGuncellemeynt(yanit,yorumID,context,yanitListe,this);
                    return true;
                } else if (id == R.id.menu_sil) {
                    islem.yorumSilynt(yanit.getYanitId(),yorumID,yanitListe,this);
                    return true;
                }
                return false;
            });
            popupmenu.show();
        });
    }

    @Override
    public int getItemCount() {

        return yanitListe.size();
    }
    public static class YanitViewHolder extends RecyclerView.ViewHolder{
        TextView kullaniciAditext;
        TextView yanitText;
        TextView yanitTarihiText;
        TextView yanitlamayiGetir;
        TextView yanitlabutonu;
        ImageView menuButonu;
        LinearLayout getYanitlarYukleniyorLayout2ynt;
        TextView begeniSayisiTextViewYnt;
        ImageView kalpImageViewYnt;
        ImageView YorumFotoImageViewYnt;
        public YanitViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextViewynt);
            yanitText=itemView.findViewById(R.id.yanittTextView);
            yanitTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            yanitlabutonu=itemView.findViewById(R.id.yanitlayazisiynt);
            menuButonu=itemView.findViewById(R.id.menuButtonynt);
            getYanitlarYukleniyorLayout2ynt=itemView.findViewById(R.id.yanitlarYukleniyorLayout2ynt);
            begeniSayisiTextViewYnt=itemView.findViewById(R.id.begeniSayisiTextViewYnt);
            kalpImageViewYnt=itemView.findViewById(R.id.kalpImageViewYnt);
            YorumFotoImageViewYnt=itemView.findViewById(R.id.YorumFotoImageViewYnt);
        }
    }
}

