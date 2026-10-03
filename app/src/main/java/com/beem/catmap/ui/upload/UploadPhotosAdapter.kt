package com.beem.catmap.ui.upload

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import coil.size.Scale
import com.beem.catmap.databinding.ItemUploadCatPhotoBinding
import com.beem.catmap.ui.manager.image.ImageUploadManager
import com.beem.catmap.ui.manager.image.UploadSession

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
            binding.ivCapturedPhoto.load(uri) {
                crossfade(true)
                crossfade(150)
                scale(Scale.FILL)
                diskCachePolicy(CachePolicy.DISABLED)
                memoryCachePolicy(CachePolicy.ENABLED)
            }

            binding.btnRemovePhoto.setOnClickListener {
                ImageUploadManager.removeImage(UploadSession.GENERAL, uri)
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