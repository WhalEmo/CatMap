package com.beem.catmap.ui.upload

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.databinding.ItemUploadCatPhotoBinding
import com.beem.catmap.ui.manager.ImageUploadManager
import com.bumptech.glide.Glide

class UploadPhotosAdapter : RecyclerView.Adapter<UploadPhotosAdapter.PhotoViewHolder>() {

    private val imageList = mutableListOf<Uri>()

    fun updateList(newList: List<Uri>) {
        val diffCallback = PhotoDiffCallback(imageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        imageList.clear()
        imageList.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemUploadCatPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

    inner class PhotoViewHolder(private val binding: ItemUploadCatPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.ivCapturedPhoto.context)
                .load(uri)
                .centerCrop()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .into(binding.ivCapturedPhoto)

            binding.btnRemovePhoto.setOnClickListener {
                ImageUploadManager.removeImage(uri)
            }
        }
    }

    private class PhotoDiffCallback(
        private val oldList: List<Uri>,
        private val newList: List<Uri>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Uriler aynı mı kontrolü
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // İçerik aynı mı kontrolü
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}