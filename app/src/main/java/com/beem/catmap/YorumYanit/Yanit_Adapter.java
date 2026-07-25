package com.beem.catmap.YorumYanit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.KullaniciAuth.Kullanici;
import com.beem.catmap.Maps.MapKedi.KullaniciAdiTiklamaListener;
import com.beem.catmap.R;
import com.beem.catmap.URLye_Ulasma;
import com.beem.catmap.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Yanit_Adapter extends ListAdapter<Yanit_Model, Yanit_Adapter.YanitViewHolder> {

    private OnYanitAksiyonListener aksiyonListener;
    private Context context;
    private int aitOlduguYorumIndeks;
    private String yorumID;
    private String catId;
    private Set<String> begenilenYanitIdSeti = new HashSet<>();
    private Map<String, Integer> begeniSayisiYanitMap = new HashMap<>();
    public KullaniciAdiTiklamaListener kullaniciAdiTiklamaListener;
    private UserRepository userRepository;

    // ListAdapter DiffUtil Tanımlaması
    private static final DiffUtil.ItemCallback<Yanit_Model> DIFF_CALLBACK = new DiffUtil.ItemCallback<Yanit_Model>() {
        @Override
        public boolean areItemsTheSame(@NonNull Yanit_Model oldItem, @NonNull Yanit_Model newItem) {
            return Objects.equals(oldItem.getYanitId(), newItem.getYanitId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Yanit_Model oldItem, @NonNull Yanit_Model newItem) {
            return Objects.equals(oldItem.getYaniticerik(), newItem.getYaniticerik())
                    && Objects.equals(oldItem.getAdi(), newItem.getAdi())
                    && oldItem.yanitMiGeldi == newItem.yanitMiGeldi;
        }
    };

    public ArrayList<Yanit_Model> getYanitList() {
        return new ArrayList<>(getCurrentList());
    }

    public Yanit_Adapter(Context context, int yorumIndeks, String yorumID, String catId) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.aitOlduguYorumIndeks = yorumIndeks;
        this.yorumID = yorumID;
        this.catId = catId;
    }

    public void setOnYanitAksiyonListener(OnYanitAksiyonListener listener) {
        this.aksiyonListener = listener;
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
            List<Yanit_Model> mevcuttur = getCurrentList();
            for (int i = 0; i < mevcuttur.size(); i++) {
                Yanit_Model yanit = mevcuttur.get(i);
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
                0.7f, 1.2f,
                0.7f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        büyütKücült.setDuration(200);
        büyütKücült.setRepeatCount(1);
        büyütKücült.setRepeatMode(Animation.REVERSE);

        kalpView.startAnimation(büyütKücült);
    }

    public void hazirliklariYapBegenme(Context context, String kullaniciId, Yorum_Model yorum) {
        Set<String> cachedSet = CacheHelperYanit.loadBegenilenSet(context);
        this.setBegenilenYanitIdSeti(cachedSet);

        Begeni_Kod_Yoneticisi_Yanit bgynt = new Begeni_Kod_Yoneticisi_Yanit();
        bgynt.KullanicininBegendigiYanitlar(catId, context, kullaniciId, this, yorum);
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
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yanit_icin, parent, false);
        return new Yanit_Adapter.YanitViewHolder(view);
    }

    private int pozisyon = -1;

    @Override
    public void onBindViewHolder(@NonNull Yanit_Adapter.YanitViewHolder holder, int position) {
        Yanit_Model yanit = getItem(position);

        if (userRepository == null) {
            userRepository = UserRepository.Companion.getInstance(context);
        }

        Kullanici currentUser = userRepository.getCurrentUser();

        holder.kullaniciAditext.setText(yanit.getAdi());
        holder.yanitText.setText(yanit.getYaniticerik());
        holder.yanitTarihiText.setText(yanit.duzenlenmisTarih());

        URLye_Ulasma ulasma = new URLye_Ulasma();
        ulasma.IDdenUrlyeUlasma(yanit.getYanitiYukleyen(), holder.YorumFotoImageViewYnt);

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

        Begeni_Kod_Yoneticisi_Yanit begeniKodYoneticisi = new Begeni_Kod_Yoneticisi_Yanit();

        holder.kalpImageViewYnt.setOnClickListener(v -> {
            if ("begeniYok".equals(holder.kalpImageViewYnt.getTag())) {
                begeniKodYoneticisi.YanitBegenme(catId, yorumID, yanit, currentUser.getID(), context, this);
                holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_24);
                kalpAnimasyonuYap(holder.kalpImageViewYnt);
                holder.kalpImageViewYnt.setTag("begenildi");
                begenilenYanitIdSeti.add(yanit.getYanitId());
            } else {
                begeniKodYoneticisi.YanitBegeniKaldirma(catId, yorumID, yanit, currentUser.getID(), context, this);
                holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_border_24);
                holder.kalpImageViewYnt.setTag("begeniYok");
                begenilenYanitIdSeti.remove(yanit.getYanitId());
            }
        });

        if (currentUser.getKullaniciAdi().equals(yanit.getAdi())) {
            if (yanit.yanitMiGeldi) {
                holder.menuButonu.setVisibility(View.GONE);
                holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.GONE);
                    holder.menuButonu.setVisibility(View.VISIBLE);
                    yanit.yanitMiGeldi = false;
                }, 3000);
            } else {
                holder.menuButonu.setVisibility(View.VISIBLE);
                holder.getYanitlarYukleniyorLayout2ynt.setVisibility(View.GONE);
            }
        } else {
            holder.menuButonu.setVisibility(View.GONE);
        }

        holder.yanitlabutonu.setOnClickListener(cvp -> {
            int eskiPozisyon = pozisyon;
            boolean ayniButonaMiBasildi = (eskiPozisyon == position);

            if (ayniButonaMiBasildi) {
                pozisyon = -1;
            } else {
                pozisyon = position;
            }

            if (eskiPozisyon != -1) {
                notifyItemChanged(eskiPozisyon);
            }
            if (position >= 0 && position < getItemCount()) {
                notifyItemChanged(position);
            }

            if (aksiyonListener != null) {
                aksiyonListener.onAltYanitlaTiklandi(
                        yanit.getAdi(),
                        yanit.getYanitiYukleyen(),
                        aitOlduguYorumIndeks,
                        ayniButonaMiBasildi
                );
            }
        });

        holder.menuButonu.setOnClickListener(menu -> {
            Yorum_Silme_Guncelleme islem = new Yorum_Silme_Guncelleme();
            PopupMenu popupmenu = new PopupMenu(context, holder.menuButonu);
            popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());

            popupmenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_guncelle) {
                    islem.yorumGuncellemeynt(catId, yanit, yorumID, context, getYanitList(), this);
                    return true;
                } else if (id == R.id.menu_sil) {
                    islem.yorumSilynt(catId, yanit.getYanitId(), yorumID, getYanitList(), this);
                    return true;
                }
                return false;
            });
            popupmenu.show();
        });
    }

    public static class YanitViewHolder extends RecyclerView.ViewHolder {
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
            kullaniciAditext = itemView.findViewById(R.id.kullaniciAdiTextViewynt);
            yanitText = itemView.findViewById(R.id.yanittTextView);
            yanitTarihiText = itemView.findViewById(R.id.tarihTextView);
            yanitlamayiGetir = itemView.findViewById(R.id.yanitGosterTextView);
            yanitlabutonu = itemView.findViewById(R.id.yanitlayazisiynt);
            menuButonu = itemView.findViewById(R.id.menuButtonynt);
            getYanitlarYukleniyorLayout2ynt = itemView.findViewById(R.id.yanitlarYukleniyorLayout2ynt);
            begeniSayisiTextViewYnt = itemView.findViewById(R.id.begeniSayisiTextViewYnt);
            kalpImageViewYnt = itemView.findViewById(R.id.kalpImageViewYnt);
            YorumFotoImageViewYnt = itemView.findViewById(R.id.YorumFotoImageViewYnt);
        }
    }
}