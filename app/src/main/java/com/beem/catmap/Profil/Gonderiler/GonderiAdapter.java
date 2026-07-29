package com.beem.catmap.Profil.Gonderiler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.R;
import com.beem.catmap.models.Gonderi;
import com.squareup.picasso.Picasso;

public class GonderiAdapter extends ListAdapter<Gonderi, GonderiAdapter.GonderiViewHolder> {
    private final OnGonderiClickListener listener;
    public interface OnGonderiClickListener {
        void onClick(Gonderi gonderi);
    }
    public GonderiAdapter(OnGonderiClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    private static final DiffUtil.ItemCallback<Gonderi> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Gonderi>() {
                @Override
                public boolean areItemsTheSame(@NonNull Gonderi oldItem, @NonNull Gonderi newItem) {
                    return oldItem.getKediID() != null && oldItem.getKediID().equals(newItem.getKediID());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Gonderi oldItem, @NonNull Gonderi newItem) {
                    return oldItem.equals(newItem);
                }
            };
    @NonNull
    @Override
    public GonderiViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gonderigriditem, parent, false);
        return new GonderiViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull GonderiViewHolder holder, int position) {
        Gonderi gonderi = getItem(position);
        holder.bind(gonderi);
    }
    class GonderiViewHolder extends RecyclerView.ViewHolder {
        private final ImageView gonderiResmi;
        public GonderiViewHolder(@NonNull View itemView) {
            super(itemView);
            gonderiResmi = itemView.findViewById(R.id.gonderiResmi);
        }
        void bind(Gonderi gonderi) {
            itemView.setOnClickListener(v -> {
                if(listener != null){
                    listener.onClick(gonderi);
                }
            });
            String ilkFotoUrl = null;
            if(gonderi.getFotoUrlListesi() != null &&
                    !gonderi.getFotoUrlListesi().isEmpty()){

                ilkFotoUrl = gonderi.getFotoUrlListesi().get(0);
            }
            if(ilkFotoUrl != null && !ilkFotoUrl.isEmpty()){
                Picasso.get()
                        .load(ilkFotoUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.kullanici)
                        .error(R.drawable.kullanici)
                        .into(gonderiResmi);

            }else{
                gonderiResmi.setImageResource(R.drawable.kullanici);
            }
        }
    }
}