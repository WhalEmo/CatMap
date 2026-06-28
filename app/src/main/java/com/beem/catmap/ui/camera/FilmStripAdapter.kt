package com.beem.catmap.ui.camera

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.databinding.ItemFilmStripBinding
import com.bumptech.glide.Glide

class FilmStripAdapter(
    private val onImageClick: (Uri) -> Unit,
    private val onImageDelete: (Uri) -> Unit
) : RecyclerView.Adapter<FilmStripAdapter.StripViewHolder>() {

    private var imageUris: List<Uri> = emptyList()

    fun updateList(newList: List<Uri>) {
        // 🎯 Bellek referansını kopararak DiffUtil'in kusursuz çalışmasını sağlıyoruz
        val tCopyList = newList.toList()

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = imageUris.size
            override fun getNewListSize(): Int = tCopyList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return imageUris[oldItemPosition] == tCopyList[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return imageUris[oldItemPosition] == tCopyList[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.imageUris = tCopyList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class StripViewHolder(val binding: ItemFilmStripBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StripViewHolder {
        val binding = ItemFilmStripBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StripViewHolder, position: Int) {
        val uri = imageUris[position]

        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .into(holder.binding.ivThumb)

        // Fotoğrafa tıklanınca tam ekran önizleme tetikle
        holder.binding.ivThumb.setOnClickListener { onImageClick(uri) }

        // Çarpı butonuna tıklanınca havuzdan silme operasyonunu tetikle
        holder.binding.btnDeleteContainer.setOnClickListener { onImageDelete(uri) }
    }

    override fun getItemCount(): Int = imageUris.size
}