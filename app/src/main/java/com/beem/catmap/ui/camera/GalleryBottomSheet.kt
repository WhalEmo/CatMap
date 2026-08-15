package com.beem.catmap.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.databinding.BottomSheetGalleryBinding
import com.beem.catmap.databinding.ItemGalleryImageBinding
import com.beem.catmap.ui.manager.ImageUploadManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.net.toUri
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import androidx.core.view.isVisible
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GalleryAdapter
    private val allImagesFromDevice = mutableListOf<String>()

    private val pageSize = 60

    private var isLoading = false
    private var hasMoreImages = true

    private var lastDateAdded: Long? = null
    private var lastImageId: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val requestGalleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                    permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (isGranted) {
            loadGalleryImages()
        } else {
            UiMessageManager.emitMessage(UiMessageState.Error("Galeriye erişim izni verilmedi."))
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewGallery.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = GalleryAdapter(allImagesFromDevice)
        binding.recyclerViewGallery.adapter = adapter

        binding.recyclerViewGallery.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)

                    // Yukarı kaydırıyorsa işlem yapma
                    if (dy <= 0) return

                    val layoutManager =
                        recyclerView.layoutManager as? GridLayoutManager
                            ?: return

                    val totalItemCount =
                        layoutManager.itemCount

                    val lastVisibleItem =
                        layoutManager.findLastVisibleItemPosition()

                    /*
                     * Kullanıcı son 9 fotoğrafa yaklaştığında
                     * yeni sayfayı önceden yükle.
                     *
                     * 3 kolon olduğundan yaklaşık
                     * son 3 satır demek.
                     */
                    val shouldLoadMore =
                        lastVisibleItem >= totalItemCount - 9

                    if (
                        shouldLoadMore &&
                        !isLoading &&
                        hasMoreImages
                    ) {
                        loadGalleryImages(reset = false)
                    }
                }
            }
        )

        if (hasGalleryPermission()) {
            loadGalleryImages()
        } else {
            requestGalleryPermissions()
        }

        lifecycleScope.launchWhenStarted {
            ImageUploadManager.selectedImages.collectLatest { centralUris ->
                updateConfirmButton(centralUris.size)

                adapter.updateSelectedList(centralUris.map { it.toString() })
            }
        }

        binding.btnConfirmSelection.setOnClickListener {
            dismiss()
        }
    }


    private fun loadImagesFromDevice2(): List<String> {
        val tempImageList = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && tempImageList.size < 60) {
                val id = cursor.getLong(idColumn)
                val contentUri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                tempImageList.add(contentUri.toString())
            }
        }
        return tempImageList
    }


    private fun loadImagesFromDevice(): GalleryPage {

        val tempImageList = mutableListOf<String>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val selection: String?
        val selectionArgs: Array<String>?

        val currentLastDateAdded = lastDateAdded
        val currentLastImageId = lastImageId

        if (currentLastDateAdded != null && currentLastImageId != null) {

            selection = """
            ${MediaStore.Images.Media.DATE_ADDED} < ?
            OR (
                ${MediaStore.Images.Media.DATE_ADDED} = ?
                AND ${MediaStore.Images.Media._ID} < ?
            )
        """.trimIndent()

            selectionArgs = arrayOf(
                currentLastDateAdded.toString(),
                currentLastDateAdded.toString(),
                currentLastImageId.toString()
            )

        } else {
            selection = null
            selectionArgs = null
        }

        val sortOrder = """
        ${MediaStore.Images.Media.DATE_ADDED} DESC,
        ${MediaStore.Images.Media._ID} DESC
    """.trimIndent()

        var pageLastDateAdded: Long? = null
        var pageLastImageId: Long? = null
        var hasMore = false

        requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            val dateAddedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (
                cursor.moveToNext() &&
                tempImageList.size < pageSize
            ) {

                val id = cursor.getLong(idColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val contentUri =
                    android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                tempImageList.add(contentUri.toString())

                pageLastImageId = id
                pageLastDateAdded = dateAdded
            }

            // 60 kayıttan sonra en az bir kayıt daha varsa
            // sonraki sayfa var demektir.
            if (tempImageList.size == pageSize) {
                hasMore = cursor.moveToNext()
            }
        }

        return GalleryPage(
            images = tempImageList,
            lastDateAdded = pageLastDateAdded,
            lastImageId = pageLastImageId,
            hasMore = hasMore
        )
    }

    private fun loadGalleryImages(reset: Boolean = true) {

        if (isLoading) return

        if (!reset && !hasMoreImages) return

        if (reset) {
            lastDateAdded = null
            lastImageId = null
            hasMoreImages = true
        }

        isLoading = true

        lifecycleScope.launch {

            try {

                val page = withContext(Dispatchers.IO) {
                    loadImagesFromDevice()
                }

                if (_binding == null) return@launch

                if (reset) {
                    adapter.updateMainImages(page.images)
                } else {
                    adapter.appendImages(page.images)
                }

                lastDateAdded = page.lastDateAdded
                lastImageId = page.lastImageId
                hasMoreImages = page.hasMore

            } finally {
                isLoading = false
            }
        }
    }



    private fun updateConfirmButton(count: Int) {
        if (count > 0) {
            binding.tvSelectionBadge.text = count.toString()

            binding.tvButtonText.text = if (count == 1) "Görseli Onayla ve İlerle" else "Görselleri Onayla ve İlerle"

            if (binding.btnConfirmSelection.isGone) {
                binding.btnConfirmSelection.visibility = View.VISIBLE
                binding.btnConfirmSelection.alpha = 0f
                binding.btnConfirmSelection.translationY = 40f

                binding.btnConfirmSelection.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .setDuration(350)
                    .start()
            }
        } else {
            if (binding.btnConfirmSelection.isVisible) {
                binding.btnConfirmSelection.animate()
                    .alpha(0f)
                    .translationY(40f)
                    .setDuration(250)
                    .withEndAction {
                        binding.btnConfirmSelection.visibility = View.GONE
                    }.start()
            }
        }
    }

    /*
    private fun loadGalleryImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            val deviceImages = loadImagesFromDevice()
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    adapter.updateMainImages(deviceImages)
                }
            }
        }
    }

     */

    private fun requestGalleryPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestGalleryPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun hasGalleryPermission(): Boolean {
        val context = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    inner class GalleryAdapter(private var images: List<String>) :
        RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

        private var selectedUris = listOf<String>()

        fun updateMainImages(newImages: List<String>) {
            this.images = newImages
            notifyDataSetChanged()
        }

        fun updateSelectedList(newList: List<String>) {
            this.selectedUris = newList
        }

        inner class GalleryViewHolder(val itemBinding: ItemGalleryImageBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val binding = ItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return GalleryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val uriString = images[position]

            var isSelected = selectedUris.contains(uriString)

            Glide.with(holder.itemView.context)
                .load(uriString.toUri())
                .centerCrop()
                .into(holder.itemBinding.imageViewItem)

            val visibilityState = if (isSelected) View.VISIBLE else View.GONE
            holder.itemBinding.viewBorder.visibility = visibilityState
            holder.itemBinding.ivCheck.visibility = visibilityState

            holder.itemView.setOnClickListener {
                holder.itemView.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(120)
                    .withEndAction {
                        holder.itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start()
                    }.start()
                if (isSelected) {
                    holder.itemBinding.viewBorder.animate().alpha(0f).setDuration(150).withEndAction {
                        holder.itemBinding.viewBorder.visibility = View.GONE
                    }.start()
                    holder.itemBinding.ivCheck.visibility = View.GONE

                    isSelected = false
                    ImageUploadManager.removeImage(uriString.toUri())
                } else {
                    // Yeni seçim yapıyorsa
                    if (selectedUris.size < 5) {
                        holder.itemBinding.viewBorder.alpha = 0f
                        holder.itemBinding.viewBorder.visibility = View.VISIBLE
                        holder.itemBinding.viewBorder.animate().alpha(1f).setDuration(150).start()
                        holder.itemBinding.ivCheck.visibility = View.VISIBLE

                        isSelected = true
                        ImageUploadManager.addImage(uriString.toUri())
                    } else {
                        UiMessageManager.emitMessage(
                            UiMessageState.Error("En fazla 5 fotoğraf seçebilirsiniz.")
                        )
                    }
                }
            }
        }

        fun appendImages(newImages: List<String>) {

            if (newImages.isEmpty()) return

            val startPosition = images.size

            images = images + newImages

            notifyItemRangeInserted(
                startPosition,
                newImages.size
            )
        }

        override fun getItemCount(): Int = images.size
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}