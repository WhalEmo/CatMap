package com.beem.catmap.ui.commentreply;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.beem.catmap.ui.extensions.DateFormatterKt.getFormattedDate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.R;
import com.beem.catmap.GetPhotoUrl;
import com.beem.catmap.data.model.CommentModel;
import com.beem.catmap.data.model.ReplyModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommentAdapter extends ListAdapter<CommentAdapter.CommentItem, RecyclerView.ViewHolder> {
    public static final int TYPE_COMMENT = 0;
    public static final int TYPE_REPLY = 1;
    public static final int TYPE_MORE = 2;

    public static class CommentItem {
        private final int type;
        private CommentModel comment;
        private ReplyModel reply;
        private String parentCommentId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommentItem item = (CommentItem) o;
            return type == item.type &&
                    Objects.equals(comment, item.comment) &&
                    Objects.equals(reply, item.reply) &&
                    Objects.equals(parentCommentId, item.parentCommentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, comment, reply, parentCommentId);
        }

        public CommentItem(CommentModel comment) {
            this.type = TYPE_COMMENT;
            this.comment = comment;
        }

        public CommentItem(ReplyModel reply, String parentCommentId) {
            this.type = TYPE_REPLY;
            this.reply = reply;
            this.parentCommentId = parentCommentId;
        }

        public CommentItem(int type, CommentModel comment) {
            this.type = type;
            this.comment = comment;
        }

        public int getType() { return type; }
        public CommentModel getComment() { return comment; }
        public ReplyModel getReply() { return reply; }
        public String getParentCommentId() { return parentCommentId; }
    }

    public interface OnYorumInteractionListener {
        void onCommentLikeClicked(CommentModel yorum,ImageView like);
        void onShowRepliesClicked(CommentModel yorum);
        void onReplyClicked(CommentModel yorum);
        void onUsernameClicked(String userId);
        void onDeleteClicked(CommentModel yorum);
        void onUpdateClicked(CommentModel yorum);

        void onReplyLikeClicked(ReplyModel yanit, String yorumId,ImageView like);
        void onShowReplyRepliesClicked(@NonNull ReplyModel yanit, @NonNull String yorumId);
        void onDeleteReply(@NonNull ReplyModel yanit, @NonNull String yorumId);
        void onReplyUpdate(@NonNull ReplyModel yanit, @NonNull String yorumId);
        void onLoadMoreRepliesClicked(CommentModel yorum);
    }

    private final Context context;
    private final String currentUserId;
    private OnYorumInteractionListener listener;

    private static final DiffUtil.ItemCallback<CommentItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<CommentItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
            if (oldItem.getType() != newItem.getType()) return false;

            switch (oldItem.getType()) {
                case TYPE_COMMENT:
                case TYPE_MORE:
                    return oldItem.getComment() != null && newItem.getComment() != null &&
                            Objects.equals(oldItem.getComment().getCommentId(), newItem.getComment().getCommentId());
                case TYPE_REPLY:
                    return oldItem.getReply() != null && newItem.getReply() != null &&
                            Objects.equals(oldItem.getReply().getReplyId(), newItem.getReply().getReplyId());
                default:
                    return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull CommentItem oldItem, @NonNull CommentItem newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
    public CommentAdapter(Context context, String currentUserId) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setOnYorumInteractionListener(OnYorumInteractionListener listener) {
        this.listener = listener;
    }

    public void submitYorumList(List<CommentModel> yorumlar) {
        List<CommentItem> flattenedList = new ArrayList<>();
        if (yorumlar != null) {
            for (CommentModel yorum : yorumlar) {
                flattenedList.add(new CommentItem(yorum));

                List<ReplyModel> yanitlar = yorum.getReplies();
                if (yanitlar != null && !yanitlar.isEmpty()) {

                    if (yorum.isAreRepliesVisible()) {
                        for (ReplyModel yanit : yanitlar) {
                            flattenedList.add(new CommentItem(yanit, yorum.getCommentId()));
                        }
                        if (yorum.isHasMoreReplies()
                                && yorum.getReplies().size() < yorum.getSumRepliesCount()) {

                            flattenedList.add(new CommentItem(TYPE_MORE, yorum));
                        }
                    } else {
                        for (ReplyModel yanit : yanitlar) {
                            if (yanit.isLocalOnly() || yanit.isSending()) {
                                flattenedList.add(new CommentItem(yanit, yorum.getCommentId()));
                            }
                        }
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
        if (viewType == TYPE_COMMENT) {
            View view = inflater.inflate(R.layout.herbi_yorum_icin, parent, false);
            return new CommentViewHolder(view);
        } else if (viewType == TYPE_REPLY) {
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

        if (holder.getItemViewType() == TYPE_COMMENT) {
            bindComment((CommentViewHolder) holder, item.getComment());
        } else if (holder.getItemViewType() == TYPE_REPLY) {
            bindYanit((YanitViewHolder) holder, item.getReply(), item.getParentCommentId());
        } else if (holder.getItemViewType() == TYPE_MORE) {
            bindDahaFazla((DahaFazlaViewHolder) holder, item.getComment());
        }
    }

    private void bindComment(CommentViewHolder holder, CommentModel yorum) {
        holder.kullaniciAditext.setText(yorum.getUsername());
        holder.yorumText.setText(yorum.getCommentContent());
        holder.yorumTarihiText.setText(getFormattedDate(yorum.getDate()));


        new GetPhotoUrl().getUrl(yorum.getLoadId(), holder.YorumFotoImageView);

        holder.kullaniciAditext.setOnClickListener(v -> {
            if (listener != null) listener.onUsernameClicked(yorum.getLoadId());
        });

        int begeniSayisi = yorum.getLikeCount();
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

        if (yorum.isLiked()) {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_24);
        } else {
            holder.kalpImageView.setImageResource(R.drawable.baseline_favorite_border_24);
        }

        holder.kalpImageView.setOnClickListener(v -> {
            if (listener != null) listener.onCommentLikeClicked(yorum,holder.kalpImageView);
        });

        if (currentUserId != null && currentUserId.equals(yorum.getLoadId())) {
            holder.menuButonu.setVisibility(VISIBLE);
            holder.menuButonu.setOnClickListener(menu -> {
                PopupMenu popupmenu = new PopupMenu(context, holder.menuButonu);
                popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(menuItem -> {
                    int id = menuItem.getItemId();
                    if (id == R.id.menu_guncelle) {
                        if (listener != null) listener.onUpdateClicked(yorum);
                        return true;
                    } else if (id == R.id.menu_sil) {
                        if (listener != null) listener.onDeleteClicked(yorum);
                        return true;
                    }
                    return false;
                });
                popupmenu.show();
            });
        } else {
            holder.menuButonu.setVisibility(GONE);
        }

        if (yorum.isAreRepliesVisible()) {

            holder.yanitlariGor.setText("Yanıtları Gizle");
        } else {
            int toplam = yorum.getSumRepliesCount();
            if (toplam > 0) {
                holder.yanitlariGor.setText(toplam + " Yanıtı Gör");
            } else {
                holder.yanitlariGor.setText("Yanıtları Gör");
            }
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            if (listener != null) listener.onShowRepliesClicked(yorum);
        });

        holder.yanitlamayiGetir.setOnClickListener(v -> {
            if (listener != null) listener.onReplyClicked(yorum);
        });
    }

    private void bindYanit(YanitViewHolder holder, ReplyModel yanit, String parentYorumId) {
        holder.kullaniciAditextYnt.setText(yanit.getName());
        holder.yanitText.setText(yanit.getReplyContent());
        holder.yanitTarihiText.setText( getFormattedDate(yanit.getDate()));

        new GetPhotoUrl().getUrl(yanit.getReplyUserId(), holder.YorumFotoImageViewYnt);


        int begeniSayisi = yanit.getLikeCountReply();
        if (begeniSayisi >= 1_000_000) {
            holder.begeniSayisiTextViewYnt.setText(String.format("%.1f m", begeniSayisi / 1_000_000.0));
        } else if (begeniSayisi >= 1_000) {
            holder.begeniSayisiTextViewYnt.setText(String.format("%.1f bin", begeniSayisi / 1_000.0).replace('.', ','));
        } else {
            holder.begeniSayisiTextViewYnt.setText(String.valueOf(begeniSayisi));
        }

        if (yanit.isSending()) {
            holder.yanitlarYukleniyorLayout2ynt.setVisibility(VISIBLE);
            holder.yanitlaLayout.setVisibility(GONE);
            holder.likeLayoutYnt.setVisibility(GONE);
        } else {
            holder.yanitlarYukleniyorLayout2ynt.setVisibility(GONE);
            holder.yanitlaLayout.setVisibility(VISIBLE);
            holder.likeLayoutYnt.setVisibility(VISIBLE);
        }

        if (yanit.isLiked()) {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_24);
        } else {
            holder.kalpImageViewYnt.setImageResource(R.drawable.baseline_favorite_border_24);
        }

        holder.kalpImageViewYnt.setOnClickListener(v -> {
            if (listener != null) listener.onReplyLikeClicked(yanit,parentYorumId,holder.kalpImageViewYnt);
        });


        holder.kullaniciAditextYnt.setOnClickListener(v -> {
            if (listener != null) listener.onUsernameClicked(yanit.getReplyUserId());
        });

        holder.yanitlayazisiynt.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowReplyRepliesClicked(yanit, parentYorumId);
            }
        });

        if (currentUserId != null && currentUserId.equals(yanit.getReplyUserId())) {
            holder.menuButonuYnt.setVisibility(VISIBLE);
            holder.menuButonuYnt.setOnClickListener(menu -> {
                PopupMenu popupmenu = new PopupMenu(context, holder.menuButonuYnt);
                popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_guncelle) {
                        if (listener != null) listener.onReplyUpdate(yanit, parentYorumId);
                        return true;
                    } else if (id == R.id.menu_sil) {
                        if (listener != null) listener.onDeleteReply(yanit, parentYorumId);
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

    private void bindDahaFazla(DahaFazlaViewHolder holder, CommentModel yorum) {
        int toplam = yorum.getSumRepliesCount();
        int sunucudanYuklenen = yorum.getReplies().size();


        int kalan = toplam - sunucudanYuklenen;

        if (kalan > 0) {
            holder.dahaFazlaText.setText(kalan + " yanıt daha göster");
        } else {
            holder.dahaFazlaText.setText("Daha fazla yanıt göster");
        }

        holder.dahaFazlaText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLoadMoreRepliesClicked(yorum);
            }
        });
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView kullaniciAditext, yorumText, yorumTarihiText, yanitlariGor, yanitlamayiGetir, begeniSayisiTextView;
        ImageView menuButonu, kalpImageView, YorumFotoImageView;
        LinearLayout yanitlarYukleniyorLayout2,likeLayout,yanitlarLayout;

        public CommentViewHolder(View itemView) {
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