package com.beem.catmap.Profil.Takipler

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
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.R
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class KullanicilarAdapter(
    private val onUserClick: (String?) -> Unit
) : ListAdapter<Kullanici, KullanicilarAdapter.ViewHolder>(KullaniciDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerFotoImageView: ImageView = itemView.findViewById(R.id.recyclerFotoImageView)
        private val recyclerKullaniciAdi: TextView = itemView.findViewById(R.id.RecyclerkullaniciAdi)
        private val takipEdiyosaButton: MaterialButton = itemView.findViewById(R.id.takipediyosa)

        fun bind(kullanici: Kullanici, onUserClick: (String?) -> Unit) {
            recyclerKullaniciAdi.text = kullanici.kullaniciAdi


            Glide.with(itemView.context)
                .load(kullanici.fotoUrl)
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(recyclerFotoImageView)

            when {
                kullanici.takipEdiyorMuyum == 2 -> {
                    takipEdiyosaButton.text = "Takip"
                }
                kullanici.takipciMi == 2 -> {
                    takipEdiyosaButton.text = "Takipçi"
                }
                else -> {
                    takipEdiyosaButton.text = "Takip et"
                    takipEdiyosaButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                }
            }

            recyclerKullaniciAdi.setOnClickListener {
                onUserClick(kullanici.id)
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

    private class KullaniciDiffCallback : DiffUtil.ItemCallback<Kullanici>() {
        override fun areItemsTheSame(oldItem: Kullanici, newItem: Kullanici): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Kullanici, newItem: Kullanici): Boolean {
            return oldItem == newItem
        }
    }
}