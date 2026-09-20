package bankal_deir.com.common
import bankal_deir.com.R

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import bankal_deir.com.Fatora.UI.FatoraMain
import bankal_deir.com.cards.cards
import bankal_deir.com.home.MainPage
import bankal_deir.com.profile.profilePage

/**
 * Shared floating bottom nav. Press an icon to raise a circle under your finger; drag it
 * across the icons and release — the circle springs to the chosen icon and the screen
 * crossfades there. [current] is "home" | "cards" | "bills" | "profile".
 */
object NavHelper {

    private val keys = listOf("home", "cards", "bills", "profile")

    fun setup(activity: Activity, current: String) {
        val row = activity.findViewById<LinearLayout>(R.id.navItems) ?: return
        val indicator = activity.findViewById<View>(R.id.navIndicator) ?: return
        val items = listOf(
            activity.findViewById<LinearLayout>(R.id.navHome),
            activity.findViewById<LinearLayout>(R.id.navCards),
            activity.findViewById<LinearLayout>(R.id.navBills),
            activity.findViewById<LinearLayout>(R.id.navProfile)
        )
        val currentIndex = keys.indexOf(current).coerceAtLeast(0)

        items.forEach { it.isClickable = false; it.isFocusable = false }
        restState(activity, items, currentIndex)

        fun xFor(i: Int) = (items[i].left + items[i].width / 2f) - indicator.width / 2f
        fun indexAt(x: Float): Int {
            for (i in items.indices) if (x >= items[i].left && x <= items[i].right) return i
            return if (x < items.first().left) 0 else items.size - 1
        }

        var active = currentIndex

        fun activeState(i: Int) {
            for (k in items.indices) {
                tint(activity, items[k], if (k == i) R.color.porcelain_bright else R.color.md_theme_onSurfaceVariant, k == i)
            }
        }

        fun showAt(i: Int) {
            active = i
            indicator.translationX = xFor(i)
            indicator.scaleX = 0.6f; indicator.scaleY = 0.6f; indicator.alpha = 0f
            indicator.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start()
            activeState(i)
        }

        fun moveTo(i: Int) {
            if (i == active) return
            active = i
            indicator.animate().translationX(xFor(i))
                .setInterpolator(DecelerateInterpolator()).setDuration(150).start()
            activeState(i)
        }

        fun cancel() {
            indicator.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(160).start()
            restState(activity, items, currentIndex)
        }

        fun release(i: Int) {
            indicator.animate().translationX(xFor(i))
                .setInterpolator(OvershootInterpolator(2.2f)).setDuration(260)
                .withEndAction { if (i == currentIndex) cancel() else navigate(activity, i) }
                .start()
        }

        row.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { showAt(indexAt(ev.x)); true }
                MotionEvent.ACTION_MOVE -> { moveTo(indexAt(ev.x)); true }
                MotionEvent.ACTION_UP -> { release(active); true }
                MotionEvent.ACTION_CANCEL -> { cancel(); true }
                else -> false
            }
        }

        // Pages are reused (REORDER_TO_FRONT) without re-running onCreate, so re-apply the
        // correct highlight every time this page comes forward — otherwise it keeps the
        // active state it was left in when you last dragged away from it.
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    active = currentIndex
                    indicator.animate().cancel()
                    indicator.alpha = 0f
                    indicator.scaleX = 1f
                    indicator.scaleY = 1f
                    restState(activity, items, currentIndex)
                }
            }
        )
    }

    private fun restState(activity: Activity, items: List<LinearLayout>, current: Int) {
        for (i in items.indices) {
            tint(activity, items[i], if (i == current) R.color.md_theme_primary else R.color.md_theme_onSurfaceVariant, i == current)
        }
    }

    private fun tint(activity: Activity, item: LinearLayout, colorRes: Int, bold: Boolean) {
        val icon = item.getChildAt(0) as? ImageView
        val label = item.getChildAt(1) as? TextView
        val c = ContextCompat.getColor(activity, colorRes)
        icon?.setColorFilter(c)
        label?.setTextColor(c)
        label?.setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun navigate(activity: Activity, index: Int) {
        when (keys[index]) {
            "home" -> go(activity, MainPage::class.java)
            "cards" -> go(activity, cards::class.java)
            "bills" -> go(activity, FatoraMain::class.java)
            "profile" -> go(activity, profilePage::class.java)
        }
    }

    private fun go(activity: Activity, target: Class<*>) {
        if (activity.javaClass == target) return
        activity.startActivity(
            Intent(activity, target)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        activity.overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out)
    }
}
