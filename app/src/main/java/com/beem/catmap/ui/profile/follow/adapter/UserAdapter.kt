package com.beem.catmap.ui.profile.follow.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import coil.size.Scale
import com.beem.catmap.R
import com.beem.catmap.data.model.UserProfileData

class UserAdapter(
    private val listType: ListType,
    private val onUserClick: (String?) -> Unit
) : ListAdapter<UserProfileData, UserAdapter.ViewHolder>(KullaniciDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerFotoImageView: ImageView = itemView.findViewById(R.id.recyclerFotoImageView)
        private val recyclerUsername: TextView = itemView.findViewById(R.id.RecyclerkullaniciAdi)
        private val btnFollowing: TextView = itemView.findViewById(R.id.takipediyosa)

        fun bind(user: UserProfileData, listType: ListType, onUserClick: (String?) -> Unit) {
            recyclerUsername.text = user.username

            recyclerFotoImageView.load(user.photoUrl) {
                crossfade(true)
                crossfade(200)
                scale(Scale.FILL)
                placeholder(R.drawable.kullanici)
                error(R.drawable.kullanici)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
            }

            when (listType) {
                ListType.FOLLOWING -> {
                    btnFollowing.text = "Takip"
                }
                ListType.FOLLOWERS -> {
                    btnFollowing.text = "Takipçi"
                }
            }


            recyclerUsername.setOnClickListener {
                onUserClick(user.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.herbi_profil_icin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), listType, onUserClick)
    }

    private class KullaniciDiffCallback : DiffUtil.ItemCallback<UserProfileData>() {
        override fun areItemsTheSame(oldItem: UserProfileData, newItem: UserProfileData): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UserProfileData, newItem: UserProfileData): Boolean {
            return oldItem == newItem
        }
    }
}