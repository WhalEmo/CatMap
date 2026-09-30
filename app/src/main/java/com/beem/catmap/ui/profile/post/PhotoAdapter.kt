package com.beem.catmap.ui.profile.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import coil.size.Scale
import com.beem.catmap.R

class PhotoAdapter(
    private var photoUrlList: List<String> = emptyList()
) : RecyclerView.Adapter<PhotoAdapter.FotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.foto_item, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val url = photoUrlList[position]

        // Picasso ve manuel ObjectAnimator yerine Coil'in optimize pipeline'ı:
        holder.imageView.load(url) {
            crossfade(true)
            crossfade(350)
            placeholder(R.drawable.cat_pulse_placeholder)
            error(R.drawable.kullanici)
            scale(Scale.FILL)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }
    }

    override fun getItemCount(): Int = photoUrlList.size

    fun updateData(newPhotos: List<String>) {
        this.photoUrlList = newPhotos
        notifyDataSetChanged()
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postImageView)
    }
}