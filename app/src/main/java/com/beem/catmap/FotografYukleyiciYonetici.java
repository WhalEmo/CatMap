package com.beem.catmap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FotografYukleyiciYonetici {
    Map<String, Bitmap> fotoCache = new HashMap<>();
    List<Target> targetListesi = new ArrayList<>();


    public FotografYukleyiciYonetici(Map<String, Bitmap> fotoCache, List<Target> targetListesi) {
        this.fotoCache = fotoCache;
        this.targetListesi = targetListesi;
    }

    public void Yukleyici(Uri Url, ImageView view, View yukleniyorOverlay, ProgressBar yukleniyorProgressBar) {
        if (fotoCache.containsKey(Url.toString())) {
            view.setImageBitmap(fotoCache.get(Url.toString()));
            return;
        }

        Target t = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                fotoCache.put(Url.toString(), bitmap);
                view.setImageBitmap(bitmap);
                targetListesi.remove(this);
                yukleniyorOverlay.setVisibility(View.GONE);
                yukleniyorProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.e("PİCASSO", "Fotoğraf yüklenemedi: " + e.getMessage());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                yukleniyorOverlay.setVisibility(View.VISIBLE);
                yukleniyorProgressBar.setVisibility(View.VISIBLE);
            }
        };

        // Çöp toplayıcıya kaptırmamak için Target'ı listeye ekle
        targetListesi.add(t);
        Picasso.get().load(Url).into(t);
    }
}
