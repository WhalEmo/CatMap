package com.beem.catmap.ui.chatlist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.beem.catmap.models.RecentChat
import com.beem.catmap.databinding.SohbetKutusuBinding
import com.bumptech.glide.Glide

class RecentChatsAdapter(
    private val onChatClick: (RecentChat) -> Unit
) : ListAdapter<RecentChat, RecentChatsAdapter.RecentChatViewHolder>(RecentChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatViewHolder {
        val binding = SohbetKutusuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentChatViewHolder(private val binding: SohbetKutusuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: RecentChat) {
            binding.kisiAdi.text = chat.otherUserName
            binding.sonMesaj.text = chat.lastMessage

            // Okunmamış mesaj sayısı rozeti
            if (chat.unreadCount > 0) {
                binding.okunmamisSayac.text = chat.unreadCount.toString()
                binding.okunmamisSayac.isVisible = true
            } else {
                binding.okunmamisSayac.isVisible = false
            }

            // Profil Resmi Yükleme (Glide)
            Glide.with(binding.root.context)
                .load(chat.otherUserPhotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(binding.kisiFoto)

            binding.root.setOnClickListener {
                Log.d("RecentChatDebug", "binding.root.setOnClickListener {")
                onChatClick(chat)
            }
        }
    }

    class RecentChatDiffCallback : DiffUtil.ItemCallback<RecentChat>() {
        override fun areItemsTheSame(oldItem: RecentChat, newItem: RecentChat) = oldItem.chatId == newItem.chatId
        override fun areContentsTheSame(oldItem: RecentChat, newItem: RecentChat) = oldItem == newItem
    }
}