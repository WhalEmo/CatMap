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
import com.beem.catmap.mesaj.Mesaj
import com.beem.catmap.mesaj.MesajFotoGonderYonetici
import com.beem.catmap.mesaj.YanitMesaj

class MessageAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (Mesaj, View) -> Unit,
    private val onReplyClick: (YanitMesaj) -> Unit,
    private val onPhotoClick: () -> Unit
) : ListAdapter<Mesaj, MessageAdapter.MesajViewHolder>(MessageDiffCallback()) {

    var isBlocked: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesajViewHolder {
        val binding = MesajBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesajViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesajViewHolder, position: Int) {
        val mesaj = getItem(position)
        holder.bind(mesaj)
    }

    inner class MesajViewHolder(
        private val binding: MesajBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesaj: Mesaj) {
            val context = itemView.context

            // 1. Tüm Görünümleri Temizle (Reset State)
            binding.solMesajLayout.isVisible = false
            binding.sagMesajLayout.isVisible = false
            binding.solFotoLayout.isVisible = false
            binding.sagFotoLayout.isVisible = false
            binding.sagMesajText.isVisible = false
            binding.solMesajText.isVisible = false
            binding.SolcevapKutusu.isVisible = false
            binding.cevapKutusu.isVisible = false

            // 2. Animasyon Kontrolü (Yanıp Sönme)
            if (mesaj.isYaniyorMu) {
                val anim = AnimationUtils.loadAnimation(context, R.anim.mesaj_anim)
                itemView.startAnimation(anim)
                mesaj.isYaniyorMu = false
            }

            val isMyMessage = mesaj.gonderici == currentUserId

            // 3. Yanıtlanan Mesaj Yapısı
            if (mesaj is YanitMesaj) {
                if (isMyMessage) {
                    binding.cevapKutusu.isVisible = true
                    binding.cevapMetni.text = mesaj.yanitlananMesaj.mesaj
                    binding.cevapKutusu.setOnClickListener { onReplyClick(mesaj) }
                } else {
                    binding.SolcevapKutusu.isVisible = true
                    binding.SolcevapMetni.text = mesaj.yanitlananMesaj.mesaj
                    binding.SolcevapKutusu.setOnClickListener { onReplyClick(mesaj) }
                }
            }

            // 4. Metin vs Fotoğraf Mesajı Ayrımı
            if (mesaj.tur != "foto") {
                if (isMyMessage) {
                    binding.sagMesajLayout.isVisible = true
                    binding.sagMesajText.isVisible = true
                    binding.sagMesajText.text = mesaj.mesaj.trim()
                    binding.sagZaman.text = mesaj.stringZaman
                    binding.gorulmeIkon.setImageResource(
                        if (mesaj.isGoruldu) R.drawable.patidolu else R.drawable.patibos
                    )
                } else {
                    binding.solMesajLayout.isVisible = true
                    binding.solMesajText.isVisible = true
                    binding.solMesajText.text = mesaj.mesaj.trim()
                    binding.solZaman.text = mesaj.stringZaman
                }
            } else {
                // Fotoğraf Mesajı
                if (isMyMessage) {
                    binding.sagMesajLayout.isVisible = true
                    binding.sagFotoLayout.isVisible = true
                    binding.sagZaman.text = mesaj.stringZaman
                    binding.gorulmeIkon.setImageResource(
                        if (mesaj.isGoruldu) R.drawable.patidolu else R.drawable.patibos
                    )

                    if (mesaj.isYuklendiMi) {
                        binding.sagplaceholder.isVisible = false
                        MesajFotoGonderYonetici.getInstance().FotoMesaj(
                            true, null, mesaj, context, Runnable { onPhotoClick() }
                        )
                    }
                } else {
                    binding.solMesajLayout.isVisible = true
                    binding.solZaman.text = mesaj.stringZaman
                    MesajFotoGonderYonetici.getInstance().FotoMesaj(
                        false, null, mesaj, context, Runnable { onPhotoClick() }
                    )
                }
            }

            // 5. Basılı Tutma (Uzun Tıklama) Olayı
            binding.sagMesajLayout.setOnLongClickListener { v ->
                if (!isBlocked) {
                    onMessageLongClick(mesaj, v)
                }
                true
            }
        }
    }
}

/**
 * Otomatik Liste Karşılaştırma ve Performans Optimizasyonu (DiffUtil)
 */
class MessageDiffCallback : DiffUtil.ItemCallback<Mesaj>() {
    override fun areItemsTheSame(oldItem: Mesaj, newItem: Mesaj): Boolean {
        return oldItem.mesajID == newItem.mesajID
    }

    override fun areContentsTheSame(oldItem: Mesaj, newItem: Mesaj): Boolean {
        return oldItem.mesaj == newItem.mesaj &&
                oldItem.isGoruldu == newItem.isGoruldu &&
                oldItem.isYaniyorMu == newItem.isYaniyorMu
    }
}