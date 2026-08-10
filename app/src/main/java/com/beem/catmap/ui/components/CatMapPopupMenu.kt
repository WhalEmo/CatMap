package com.beem.catmap.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.beem.catmap.R
import com.beem.catmap.databinding.ItemCatmapPopupMenuBinding
import android.widget.LinearLayout

class CatMapPopupMenu private constructor(
    private val context: Context,
    private val items: List<MenuItem>
) {

    data class MenuItem(
        val id: Int,
        val title: String,
        @DrawableRes val iconRes: Int? = null,
        @ColorInt val textColor: Int? = null,
        @ColorInt val iconTint: Int? = null,
        val isVisible: Boolean = true,
        val isEnabled: Boolean = true,
        val onClick: () -> Unit
    )

    fun show(anchorView: View) {
        val visibleItems = items.filter { it.isVisible }
        if (visibleItems.isEmpty()) return

        val inflater = LayoutInflater.from(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_catmap_popup)
            elevation = 16f
            setPadding(0, 8, 0, 8)
        }

        val popupWindow = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Odaklanılabilir (Boşluğa basınca kapanır)
        ).apply {
            elevation = 16f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            animationStyle = android.R.style.Animation_Dialog
        }

        visibleItems.forEachIndexed { index, menuItem ->
            val itemButton = ItemCatmapPopupMenuBinding.inflate(inflater, container, false).root

            itemButton.text = menuItem.title

            // 🟢 YENİ: Butonun aktiflik durumunu MaterialButton'a işliyoruz
            itemButton.isEnabled = menuItem.isEnabled

            // Yazı Rengi Özelleştirme (Eğer buton pasifse rengi yarı saydam/muted yaparak ezilmesini sağlıyoruz)
            if (menuItem.isEnabled) {
                menuItem.textColor?.let { itemButton.setTextColor(it) }
            } else {
                // Pasif durum için Material 3 standart sönük rengi (veya projedeki text_muted)
                itemButton.setTextColor(ContextCompat.getColor(context, R.color.catmap_text_muted))
            }

            // İkon Yönetimi
            if (menuItem.iconRes != null) {
                itemButton.icon = ContextCompat.getDrawable(context, menuItem.iconRes)

                if (menuItem.isEnabled) {
                    menuItem.iconTint?.let { itemButton.iconTint = ColorStateList.valueOf(it) }
                } else {
                    itemButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.catmap_text_muted))
                }
            } else {
                itemButton.icon = null
            }

            // Tıklama Olayı (Sadece aktifse tıklanabilir)
            if (menuItem.isEnabled) {
                itemButton.setOnClickListener {
                    popupWindow.dismiss()
                    menuItem.onClick()
                }
            }

            container.addView(itemButton)

            if (index < visibleItems.size - 1) {
                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(12, 0, 12, 0)
                    }
                    setBackgroundColor(ContextCompat.getColor(context, R.color.catmap_divider))
                    alpha = 0.08f
                }
                container.addView(divider)
            }
        }

        // Pop-up'ı tam olarak 3 noktanın altına hizalıyoruz
        popupWindow.showAsDropDown(anchorView, -30, 10)
    }

    /**
     * 🏗️ CATMAP POPUP MENU BUILDER
     */
    class Builder(private val context: Context) {
        private val items = mutableListOf<MenuItem>()

        fun addItem(
            id: Int,
            title: String,
            @DrawableRes iconRes: Int? = null,
            @ColorInt textColor: Int? = null,
            @ColorInt iconTint: Int? = null,
            isVisible: Boolean = true,
            isEnabled: Boolean = true,
            onClick: () -> Unit
        ) = apply {
            items.add(
                MenuItem(id, title, iconRes, textColor, iconTint, isVisible, isEnabled, onClick)
            )
        }

        fun build(): CatMapPopupMenu {
            return CatMapPopupMenu(context, items)
        }
    }
}