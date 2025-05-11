package com.emrullah.catmap;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class YorumAdapter extends RecyclerView.Adapter<YorumAdapter.YorumViewHolder> {
    private List<Yorumlar> yorumList;
    private Context context;

    public YorumAdapter(List<Yorumlar> yorumList, Context context) {
        this.yorumList = yorumList;
        this.context = context;
    }

    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yorum_icin, parent, false);
        return new YorumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {
        Yorumlar yorum = yorumList.get(position);
        holder.kullaniciAdiTextView.setText(yorum.getKullanici_isim());
        holder.yorumTextView.setText(yorum.getYorum());
        com.google.firebase.Timestamp timestamp = yorum.getTarih();
        Date date=timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String formattedDate = sdf.format(date);
        holder.tarihTextView.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return yorumList.size();
    }

    public class YorumViewHolder extends RecyclerView.ViewHolder {
        TextView kullaniciAdiTextView;
        TextView yorumTextView;
        TextView  tarihTextView;

        public YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAdiTextView = itemView.findViewById(R.id.kullaniciAdiTextView);
            yorumTextView = itemView.findViewById(R.id.yorumTextView);
            tarihTextView = itemView.findViewById(R.id.tarihTextView);
        }
    }
}
