package bankal_deir.com.pinPage

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import bankal_deir.com.R
import com.google.android.material.button.MaterialButton

/**
 * Touch feedback for the PIN keypad.
 *
 * The keys carry no ripple and no colour state: everything the user feels is motion.
 * A key sinks under the finger while a soft halo fades in behind it, then springs back
 * on release. The dots bounce as they fill and shake as a group when the PIN is wrong.
 */
object PinFeedback {

    private const val PRESS_SCALE = 0.90f

    /**
     * Sink + halo on press, spring back on release. Returns false so the view's own
     * OnClickListener still fires.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun attachKeyPress(key: MaterialButton) {
        val haloColor = ContextCompat.getColor(key.context, R.color.md_theme_surfaceContainerHigh)
        val idle = Color.TRANSPARENT

        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    key.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    key.animate()
                        .scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                        .setDuration(90)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                    key.tintTo(idle, haloColor, 120)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    key.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(280)
                        .setInterpolator(OvershootInterpolator(2.5f))
                        .start()
                    key.tintTo(haloColor, idle, 220)
                }
            }
            false
        }
    }

    private fun MaterialButton.tintTo(from: Int, to: Int, duration: Long) {
        ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            this.duration = duration
            addUpdateListener { backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int) }
            start()
        }
    }

    /** A digit landed: the dot pops in past its size and settles. */
    fun fillDot(dot: View) {
        dot.setBackgroundResource(R.drawable.pin_circle_filled)
        dot.scaleX = 0.4f
        dot.scaleY = 0.4f
        dot.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(3.5f))
            .start()
    }

    /** Backspace: the dot deflates before emptying. */
    fun clearDot(dot: View) {
        dot.animate()
            .scaleX(0.6f).scaleY(0.6f)
            .setDuration(90)
            .withEndAction {
                dot.setBackgroundResource(R.drawable.pin_circle_empty)
                dot.animate().scaleX(1f).scaleY(1f).setDuration(160)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }
            .start()
    }

    fun resetDot(dot: View) {
        dot.animate().cancel()
        dot.scaleX = 1f
        dot.scaleY = 1f
        dot.setBackgroundResource(R.drawable.pin_circle_empty)
    }

    /** Wrong PIN: the row turns red, shakes once, then empties. */
    fun rejectDots(dots: List<View>, onFinished: () -> Unit) {
        dots.forEach { it.setBackgroundResource(R.drawable.pin_circle_error) }
        val row = dots.first().parent as View
        row.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        ObjectAnimator.ofFloat(
            row, View.TRANSLATION_X,
            0f, -18f, 18f, -14f, 14f, -7f, 7f, 0f
        ).apply {
            duration = 450
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dots.forEach { resetDot(it) }
                    onFinished()
                }
            })
            start()
        }
    }

    /** Correct PIN: one confident pulse before the screen hands off. */
    fun acceptDots(dots: List<View>, onFinished: () -> Unit) {
        dots.forEachIndexed { index, dot ->
            dot.animate()
                .scaleX(1.25f).scaleY(1.25f)
                .setStartDelay(index * 45L)
                .setDuration(140)
                .withEndAction {
                    dot.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                .start()
        }
        dots.first().postDelayed({ onFinished() }, 400)
    }
}
