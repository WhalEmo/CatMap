package com.beem.catmap.ui.onboarding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.RenderMode
import com.beem.catmap.databinding.ItemOnboardingBinding
import com.beem.catmap.ui.onboard.OnboardingPage

class OnboardingAdapter(
    private val items: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingPage) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description

            binding.animationView.setRenderMode(RenderMode.HARDWARE)
            binding.animationView2.setRenderMode(RenderMode.HARDWARE)

            binding.animationView.setAnimation(item.animationResId1)
            binding.animationView.playAnimation()

            binding.animationView2.setAnimation(item.animationResId2)
            binding.animationView2.playAnimation()
        }

    }
    override fun onViewDetachedFromWindow(holder: OnboardingViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.animationView.pauseAnimation()
        holder.binding.animationView2.pauseAnimation()
    }

    override fun onViewAttachedToWindow(holder: OnboardingViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.animationView.resumeAnimation()
        holder.binding.animationView2.resumeAnimation()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size


}