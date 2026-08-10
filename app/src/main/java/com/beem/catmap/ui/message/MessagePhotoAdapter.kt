package com.beem.catmap.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.bumptech.glide.Glide

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

        // 🚀 Yüksek Çözünürlüklü ve Pürüzsüz Yükleme
        Glide.with(holder.itemView.context)
            .asBitmap()
            .load(url)
            .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .fitCenter()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photoList.size

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageFullScreen)
        val fotoSayaci: TextView = itemView.findViewById(R.id.fotoSayaci)
    }
}