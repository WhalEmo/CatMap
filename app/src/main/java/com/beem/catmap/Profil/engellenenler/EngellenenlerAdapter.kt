package com.beem.catmap.Profil.engellenenler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.R
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Objects
class EngellenenlerAdapter(
    private val onEngelClick: (Kullanici) -> Unit,
    private val onKullaniciClick: (String) -> Unit
) : ListAdapter<Kullanici, EngellenenlerAdapter.ViewHolder>(DiffCallback()) {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.herbi_engellenen, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val foto = itemView.findViewById<CircleImageView>(R.id.recyclerFotoImageView)
        private val kullaniciAdi = itemView.findViewById<TextView>(R.id.RecyclerkullaniciAdi)
        private val engelButton = itemView.findViewById<Button>(R.id.engel)
        fun bind(kullanici: Kullanici) {
            kullaniciAdi.text = kullanici.kullaniciAdi


            Glide.with(itemView)
                .load(kullanici.fotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .centerCrop()
                .into(foto)



            engelButton.setOnClickListener {
                onEngelClick(kullanici)
            }
            itemView.setOnClickListener {

                kullanici.id?.let {
                    onKullaniciClick(it)
                }

            }
        }
    }



    class DiffCallback :
        DiffUtil.ItemCallback<Kullanici>() {
        override fun areItemsTheSame(oldItem: Kullanici, newItem: Kullanici): Boolean {
            return oldItem.id == newItem.id
        }


        override fun areContentsTheSame(oldItem: Kullanici, newItem: Kullanici): Boolean {
            return Objects.equals(oldItem, newItem);
        }
    }
}