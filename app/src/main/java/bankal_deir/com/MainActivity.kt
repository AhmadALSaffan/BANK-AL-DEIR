package bankal_deir.com

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.databinding.ActivityMainBinding
import bankal_deir.com.pinPage.PinPage
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var mAuth: FirebaseAuth? = null
    private lateinit var ViewPagerAdapter: ViewPagerAdapter
    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupOnboarding()
        setupClickListeners()
        hideSystemBars()

    }

    override fun onStart() {
        super.onStart()
        if (mAuth?.currentUser != null){
            val intentToMain = Intent(this, PinPage::class.java)
            startActivity(intentToMain)
            finish()
        }
    }

    private fun setupOnboarding() {
        val onboardingItems = listOf(
            OnboardingItem(
                R.drawable.onbording1,
                "Your account, on your phone",
                "Check your balance, follow every transaction and manage your cards without going to a branch."
            ),
            OnboardingItem(
                R.drawable.onbording2,
                "Transfers that arrive in seconds",
                "Send money to any Bank Al-Deir account, pay your bills and top up your phone."
            ),
            OnboardingItem(
                R.drawable.onbording3,
                "Locked down by default",
                "A PIN on every session, encrypted connections, and a notification for every payment."
            )
        )

        ViewPagerAdapter = ViewPagerAdapter(onboardingItems)
        binding.viewPager2.adapter = ViewPagerAdapter

        setupDots(onboardingItems.size)

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectDot(position)
                playPageAnimation(position)

                val isLast = position == onboardingItems.size - 1
                binding.btnGetStarted.text = if (isLast) "Get started" else "Continue"
                binding.tvSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
            }
        })

        // The hero drifts slower than the page it sits on, so the swipe has depth
        // without the page itself doing anything showy.
        binding.viewPager2.setPageTransformer { page, position ->
            page.findViewById<View>(R.id.ivOnboarding).translationX = -position * page.width / 4f
        }
    }

    /** Replays the staggered entrance on whichever page just settled. */
    private fun playPageAnimation(position: Int) {
        val pages = binding.viewPager2.getChildAt(0) as? RecyclerView ?: return
        pages.post {
            val holder = pages.findViewHolderForAdapterPosition(position)
            (holder as? ViewPagerAdapter.ViewPagerViewHolder)?.playEnterAnimation()
        }
    }

    private fun setupDots(count: Int) {
        binding.dotsContainer.removeAllViews()
        dots.clear()
        repeat(count) { index ->
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dpToPx(if (index == 0) 20 else 6), dpToPx(6))
            params.marginEnd = dpToPx(6)
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (index == 0) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot
            )
            binding.dotsContainer.addView(dot)
            dots.add(dot)
        }
    }

    private fun selectDot(selected: Int) {
        dots.forEachIndexed { index, dot ->
            val isSelected = index == selected
            dot.background = ContextCompat.getDrawable(
                this,
                if (isSelected) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot
            )
            animateDotWidth(dot, dpToPx(if (isSelected) 20 else 6))
        }
    }

    private fun animateDotWidth(dot: View, targetWidth: Int) {
        val current = dot.layoutParams.width
        if (current == targetWidth) return
        ValueAnimator.ofInt(current, targetWidth).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                dot.layoutParams = dot.layoutParams.also { it.width = animator.animatedValue as Int }
            }
            start()
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setupClickListeners() {
        binding.btnGetStarted.setOnClickListener {
            if (binding.viewPager2.currentItem < ViewPagerAdapter.itemCount - 1) {

                binding.viewPager2.currentItem = binding.viewPager2.currentItem + 1
            } else {
                navigateToMainApp()
            }
        }

        binding.tvSkip.setOnClickListener {
            navigateToMainApp()
        }
    }

    private fun navigateToMainApp() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("onboarding_completed", true).apply()

        val intentToLogin = Intent(this, LoginPage::class.java)
        startActivity(intentToLogin)
        finish()
    }
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
