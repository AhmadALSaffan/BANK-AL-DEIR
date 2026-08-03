package bankal_deir.com

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ViewPagerAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {

    inner class ViewPagerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.ivOnboarding)
        private val title: TextView = view.findViewById(R.id.tvTitle)
        private val description: TextView = view.findViewById(R.id.tvDescription)

        fun bind(item: OnboardingItem) {
            image.setImageResource(item.image)
            title.text = item.title
            description.text = item.description
        }

        /** Hero settles first, then the copy rises under it. */
        fun playEnterAnimation() {
            enter(image, delay = 0, rise = 20f, withScale = true)
            enter(title, delay = 90, rise = 16f)
            enter(description, delay = 150, rise = 16f)
        }

        private fun enter(view: View, delay: Long, rise: Float, withScale: Boolean = false) {
            val density = view.resources.displayMetrics.density
            view.animate().cancel()
            view.alpha = 0f
            view.translationY = rise * density
            if (withScale) {
                view.scaleX = 0.96f
                view.scaleY = 0.96f
            }
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .apply { if (withScale) scaleX(1f).scaleY(1f) }
                .setStartDelay(delay)
                .setDuration(420)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return ViewPagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
