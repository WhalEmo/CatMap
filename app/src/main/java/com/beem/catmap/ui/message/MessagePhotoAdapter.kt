package com.beem.catmap.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import coil.size.Scale
import coil.size.Size
import com.beem.catmap.R

class MessagePhotoAdapter(
    private val photoList: List<String>
) : RecyclerView.Adapter<MessagePhotoAdapter.FotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mesajlasma_foto_itemleri, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val url = photoList[position]

        holder.fotoSayaci.text = "${position + 1} / ${photoList.size}"

        holder.imageView.load(url) {
            crossfade(true)
            crossfade(250)
            size(Size.ORIGINAL)
            scale(Scale.FIT)
            placeholder(R.drawable.placeholder)
            error(R.drawable.placeholder)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }
    }

    override fun getItemCount(): Int = photoList.size

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageFullScreen)
        val fotoSayaci: TextView = itemView.findViewById(R.id.fotoSayaci)
    }
}