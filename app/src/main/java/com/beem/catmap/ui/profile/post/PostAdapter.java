package com.beem.catmap.ui.profile.post;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.R;
import com.beem.catmap.data.model.Post;
import com.squareup.picasso.Picasso;

public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {
    private final OnPostClickListener listener;
    public interface OnPostClickListener {
        void onClick(Post post);
    }
    public PostAdapter(OnPostClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Post>() {
                @Override
                public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
                    return oldItem.getCatId() != null && oldItem.getCatId().equals(newItem.getCatId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
                    return oldItem.equals(newItem);
                }
            };
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gonderigriditem, parent, false);
        return new PostViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post);
    }
    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView postImage;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postImage = itemView.findViewById(R.id.gonderiResmi);
        }
        void bind(Post post) {
            itemView.setOnClickListener(v -> {
                if(listener != null){
                    listener.onClick(post);
                }
            });
            String firstPhotoUrl = null;
            if(post.getPhotoUrlList() != null &&
                    !post.getPhotoUrlList().isEmpty()){

                firstPhotoUrl = post.getPhotoUrlList().get(0);
            }
            if(firstPhotoUrl != null && !firstPhotoUrl.isEmpty()){
                Picasso.get()
                        .load(firstPhotoUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.kullanici)
                        .error(R.drawable.kullanici)
                        .into(postImage);

            }else{
                postImage.setImageResource(R.drawable.kullanici);
            }
        }
    }
}