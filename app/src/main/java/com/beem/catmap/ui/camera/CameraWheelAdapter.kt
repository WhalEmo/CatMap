package com.beem.catmap.ui.camera


import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.databinding.ItemBottomSelectorWheelBinding
import com.bumptech.glide.Glide

class CameraWheelAdapter(private var capturedImages: List<Uri> = emptyList()) :
    RecyclerView.Adapter<CameraWheelAdapter.WheelViewHolder>() {

    fun updateList(newList: List<Uri>) {
        val tCopyList = newList.toList()

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = capturedImages.size + 1
            override fun getNewListSize(): Int = tCopyList.size + 1

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition == 0 && newItemPosition == 0) return true
                if (oldItemPosition == 0 || newItemPosition == 0) return false
                return capturedImages[oldItemPosition - 1] == tCopyList[newItemPosition - 1]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition == 0 && newItemPosition == 0) return true
                if (oldItemPosition == 0 || newItemPosition == 0) return false
                return capturedImages[oldItemPosition - 1] == tCopyList[newItemPosition - 1]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.capturedImages = tCopyList
        diffResult.dispatchUpdatesTo(this)
    }

    fun getImageAt(position: Int): Uri? {
        if (position <= 0 || position > capturedImages.size) return null
        return capturedImages[position - 1]
    }

    inner class WheelViewHolder(val binding: ItemBottomSelectorWheelBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WheelViewHolder {
        val binding = ItemBottomSelectorWheelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WheelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WheelViewHolder, position: Int) {
        if (position == 0) {
            // 🎯 POZİSYON 0: Canlı Kamera Modu (Turuncu Deklanşör)
            holder.binding.viewCameraShutter.visibility = View.VISIBLE
            holder.binding.ivCapturedThumb.visibility = View.GONE
        } else {
            // 🎯 POZİSYON > 0: Çekilen Fotoğraf Vagonu
            holder.binding.viewCameraShutter.visibility = View.GONE
            holder.binding.ivCapturedThumb.visibility = View.VISIBLE

            val imageUri = capturedImages[position - 1]
            Glide.with(holder.itemView.context)
                .load(imageUri)
                .centerCrop()
                .into(holder.binding.ivCapturedThumb)
        }
    }

    // Sabit deklanşörümüzden dolayı toplam eleman sayısı liste boyutunun 1 fazlasıdır
    override fun getItemCount(): Int = capturedImages.size + 1
}