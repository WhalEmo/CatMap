package com.beem.catmap.YorumYanit;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.opengl.Visibility;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Yorum_Adapter extends ListAdapter<Yorum_Adapter.CommentItem, RecyclerView.ViewHolder> {
    public static final int TYPE_YORUM = 0;
    public static final int TYPE_YANIT = 1;
    public static final int TYPE_DAHA_FAZLA = 2;

    public static class CommentItem {
        private final int type;
        private Yorum_Model yorum;
        private Yanit_Model yanit;
        private String parentYorumId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommentItem item = (CommentItem) o;
            return type == item.type &&
                    Objects.equals(yorum, item.yorum) &&
                    Objects.equals(yanit, item.yanit) &&
                    Objects.equals(parentYorumId, item.parentYorumId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, yorum, yanit, parentYorumId);
        }

        public CommentItem(Yorum_Model yorum) {
            this.type = TYPE_YORUM;
            this.yorum = yorum;
        }

        public CommentItem(Yanit_Model yanit, String parentYorumId) {
            this.type = TYPE_YANIT;
            this.yanit = yanit;
            this.parentYorumId = parentYorumId;
        }

        public CommentItem(int type, Yorum_Model yorum) {
            this.type = type;
            this.yorum = yorum;
        }

        public int getType() { return type; }
        public Yorum_Model getYorum() { return yorum; }
        public Yanit_Model getYanit() { return yanit; }
        public String getParentYorumId() { return parentYorumId; }
    }

    public interface OnYorumInteractionListener {
        void onKalpTiklandi(Yorum_Model yorum);
        void onYanitlariGorTiklandi(Yorum_Model yorum);
        void onYanitlaTiklandi(Yorum_Model yorum);
        void onKullaniciAdiTiklandi(String userId);
        void onSilTiklandi(Yorum_Model yorum);
        void onGuncelleTiklandi(Yorum_Model yorum);

        void onYanitKalpTiklandi(Yanit_Model yanit,String yorumId);
        void onYanitYanitlaTiklandi(@NonNull Yanit_Model yanit, @NonNull String yorumId);
        void onYanitSilTiklandi(@NonNull Yanit_Model yanit, @NonNull String yorumId);
        void onYanitGuncelleTiklandi(@NonNull Yanit_Model yanit, @NonNull String yorumId);
        void onDahaFazlaYanitGetirTiklandi(Yorum_Model yorum);
    }

    private final Context context;
    private final String currentUserId;
    private OnYorumInteractionListener listener;

    private static final DiffUtil.ItemCallback<CommentItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<CommentItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
            if (oldItem.getType() != newItem.getType()) return false;

            switch (oldItem.getType()) {
                case TYPE_YORUM:
                case TYPE_DAHA_FAZLA:
                    return oldItem.getYorum() != null && newItem.getYorum() != null &&
                            Objects.equals(oldItem.getYorum().getYorumID(), newItem.getYorum().getYorumID());
                case TYPE_YANIT:
                    return oldItem.getYanit() != null && newItem.getYanit() != null &&
                            Objects.equals(oldItem.getYanit().getYanitId(), newItem.getYanit().getYanitId());
                default:
                    return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
            // Doğrudan CommentItem.equals() metoduna yönlendiriyoruz.
            // Sınıflara eklediğimiz equals() sayesinde derinlemesine kontrol yapılır.
            return Objects.equals(oldItem, newItem);
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

    public void submitYorumList(List<Yorum_Model> yorumlar) {
        List<CommentItem> flattenedList = new ArrayList<>();
        if (yorumlar != null) {
            for (Yorum_Model yorum : yorumlar) {
                flattenedList.add(new CommentItem(yorum));
                if (yorum.isYanitlarGorunuyor() && yorum.getYanitlar() != null) {
                    for (Yanit_Model yanit : yorum.getYanitlar()) {
                        flattenedList.add(new CommentItem(yanit, yorum.getYorumID()));
                    }
                    if (yorum.isDahafazlaGozukuyorMu() && !yorum.getYanitlar().isEmpty()) {
                        flattenedList.add(new CommentItem(TYPE_DAHA_FAZLA, yorum));
                    }
                }
            }
        }
        super.submitList(flattenedList);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_YORUM) {
            View view = inflater.inflate(R.layout.herbi_yorum_icin, parent, false);
            return new YorumViewHolder(view);
        } else if (viewType == TYPE_YANIT) {
            View view = inflater.inflate(R.layout.herbi_yanit_icin, parent, false);
            return new YanitViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_daha_fazla_yanit, parent, false);
            return new DahaFazlaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CommentItem item = getItem(position);

        if (holder.getItemViewType() == TYPE_YORUM) {
            bindYorum((YorumViewHolder) holder, item.getYorum());
        } else if (holder.getItemViewType() == TYPE_YANIT) {
            bindYanit((YanitViewHolder) holder, item.getYanit(), item.getParentYorumId());
        } else if (holder.getItemViewType() == TYPE_DAHA_FAZLA) {
            bindDahaFazla((DahaFazlaViewHolder) holder, item.getYorum());
        }
    }

    private void bindYorum(YorumViewHolder holder, Yorum_Model yorum) {
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

        if (yorum.getSending()) {
            holder.yanitlarYukleniyorLayout2.setVisibility(VISIBLE);
            holder.likeLayout.setVisibility(GONE);
            holder.yanitlarLayout.setVisibility(GONE);
        } else {
            holder.yanitlarYukleniyorLayout2.setVisibility(GONE);
            holder.likeLayout.setVisibility(VISIBLE);
            holder.yanitlarLayout.setVisibility(VISIBLE);
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

        if (currentUserId != null && currentUserId.equals(yorum.getYukleyenId())) {
            holder.menuButonu.setVisibility(VISIBLE);
            holder.menuButonu.setOnClickListener(menu -> {
                PopupMenu popupmenu = new PopupMenu(context, holder.menuButonu);
                popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(menuItem -> {
                    int id = menuItem.getItemId();
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
            holder.menuButonu.setVisibility(GONE);
        }

        if (yorum.isYanitlarGorunuyor()) {
            holder.yanitlariGor.setText("Yanıtları Gizle");
        } else {
            holder.yanitlariGor.setText("Yanıtları Gör");
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            if (listener != null) listener.onYanitlariGorTiklandi(yorum);
        });

        holder.yanitlamayiGetir.setOnClickListener(v -> {
            if (listener != null) listener.onYanitlaTiklandi(yorum);
        });
    }

    private void bindYanit(YanitViewHolder holder, Yanit_Model yanit, String parentYorumId) {
        holder.kullaniciAditextYnt.setText(yanit.getAdi());
        holder.yanitText.setText(yanit.getYaniticerik());
        holder.yanitTarihiText.setText(yanit.duzenlenmisTarih());

        new URLye_Ulasma().IDdenUrlyeUlasma(yanit.getYanitiYukleyen(), holder.YorumFotoImageViewYnt);


        int begeniSayisi = yanit.getBegeniSayisiYanit();
        if (begeniSayisi >= 1_000_000) {
            holder.begeniSayisiTextViewYnt.setText(String.format("%.1f m", begeniSayisi / 1_000_000.0));
        } else if (begeniSayisi >= 1_000) {
            holder.begeniSayisiTextViewYnt.setText(String.format("%.1f bin", begeniSayisi / 1_000.0).replace('.', ','));
        } else {
            holder.begeniSayisiTextViewYnt.setText(String.valueOf(begeniSayisi));
        }

        if (yanit.isSending()) {
            holder.yanitlarYukleniyorLayout2ynt.setVisibility(VISIBLE);
            holder.yanitlaLayout.setVisibility(VISIBLE);
            holder.likeLayoutYnt.setVisibility(GONE);
        } else {
            holder.yanitlarYukleniyorLayout2ynt.setVisibility(GONE);
            holder.yanitlaLayout.setVisibility(GONE);
            holder.likeLayoutYnt.setVisibility(VISIBLE);
        }

        if (yanit.isBegenildiMi()) {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_24);
        } else {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_border_24);
        }

        holder.kalpImageViewYnt.setOnClickListener(v -> {
            kalpAnimasyonuYap(holder.kalpImageViewYnt);
            if (listener != null) listener.onYanitKalpTiklandi(yanit,parentYorumId);
        });


        holder.kullaniciAditextYnt.setOnClickListener(v -> {
            if (listener != null) listener.onKullaniciAdiTiklandi(yanit.getYanitiYukleyen());
        });

        holder.yanitlayazisiynt.setOnClickListener(v -> {
            if (listener != null) {
                listener.onYanitYanitlaTiklandi(yanit, parentYorumId);
            }
        });

        if (currentUserId != null && currentUserId.equals(yanit.getYanitiYukleyen())) {
            holder.menuButonuYnt.setVisibility(VISIBLE);
            holder.menuButonuYnt.setOnClickListener(menu -> {
                PopupMenu popupmenu = new PopupMenu(context, holder.menuButonuYnt);
                popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_guncelle) {
                        if (listener != null) listener.onYanitGuncelleTiklandi(yanit, parentYorumId);
                        return true;
                    } else if (id == R.id.menu_sil) {
                        if (listener != null) listener.onYanitSilTiklandi(yanit, parentYorumId);
                        return true;
                    }
                    return false;
                });
                popupmenu.show();
            });
        } else {
            holder.menuButonuYnt.setVisibility(GONE);
        }
    }

    private void bindDahaFazla(DahaFazlaViewHolder holder, Yorum_Model yorum) {
        holder.dahaFazlaText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDahaFazlaYanitGetirTiklandi(yorum);
            }
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
        TextView kullaniciAditext, yorumText, yorumTarihiText, yanitlariGor, yanitlamayiGetir, begeniSayisiTextView;
        ImageView menuButonu, kalpImageView, YorumFotoImageView;
        LinearLayout yanitlarYukleniyorLayout2,likeLayout,yanitlarLayout;

        public YorumViewHolder(View itemView) {
            super(itemView);
            kullaniciAditext = itemView.findViewById(R.id.kullaniciAdiTextView);
            yanitlarYukleniyorLayout2 = itemView.findViewById(R.id.yanitlarYukleniyorLayout2);
            likeLayout= itemView.findViewById(R.id.likeLayout);
            yorumText = itemView.findViewById(R.id.yorumTextView);
            yorumTarihiText = itemView.findViewById(R.id.tarihTextView);
            yanitlariGor = itemView.findViewById(R.id.yanitlariGorTextView);
            yanitlamayiGetir = itemView.findViewById(R.id.yanitGosterTextView);
            menuButonu = itemView.findViewById(R.id.menuButton);
            kalpImageView = itemView.findViewById(R.id.kalpImageView);
            begeniSayisiTextView = itemView.findViewById(R.id.begeniSayisiTextView);
            YorumFotoImageView = itemView.findViewById(R.id.YorumFotoImageView);
            yanitlarLayout = itemView.findViewById(R.id.yanitlarLayout);
        }
    }

    public static class YanitViewHolder extends RecyclerView.ViewHolder {
        TextView kullaniciAditextYnt, yanitText, yanitTarihiText, yanitlayazisiynt, begeniSayisiTextViewYnt;
        ImageView menuButonuYnt, YorumFotoImageViewYnt,kalpImageViewYnt;
        LinearLayout likeLayoutYnt, yanitlarYukleniyorLayout2ynt,yanitlaLayout;

        public YanitViewHolder(View itemView) {
            super(itemView);
            kullaniciAditextYnt = itemView.findViewById(R.id.kullaniciAdiTextViewynt);
            yanitText = itemView.findViewById(R.id.yanittTextView);
            yanitlayazisiynt = itemView.findViewById(R.id.yanitlayazisiynt);
            yanitTarihiText = itemView.findViewById(R.id.tarihTextView);
            menuButonuYnt = itemView.findViewById(R.id.menuButtonynt);
            YorumFotoImageViewYnt = itemView.findViewById(R.id.YorumFotoImageViewYnt);
            kalpImageViewYnt = itemView.findViewById(R.id.kalpImageViewYnt);
            begeniSayisiTextViewYnt = itemView.findViewById(R.id.begeniSayisiTextViewYnt);
            likeLayoutYnt = itemView.findViewById(R.id.likeLayoutYnt);
            yanitlarYukleniyorLayout2ynt = itemView.findViewById(R.id.yanitlarYukleniyorLayout2ynt);
            yanitlaLayout = itemView.findViewById(R.id.yanitlaLayout);
        }
    }

    public static class DahaFazlaViewHolder extends RecyclerView.ViewHolder {
        TextView dahaFazlaText;

        public DahaFazlaViewHolder(View itemView) {
            super(itemView);
            dahaFazlaText = itemView.findViewById(R.id.dahaFazlaYanitText);
        }
    }
}