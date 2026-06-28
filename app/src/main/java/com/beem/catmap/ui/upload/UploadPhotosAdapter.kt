package com.beem.catmap.ui.upload

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.databinding.ItemUploadCatPhotoBinding
import com.beem.catmap.ui.manager.ImageUploadManager
import com.bumptech.glide.Glide

class UploadPhotosAdapter : RecyclerView.Adapter<UploadPhotosAdapter.PhotoViewHolder>() {

    private val imageList = mutableListOf<Uri>()

    fun updateList(newList: List<Uri>) {
        imageList.clear()
        imageList.addAll(newList)
        notifyDataSetChanged() // İleride buraya DiffUtil çakıp Pinterest akıcılığına uçuracağız dayıcım
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
                .into(binding.ivCapturedPhoto)

            binding.btnRemovePhoto.setOnClickListener {
                ImageUploadManager.removeImage(uri)
            }
        }
    }
}