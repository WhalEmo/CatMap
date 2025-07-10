package com.emrullah.catmap.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.KullaniciAdiTiklamaListener;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.emrullah.catmap.Yanit_Model;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Kullanicilar_adapter extends RecyclerView.Adapter<Kullanicilar_adapter.ViewHolder> {

    private Context context;
    private List<Kullanici> kullaniciList;
    public KullaniciAdiTiklamaListener kullaniciAdiTiklamaListener;
    private MainViewModel viewModel;

    public Kullanicilar_adapter(Context context, List<Kullanici> kullaniciList,MainViewModel viewModel) {
        this.context = context;
        this.kullaniciList = kullaniciList;
        this.viewModel=viewModel;
    }

    public void setKullaniciAdiTiklamaListener(KullaniciAdiTiklamaListener listener) {
        this.kullaniciAdiTiklamaListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recyclerFotoImageView;
        TextView RecyclerkullaniciAdi;
        Button takipediyosa;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerFotoImageView=itemView.findViewById(R.id.recyclerFotoImageView);
            RecyclerkullaniciAdi=itemView.findViewById(R.id.RecyclerkullaniciAdi);
            takipediyosa=itemView.findViewById(R.id.takipediyosa);
        }
    }

    @NonNull
    @Override
    public Kullanicilar_adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_profil_icin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Kullanicilar_adapter.ViewHolder holder, int position) {
        Kullanici kullanici = kullaniciList.get(position);
        holder.RecyclerkullaniciAdi.setText(kullanici.getKullaniciAdi());
        Picasso.get()
                .load(kullanici.getFotoUrl())
                .fit()
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .into(holder.recyclerFotoImageView);


        if (kullanici.TakipEdiyorMuyum==2){
            holder.takipediyosa.setText("Takip");
        } else if (kullanici.TakipciMi==2) {
            holder.takipediyosa.setText("Takipçi");
        }else{
            holder.takipediyosa.setText("Takip et");
            holder.takipediyosa.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));

        }

        holder.RecyclerkullaniciAdi.setOnClickListener(t->{
               if (kullaniciAdiTiklamaListener != null) {
                   kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(kullanici.getID());
               }
       });
    }
    @Override
    public int getItemCount() {
        return kullaniciList.size();
    }
}
