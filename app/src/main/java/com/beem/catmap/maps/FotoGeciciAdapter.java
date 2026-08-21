package com.beem.catmap.maps;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class FotoGeciciAdapter extends ListAdapter<Uri, FotoGeciciAdapter.FotoHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    private final Context baglanti;
    private final FotografYukleyiciYonetici yukleyici;
    private final OnPhotoClickListener onPhotoClickListener;

    private static class FotoDiffCallback extends DiffUtil.ItemCallback<Uri> {
        @Override
        public boolean areItemsTheSame(@NonNull Uri oldItem, @NonNull Uri newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Uri oldItem, @NonNull Uri newItem) {
            return oldItem.toString().equals(newItem.toString());
        }
    }

    public FotoGeciciAdapter(Context baglanti, FotografYukleyiciYonetici yukleyici, OnPhotoClickListener listener) {
        super(new FotoDiffCallback());
        this.baglanti = baglanti;
        this.yukleyici = yukleyici;
        this.onPhotoClickListener = listener;
    }

    @NonNull
    @Override
    public FotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View foto = LayoutInflater.from(baglanti).inflate(R.layout.foto, parent, false);
        return new FotoHolder(foto);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoHolder holder, int position) {
        Uri fotoUri = getItem(position); // ListAdapter içinden öğeyi çeker

        if (yukleyici == null) {
            if (fotoUri != null) {
                Glide.with(holder.itemView.getContext())
                        .load(fotoUri)
                        .override(1080, 1080)
                        .centerCrop()
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.foto);
            }
        } else {
            yukleyici.Yukleyici(fotoUri, holder.foto, holder.yukleniyorOverlay, holder.yukleniyorProgressBar);
        }

        holder.foto.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && onPhotoClickListener != null) {
                onPhotoClickListener.onPhotoClick(pos);
            }
        });
    }

    public static class FotoHolder extends RecyclerView.ViewHolder {
        ImageView foto;
        View yukleniyorOverlay;
        ProgressBar yukleniyorProgressBar;

        public FotoHolder(@NonNull View itemView) {
            super(itemView);
            foto = itemView.findViewById(R.id.fotoView);
            yukleniyorOverlay = itemView.findViewById(R.id.loadingOverlay);
            yukleniyorProgressBar = itemView.findViewById(R.id.loadingProgressBar);
        }
    }
}