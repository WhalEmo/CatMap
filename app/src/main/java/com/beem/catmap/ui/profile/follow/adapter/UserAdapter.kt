package com.beem.catmap.ui.profile.follow.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.R
import com.bumptech.glide.Glide

class UserAdapter(
    private val onUserClick: (String?) -> Unit
) : ListAdapter<UserModel, UserAdapter.ViewHolder>(KullaniciDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerFotoImageView: ImageView = itemView.findViewById(R.id.recyclerFotoImageView)
        private val recyclerUsername: TextView = itemView.findViewById(R.id.RecyclerkullaniciAdi)
        private val btnFollowing: TextView = itemView.findViewById(R.id.takipediyosa)

        fun bind(userModel: UserModel, onUserClick: (String?) -> Unit) {
            recyclerUsername.text = userModel.username


            Glide.with(itemView.context)
                .load(userModel.photoUrl)
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(recyclerFotoImageView)

            when {
                userModel.isFollowing == 2 -> {
                    btnFollowing.text = "Takip"
                }
                userModel.isFollowers == 2 -> {
                    btnFollowing.text = "Takipçi"
                }
                else -> {
                    btnFollowing.text = "Takip et"
                    btnFollowing.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                }
            }

            recyclerUsername.setOnClickListener {
                onUserClick(userModel.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.herbi_profil_icin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onUserClick)
    }

    private class KullaniciDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
}