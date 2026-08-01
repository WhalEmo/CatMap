package com.beem.catmap.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.beem.catmap.databinding.MesajBinding
import com.beem.catmap.mesaj.MesajFotoGonderYonetici
import com.beem.catmap.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (ChatMessage, View) -> Unit,
    private val onReplyClick: (ChatMessage) -> Unit,
    private val onPhotoClick: (List<String>) -> Unit
) : ListAdapter<ChatMessage, MessageAdapter.MesajViewHolder>(MessageDiffCallback()) {

    var isBlocked: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesajViewHolder {
        val binding = MesajBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesajViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesajViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
    }

    inner class MesajViewHolder(
        private val binding: MesajBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val context = itemView.context

            // 1. Görünümleri Sıfırla (Reset State)
            resetViews()

            val isMyMessage = message.senderId == currentUserId
            val formattedTime = formatTimestamp(message.timestamp)

            // 2. Yanıtlanan (Reply) Mesaj Önizlemesi
            if (message is ChatMessage.Reply && message.repliedMessage != null) {
                val replyText = when (val parent = message.repliedMessage) {
                    is ChatMessage.Text -> parent.message
                    is ChatMessage.Photo -> "📷 Fotoğraf"
                    is ChatMessage.Reply -> parent.message
                    else -> "empty"
                }

                if (isMyMessage) {
                    binding.cevapKutusu.isVisible = true
                    binding.cevapMetni.text = replyText
                    binding.cevapKutusu.setOnClickListener { onReplyClick(message.repliedMessage) }
                } else {
                    binding.SolcevapKutusu.isVisible = true
                    binding.SolcevapMetni.text = replyText
                    binding.SolcevapKutusu.setOnClickListener { onReplyClick(message.repliedMessage) }
                }
            }

            // 3. Mesaj Türüne Göre Rendering (Pattern Matching)
            when (message) {
                is ChatMessage.Text, is ChatMessage.Reply -> {
                    val messageContent = if (message is ChatMessage.Text) message.message else (message as ChatMessage.Reply).message

                    if (isMyMessage) {
                        binding.sagMesajLayout.isVisible = true
                        binding.sagMesajText.isVisible = true
                        binding.sagMesajText.text = messageContent.trim()
                        binding.sagZaman.text = formattedTime
                        binding.gorulmeIkon.setImageResource(
                            if (message.isRead) R.drawable.patidolu else R.drawable.patibos
                        )
                    } else {
                        binding.solMesajLayout.isVisible = true
                        binding.solMesajText.isVisible = true
                        binding.solMesajText.text = messageContent.trim()
                        binding.solZaman.text = formattedTime
                    }
                }

                is ChatMessage.Photo -> {
                    if (isMyMessage) {
                        binding.sagMesajLayout.isVisible = true
                        binding.sagFotoLayout.isVisible = true
                        binding.sagZaman.text = formattedTime
                        binding.gorulmeIkon.setImageResource(
                            if (message.isRead) R.drawable.patidolu else R.drawable.patibos
                        )
                        binding.sagplaceholder.isVisible = false
                    } else {
                        binding.solMesajLayout.isVisible = true
                        binding.solFotoLayout.isVisible = true
                        binding.solZaman.text = formattedTime
                    }

                    // Fotoğraf Detayı Tıklaması
                    val targetLayout = if (isMyMessage) binding.sagFotoLayout else binding.solFotoLayout
                    targetLayout.setOnClickListener {
                        onPhotoClick(message.photoUrls)
                    }
                }
            }

            // 4. Uzun Basma (Long Click) İşlemi
            binding.sagMesajLayout.setOnLongClickListener { v ->
                if (!isBlocked) {
                    onMessageLongClick(message, v)
                }
                true
            }
        }

        private fun resetViews() {
            binding.solMesajLayout.isVisible = false
            binding.sagMesajLayout.isVisible = false
            binding.solFotoLayout.isVisible = false
            binding.sagFotoLayout.isVisible = false
            binding.sagMesajText.isVisible = false
            binding.solMesajText.isVisible = false
            binding.SolcevapKutusu.isVisible = false
            binding.cevapKutusu.isVisible = false
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}

/**
 * Modern ChatMessage Yapısıyla Uyumlu DiffUtil
 */
class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}