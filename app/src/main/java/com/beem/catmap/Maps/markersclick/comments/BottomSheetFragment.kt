package com.beem.catmap.YorumYanit

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.Maps.markersclick.comments.CommentViewModel
import com.beem.catmap.R
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.ui.navigation.NavigationHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: CommentViewModel by viewModels()
    private lateinit var userRepository: UserRepository
    private val begeniYoneticisi = Begeni_Kod_Yoneticisi_Yorum()

    private lateinit var yorumlarRecyclerView: RecyclerView
    private lateinit var yorumLayout: View
    private lateinit var yanitLayout: View
    private lateinit var carpiLayout: View
    private lateinit var iptalButton: View
    private lateinit var kimeYanitText: TextView
    private lateinit var yntEditText: EditText
    private lateinit var yorumEditText: EditText
    private lateinit var yorumGonderButton: View
    private lateinit var yanitGonderButton: View
    private lateinit var bosYorumText: TextView

    private lateinit var yorumAdapter: Yorum_Adapter
    private val yorumlar = ArrayList<Yorum_Model>()

    private var catId: String = ""
    private var hedefYorumIndeks: Int = -1

    override fun onDestroyView() {
        super.onDestroyView()
        if (::yorumAdapter.isInitialized) {
            yorumAdapter.durdurZamanlayici()
        }
        yorumlar.forEach { yorum ->
            yorum.yanitAdapter?.durdurZamanlayici()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catId = arguments?.getString(ARG_CAT_ID) ?: ""
        userRepository = UserRepository.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.yorum_gosterme, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupTextWatchers()
        observeViewModel()

        if (catId.isNotEmpty()) {
            viewModel.initCatId(catId)
        }
    }

    private fun initViews(view: View) {
        yorumlarRecyclerView = view.findViewById(R.id.yorumlarRecyclerView)
        yorumLayout = view.findViewById(R.id.yorumgndrLayout)
        yanitLayout = view.findViewById(R.id.yntgndrLayout)
        carpiLayout = view.findViewById(R.id.carpilayout)
        iptalButton = view.findViewById(R.id.iptalButton)
        kimeYanitText = view.findViewById(R.id.kimeyanit)
        yntEditText = view.findViewById(R.id.yntEditText)
        yorumEditText = view.findViewById(R.id.yorumEditText)
        yorumGonderButton = view.findViewById(R.id.yorumgonder)
        yanitGonderButton = view.findViewById(R.id.yntgonder)
        bosYorumText = view.findViewById(R.id.bosYorumTextView)

        setButtonState(yorumGonderButton, false)
        setButtonState(yanitGonderButton, false)

        iptalButton.setOnClickListener { resetToCommentMode() }
        yorumGonderButton.setOnClickListener { sendComment() }
        yanitGonderButton.setOnClickListener { sendReply() }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        yorumlarRecyclerView.layoutManager = layoutManager

        // 🟢 Yorum_Adapter constructor'ına catId eklendi
        yorumAdapter = Yorum_Adapter(yorumlar, requireContext(), catId)
        yorumlarRecyclerView.adapter = yorumAdapter

        yorumAdapter.setAksiyonListener { kullaniciAdi, yukleyenId, position, ayniButonaMiBasildi ->
            if (ayniButonaMiBasildi) {
                resetToCommentMode()
            } else {
                hedefYorumIndeks = position
                carpiLayout.isVisible = true
                yorumLayout.isVisible = false
                yanitLayout.isVisible = true
                kimeYanitText.text = "@$kullaniciAdi yanıtlanıyor"
                yntEditText.requestFocus()
            }
        }

        yorumAdapter.setKullaniciAdiTiklamaListener { kullaniciID ->
            dismiss()
            NavigationHelper.navigateToProfile(kullaniciID)
        }

        val cachedSet = CacheHelperYorum.loadBegenilenSet(requireContext())
        val begeniMap = CacheHelperYorum.loadBegeniSayilariMap(requireContext())
        yorumAdapter.setBegenilenYorumIDSeti(cachedSet)
        yorumAdapter.setBegeniSayisiMap(begeniMap)

        userRepository.getCurrentUserId()?.let { userId ->
            begeniYoneticisi.KullanicininBegendigiYorumalar(catId, requireContext(), userId, yorumAdapter)
        }

        yorumlarRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreComments()
                    }
                }
            }
        })
    }

    private fun setupTextWatchers() {
        yorumEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setButtonState(yorumGonderButton, !s.toString().trim().isEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        yntEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setButtonState(yanitGonderButton, !s.toString().trim().isEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.comments.collectLatest { list ->
                yorumlar.clear()
                yorumlar.addAll(list)
                yorumAdapter.notifyDataSetChanged()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isEmpty.collectLatest { isEmpty ->
                bosYorumText.isVisible = isEmpty
                yorumlarRecyclerView.isVisible = !isEmpty
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionSuccess.collectLatest {
                yorumEditText.setText("")
                resetToCommentMode()
            }
        }
    }

    private fun sendComment() {
        val text = yorumEditText.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendComment(text)
        }
    }

    private fun sendReply() {
        if (hedefYorumIndeks < 0 || hedefYorumIndeks >= yorumlar.size) return
        val targetComment = yorumlar[hedefYorumIndeks]
        val text = yntEditText.text.toString().trim()

        if (text.isNotEmpty()) {
            viewModel.sendReply(targetComment.yorumID, text) { replyId ->
                val currentUser = userRepository.getCurrentUser()
                val newReply = Yanit_Model(
                    replyId ?: "gecici",
                    currentUser?.kullaniciAdi ?: "",
                    text,
                    null,
                    userRepository.getCurrentUserId()
                )
                targetComment.yanitlar?.add(0, newReply)
                targetComment.isYanitYokMu = false
                yorumAdapter.notifyItemChanged(hedefYorumIndeks)

                yntEditText.setText("")
                resetToCommentMode()
            }
        }
    }

    private fun resetToCommentMode() {
        carpiLayout.isVisible = false
        yanitLayout.isVisible = false
        yorumLayout.isVisible = true
        hedefYorumIndeks = -1
    }

    private fun setButtonState(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.5f
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    companion object {
        const val TAG = "CommentsBottomSheetFragment"
        private const val ARG_CAT_ID = "arg_cat_id"

        fun newInstance(catId: String): CommentsBottomSheetFragment {
            val fragment = CommentsBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_CAT_ID, catId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}