package com.beem.catmap.ui.camera

import android.graphics.PorterDuff
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.beem.catmap.databinding.ItemCameraThumbPlaceholderBinding
import com.bumptech.glide.Glide

class CameraThumbAdapter(private var imageUris: List<Uri> = emptyList()) :
    RecyclerView.Adapter<CameraThumbAdapter.ThumbViewHolder>() {

    fun updateList(newList: List<Uri>) {
        val tCopyList = newList.toList()

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = imageUris.size
            override fun getNewListSize(): Int = tCopyList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return imageUris[oldItemPosition] == tCopyList[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // 🎯 SİHİRLİ GEOMETRİ KONTROLÜ:
                // Uri'ler aynı olsa bile, elemanın listedeki yeri (en son eleman olup olmama durumu)
                // değiştiyse içeriği DEĞİŞMİŞ say ki çizgiyi doğru güncelleyebilelim!
                val isOldLast = oldItemPosition == imageUris.size - 1
                val isNewLast = newItemPosition == tCopyList.size - 1

                return imageUris[oldItemPosition] == tCopyList[newItemPosition] && isOldLast == isNewLast
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.imageUris = tCopyList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ThumbViewHolder(val binding: ItemCameraThumbPlaceholderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemCameraThumbPlaceholderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        val uri = imageUris[position]

        // 🎯 GÜVENLİK: Animasyon kalıntılarından dolayı görünmez kalmasın diye resetle
        holder.itemView.alpha = 1.0f
        holder.itemView.translationX = 0f

        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .into(holder.binding.ivThumb)

        // Dinamik Renk Yönetimi (En son eleman turuncu, eskiler gri)
        val isLatestAmount = (position == imageUris.size - 1)

        if (isLatestAmount) {
            val orangeColor = ContextCompat.getColor(holder.itemView.context, R.color.catmap_accent)
            holder.binding.viewBorderOverlay.setBackgroundColor(Color.TRANSPARENT)
            holder.binding.viewBorderOverlay.setBackgroundResource(R.drawable.bg_camera_thumb_border)
            holder.binding.viewLinkLine.setBackgroundColor(orangeColor)
        } else {
            val mutedColor = ContextCompat.getColor(holder.itemView.context, R.color.catmap_text_muted)
            val mutedBorder = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_camera_thumb_border)?.mutate()
            mutedBorder?.setColorFilter(mutedColor, PorterDuff.Mode.SRC_IN)

            holder.binding.viewBorderOverlay.background = mutedBorder
            holder.binding.viewLinkLine.setBackgroundColor(mutedColor)
        }

        // 🎯 ARTIK KUSURSUZ ÇALIŞAN ÇİZGİ MANTIĞI
        if (position == imageUris.size - 1) {
            holder.binding.viewLinkLine.visibility = View.GONE
        } else {
            holder.binding.viewLinkLine.visibility = View.VISIBLE
        }
    }

    override fun onViewRecycled(holder: ThumbViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1.0f
        holder.itemView.translationX = 0f
    }

    override fun getItemCount(): Int = imageUris.size
}