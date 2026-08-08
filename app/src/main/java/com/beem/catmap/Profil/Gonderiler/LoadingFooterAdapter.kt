package com.beem.catmap.Profil.Gonderiler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R

class LoadingFooterAdapter : RecyclerView.Adapter<LoadingFooterAdapter.FooterViewHolder>() {

    private var isLoading = false

    fun setLoading(loading: Boolean) {
        if (this.isLoading != loading) {
            this.isLoading = loading
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = if (isLoading) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        // Ekstra işlem gerekmiyor
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}