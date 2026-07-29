package com.beem.catmap.Profil.Gonderiler;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.Maps.FotoYuklemeListener;
import com.beem.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.FotoViewHolder> {

    private ArrayList<String> fotoUrlListesi;
    private FotoYuklemeListener listener;
    private int yuklenenFotoSayisi = 0;

    public FotoAdapter(ArrayList<String> fotoUrlListesi, FotoYuklemeListener listener) {
        this.fotoUrlListesi = (fotoUrlListesi != null) ? fotoUrlListesi : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.foto_item, parent, false);
        return new FotoViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        String url = fotoUrlListesi.get(position);


        if (holder.imageView.getTag() instanceof ObjectAnimator) {
            ((ObjectAnimator) holder.imageView.getTag()).cancel();
        }
        holder.imageView.animate().cancel();
        holder.imageView.setAlpha(0f);
        holder.imageView.setBackgroundResource(R.drawable.cat_pulse_placeholder);

        ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(holder.imageView, "alpha", 0.4f, 1f);
        pulseAnim.setDuration(800);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnim.setInterpolator(new DecelerateInterpolator());

        holder.imageView.setTag(pulseAnim);

        pulseAnim.start();

        Picasso.get()
                .load(url)
                .fit()
                .centerCrop()
                .into(holder.imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        pulseAnim.cancel();

                        holder.imageView.setAlpha(0f);

                        holder.imageView.animate()
                                .alpha(1f)
                                .setDuration(450)
                                .setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> {
                                    holder.imageView.setBackground(null);
                                })
                                .start();
                    }

                    @Override
                    public void onError(Exception e) {
                        pulseAnim.cancel();
                        holder.imageView.setAlpha(1f);

                        holder.imageView.setImageResource(R.drawable.kullanici);
                    }
                });
    }

    @Override
    public int getItemCount() {
        if (fotoUrlListesi == null) {
            return 0;
        }
        return fotoUrlListesi.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.postImageView);
        }
    }
}


