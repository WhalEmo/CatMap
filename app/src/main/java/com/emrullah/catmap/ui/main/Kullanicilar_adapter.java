package com.emrullah.catmap.ui.main;

import android.content.Context;
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
import com.emrullah.catmap.R;
import com.emrullah.catmap.Yanit_Model;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
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
        Button takippet;
        Button takipediyosa;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerFotoImageView=itemView.findViewById(R.id.recyclerFotoImageView);
            RecyclerkullaniciAdi=itemView.findViewById(R.id.RecyclerkullaniciAdi);
            takippet=itemView.findViewById(R.id.takippet);
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
        if(kullanici.getTakipEdiliyorMu()==true&&kullanici.getTakipciMi()==true||kullanici.getTakipciMi()==false){
            holder.takipediyosa.setVisibility(View.VISIBLE);//takip ediliyor
            holder.takippet.setVisibility(View.GONE);//takip et
        }else if(kullanici.getTakipciMi()==true&&kullanici.getTakipEdiliyorMu()==false){
            holder.takipediyosa.setVisibility(View.GONE);
            holder.takippet.setText("Sende takip et");
            holder.takippet.setVisibility(View.VISIBLE);
        }
        holder.RecyclerkullaniciAdi.setOnClickListener(t->{
               if (kullaniciAdiTiklamaListener != null) {
                   kullaniciAdiTiklamaListener.onKullaniciAdiTiklandi(kullanici.getID());
               }
       });
        holder.takippet.setOnClickListener(t->{
            kullanici.setTakipEdiliyorMu(true);
            viewModel.TakipEt(kullanici.getID());
            notifyItemChanged(position);
        });
        holder.takipediyosa.setOnClickListener(b->{
            kullanici.setTakipEdiliyorMu(false);
            viewModel.TakiptenCikarma(kullanici.getID());
            notifyItemChanged(position);
        });
    }
    @Override
    public int getItemCount() {
        return kullaniciList.size();
    }
}
