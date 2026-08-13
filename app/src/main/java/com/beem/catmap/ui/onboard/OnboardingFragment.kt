package com.beem.catmap.ui.onboarding

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentOnboardingBinding
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.onboard.OnboardingPage

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val activeWidthDp = 24
    private val inactiveWidthDp = 8
    private val heightDp = 6
    private val marginDp = 4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = listOf(
            OnboardingPage(
                title = "CatMap'e Hoş Geldin!",
                description = "Çevrendeki sokak kedilerini haritada keşfet ve onların dünyasına adım at.",
                animationResId1 = R.raw.map,
                animationResId2 = R.raw.cat
            ),
            OnboardingPage(
                title = "Kedileri Paylaş",
                description = "Gördüğün sevimli dostlarımızın fotoğraflarını çekip haritaya ekleyerek topluluğa katkıda bulun.",
                animationResId1 = R.raw.camera,
                animationResId2 = R.raw.loadcat
            ),
            OnboardingPage(
                title = "İletişime Geç",
                description = "Diğer kedi severlerle mesajlaş, gönderilere yorum yap ve birlikte destek ol.",
                animationResId1 = R.raw.like,
                animationResId2 = R.raw.chatmessage
            )
        )

        binding.viewPager.adapter = OnboardingAdapter(pages)

        setupIndicators(pages.size)
        setCurrentIndicator(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                binding.btnNext.text = if (position == pages.lastIndex) "Başla" else "İlerle"
            }
        })

        binding.btnNext.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition < pages.lastIndex) {
                binding.viewPager.currentItem = currentPosition + 1
            } else {
                completeOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun setupIndicators(count: Int) {
        binding.layoutIndicators.removeAllViews()
        val context = requireContext()

        for (i in 0 until count) {
            val view = View(context)
            val params = LinearLayout.LayoutParams(
                dpToPx(inactiveWidthDp),
                dpToPx(heightDp)
            ).apply {
                setMargins(dpToPx(marginDp), 0, dpToPx(marginDp), 0)
            }
            view.layoutParams = params
            view.background = createCapsuleDrawable(false)
            binding.layoutIndicators.addView(view)
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = binding.layoutIndicators.childCount
        for (i in 0 until childCount) {
            val view = binding.layoutIndicators.getChildAt(i)
            val isSelected = i == index

            val targetWidth = dpToPx(if (isSelected) activeWidthDp else inactiveWidthDp)
            val currentWidth = view.width.takeIf { it > 0 } ?: dpToPx(if (isSelected) activeWidthDp else inactiveWidthDp)

            // Genişlik değişimi için animasyon
            val anim = ValueAnimator.ofInt(currentWidth, targetWidth)
            anim.addUpdateListener { valueAnimator ->
                val params = view.layoutParams
                params.width = valueAnimator.animatedValue as Int
                view.layoutParams = params
            }
            anim.duration = 200
            anim.start()

            view.background = createCapsuleDrawable(isSelected)
        }
    }

    private fun createCapsuleDrawable(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(heightDp).toFloat()
            if (isSelected) {
                setColor(ContextCompat.getColor(requireContext(), R.color.catmap_primary))
            } else {
                setColor(ContextCompat.getColor(requireContext(),R.color.soft_gray))
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun completeOnboarding() {
        SmartNavigationEngine.navigateTo(Screen.MAP)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}