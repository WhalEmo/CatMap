package com.beem.catmap.YorumYanit;

import android.content.Context;
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

import com.beem.catmap.R;
import com.beem.catmap.URLye_Ulasma;

import java.util.Objects;

public class Yorum_Adapter extends ListAdapter<Yorum_Model, Yorum_Adapter.YorumViewHolder> {

    public interface OnYorumInteractionListener {
        void onKalpTiklandi(Yorum_Model yorum);
        void onYanitlariGorTiklandi(Yorum_Model yorum);
        void onYanitlaTiklandi(Yorum_Model yorum);
        void onKullaniciAdiTiklandi(String userId);
        void onSilTiklandi(Yorum_Model yorum);
        void onGuncelleTiklandi(Yorum_Model yorum);
    }

    private final Context context;
    private final String currentUserId;
    private OnYorumInteractionListener listener;

    private static final DiffUtil.ItemCallback<Yorum_Model> DIFF_CALLBACK = new DiffUtil.ItemCallback<Yorum_Model>() {
        @Override
        public boolean areItemsTheSame(@NonNull Yorum_Model oldItem, @NonNull Yorum_Model newItem) {
            return Objects.equals(oldItem.getYorumID(), newItem.getYorumID());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Yorum_Model oldItem, @NonNull Yorum_Model newItem) {
            return oldItem.equals(newItem);
        }
    };

    public Yorum_Adapter(Context context, String currentUserId) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setOnYorumInteractionListener(OnYorumInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yorum_icin, parent, false);
        return new YorumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {
        Yorum_Model yorum = getItem(position);

        holder.kullaniciAditext.setText(yorum.getKullaniciAdi());
        holder.yorumText.setText(yorum.getYorumicerik());
        holder.yorumTarihiText.setText(yorum.duzenlenmisTarih());


        new URLye_Ulasma().IDdenUrlyeUlasma(yorum.getYukleyenId(), holder.YorumFotoImageView);

        holder.kullaniciAditext.setOnClickListener(v -> {
            if (listener != null) listener.onKullaniciAdiTiklandi(yorum.getYukleyenId());
        });

        int begeniSayisi = yorum.getBegeniSayisi();
        if (begeniSayisi >= 1_000_000) {
            holder.begeniSayisiTextView.setText(String.format("%.1f m", begeniSayisi / 1_000_000.0));
        } else if (begeniSayisi >= 1_000) {
            holder.begeniSayisiTextView.setText(String.format("%.1f bin", begeniSayisi / 1_000.0).replace('.', ','));
        } else {
            holder.begeniSayisiTextView.setText(String.valueOf(begeniSayisi));
        }

        if (yorum.isBegenildiMi()) {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_24);
        } else {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24);
        }


        holder.kalpImageView.setOnClickListener(v -> {
            kalpAnimasyonuYap(holder.kalpImageView);
            if (listener != null) listener.onKalpTiklandi(yorum);
        });

        // Kendi Yorumu ise Menü Göster
        if (currentUserId != null && currentUserId.equals(yorum.getYukleyenId())) {
            holder.menuButonu.setVisibility(View.VISIBLE);
            holder.menuButonu.setOnClickListener(menu -> {
                PopupMenu popupmenu = new PopupMenu(context, holder.menuButonu);
                popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_guncelle) {
                        if (listener != null) listener.onGuncelleTiklandi(yorum);
                        return true;
                    } else if (id == R.id.menu_sil) {
                        if (listener != null) listener.onSilTiklandi(yorum);
                        return true;
                    }
                    return false;
                });
                popupmenu.show();
            });
        } else {
            holder.menuButonu.setVisibility(View.GONE);
        }


        if (yorum.isYanitlarGorunuyor()) {
            holder.container.setVisibility(View.VISIBLE);
            holder.yanitlariGor.setText("Yanıtları Gizle");
        } else {
            holder.container.setVisibility(View.GONE);
            holder.yanitlariGor.setText("Yanıtları Gör");
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            if (listener != null) listener.onYanitlariGorTiklandi(yorum);
        });

        holder.yanitlamayiGetir.setOnClickListener(v -> {
            if (listener != null) listener.onYanitlaTiklandi(yorum);
        });
    }

    private void kalpAnimasyonuYap(ImageView kalpView) {
        ScaleAnimation buyutKucult = new ScaleAnimation(
                0.7f, 1.2f,
                0.7f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        buyutKucult.setDuration(200);
        buyutKucult.setRepeatCount(1);
        buyutKucult.setRepeatMode(Animation.REVERSE);
        kalpView.startAnimation(buyutKucult);
    }

    public static class YorumViewHolder extends RecyclerView.ViewHolder {
        TextView kullaniciAditext, yorumText, yorumTarihiText, yanitlariGor, yanitlamayiGetir, dahafazla, yanityoksa, begeniSayisiTextView;
        RecyclerView recyclerViewyanitlar;
        LinearLayout container, yanitlarYukleniyorLayout, getYanitlarYukleniyorLayout2;
        ImageView menuButonu, kalpImageView, YorumFotoImageView;

        public YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext = itemView.findViewById(R.id.kullaniciAdiTextView);
            yorumText = itemView.findViewById(R.id.yorumTextView);
            yorumTarihiText = itemView.findViewById(R.id.tarihTextView);
            yanitlariGor = itemView.findViewById(R.id.yanitlariGorTextView);
            yanitlamayiGetir = itemView.findViewById(R.id.yanitGosterTextView);
            recyclerViewyanitlar = itemView.findViewById(R.id.yanitlarRecyclerView);
            dahafazla = itemView.findViewById(R.id.dahaFazlaYanitText);
            container = itemView.findViewById(R.id.yanitlarContainer);
            yanityoksa = itemView.findViewById(R.id.yanityok);
            yanitlarYukleniyorLayout = itemView.findViewById(R.id.yanitlarYukleniyorLayout);
            menuButonu = itemView.findViewById(R.id.menuButton);
            getYanitlarYukleniyorLayout2 = itemView.findViewById(R.id.yanitlarYukleniyorLayout2);
            kalpImageView = itemView.findViewById(R.id.kalpImageView);
            begeniSayisiTextView = itemView.findViewById(R.id.begeniSayisiTextView);
            YorumFotoImageView = itemView.findViewById(R.id.YorumFotoImageView);
        }
    }
}