package com.emrullah.catmap.engellenenler;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.emrullah.catmap.mesaj.MesajFotoAdapter;
import com.emrullah.catmap.ui.main.ProfilSayfasiFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class engellenenlerAdapter extends RecyclerView.Adapter<engellenenlerAdapter.engellenenViewHolder> {
    Context context;
    private RecyclerView recyclerView;
    private engellenenlerAdapter  adapter;
    private ArrayList<Kullanici> engellenenlerList = new ArrayList<>();
    private OnEngelClickListener listener;
    private onKullaniciTiklandi klistener;

    public interface OnEngelClickListener {
        void onEngelClick(Kullanici kullanici);
    }
    public interface onKullaniciTiklandi {
        void onadClick(String id);
    }
    public void setOnEngelClickListener(OnEngelClickListener listener) {
        this.listener = listener;
    }
    public void setOnadClickListener(onKullaniciTiklandi listener) {
        this.klistener = listener;
    }
    public void remove(Kullanici kullanici) {
        int position = engellenenlerList.indexOf(kullanici);
        if (position != -1) {
            engellenenlerList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public engellenenlerAdapter(Context context, ArrayList<Kullanici> engellenenlerList) {
        this.context = context;
        this.engellenenlerList = engellenenlerList;
    }
    public void setListe(ArrayList<Kullanici>liste) {
        this.engellenenlerList = liste;
    }

    @NonNull
    @Override
    public engellenenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_engellenen, parent, false);
        return new engellenenViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull engellenenlerAdapter.engellenenViewHolder holder, int position) {
        Kullanici kullanici = engellenenlerList.get(position);

        holder.RecyclerkullaniciAdi.setText(kullanici.getKullaniciAdi());
        Picasso.get()
                .load(kullanici.getFotoUrl())
                .fit()
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .into(holder.recyclerFotoImageView);
        holder.engel.setOnClickListener(b->{
            if (listener != null) {
                listener.onEngelClick(kullanici);
            }
        });
        holder.RecyclerkullaniciAdi.setOnClickListener(v->{
            if (klistener != null) {
                klistener.onadClick(kullanici.getID());
            }
        });

    }

    @Override
    public int getItemCount() {
        return engellenenlerList.size();
    }

    public static class engellenenViewHolder extends RecyclerView.ViewHolder{
         CircleImageView recyclerFotoImageView;
         TextView RecyclerkullaniciAdi;
         Button engel;

        public engellenenViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerFotoImageView=itemView.findViewById(R.id.recyclerFotoImageView);
            RecyclerkullaniciAdi=itemView.findViewById(R.id.RecyclerkullaniciAdi);
            engel=itemView.findViewById(R.id.engel);

        }
    }

}
