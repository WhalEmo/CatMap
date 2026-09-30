package com.beem.catmap.ui.markersclick

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.ui.markersclick.components.CatPhotoItem

class ComposePhotoPagerAdapter(
    private val onPhotoClick: (Int) -> Unit
) : ListAdapter<String, ComposePhotoPagerAdapter.PhotoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PhotoViewHolder(composeView, onPhotoClick)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PhotoViewHolder(
        private val composeView: ComposeView,
        private val onPhotoClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(composeView) {

        fun bind(photoUrl: String, position: Int) {
            composeView.setContent {
                CatPhotoItem(
                    url = photoUrl,
                    onOpenFullScreen = { onPhotoClick(position) }
                )
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
}