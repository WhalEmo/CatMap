package com.beem.catmap.ui.message

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.R
import androidx.core.graphics.drawable.toDrawable

class MessagePhotoPreviewDialog : DialogFragment() {

    private lateinit var fotoViewPager: ViewPager2
    private lateinit var btnKapat: ImageButton
    private var photoUrls: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
        arguments?.let {
            photoUrls = it.getStringArrayList(ARG_PHOTO_URLS) ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.mesaj_coklu_foto_gosterim, container, false)

        fotoViewPager = view.findViewById(R.id.fotoViewPager)
        btnKapat = view.findViewById(R.id.btnKapat)

        btnKapat.setOnClickListener {
            dismiss()
        }

        val adapter = MessagePhotoAdapter(photoUrls)
        fotoViewPager.adapter = adapter

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    companion object {
        private const val ARG_PHOTO_URLS = "ARG_PHOTO_URLS"

        fun newInstance(photoUrls: List<String>): MessagePhotoPreviewDialog {
            return MessagePhotoPreviewDialog().apply {
                arguments = bundleOf(ARG_PHOTO_URLS to ArrayList(photoUrls))
            }
        }
    }
}