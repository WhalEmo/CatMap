package com.beem.catmap.Profil.Takipler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R

class FooterAdapter : RecyclerView.Adapter<FooterAdapter.FooterViewHolder>() {

    private var isLoading: Boolean = false

    fun setLoading(loading: Boolean) {
        if (isLoading != loading) {
            isLoading = loading
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = if (isLoading) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.bind(isLoading)
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(isLoading: Boolean) {
            itemView.isVisible = isLoading
        }
    }
}