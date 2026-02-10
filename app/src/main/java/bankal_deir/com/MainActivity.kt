package bankal_deir.com

import bankal_deir.com.ViewPagerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.databinding.ActivityMainBinding
import bankal_deir.com.pinPage.PinPage
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var mAuth: FirebaseAuth?=null
    private lateinit var ViewPagerAdapter: ViewPagerAdapter
    private val images = listOf(
        R.drawable.card1,
        R.drawable.card2,
        R.drawable.card3
    )
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
                R.drawable.card1,
                "Welcome to Banking App",
                "Manage your finances easily and securely with our advanced banking solutions"
            ),
            OnboardingItem(
                R.drawable.card2,
                "Fast Transactions",
                "Send and receive money instantly with just a few taps"
            ),
            OnboardingItem(
                R.drawable.card3,
                "Secure & Safe",
                "Your money is protected with bank-level security and encryption"
            )
        )

        ViewPagerAdapter = ViewPagerAdapter(onboardingItems)
        binding.viewPager2.adapter = ViewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { _, _ -> }.attach()

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position == onboardingItems.size - 1) {
                    binding.btnGetStarted.text = "GET STARTED"
                    binding.tvSkip.visibility = View.GONE
                } else {
                    binding.btnGetStarted.text = "NEXT"
                    binding.tvSkip.visibility = View.VISIBLE
                }
            }
        })
        binding.viewPager2.setPageTransformer { page, position ->
            page.apply {
                val pageWidth = width
                when {
                    position < -1 -> {
                        alpha = 0f
                    }
                    position <= 1 -> {
                        alpha = 0.25f + (1 - Math.abs(position))

                        val scaleFactor = 0.85f.coerceAtLeast(1 - Math.abs(position))
                        scaleX = scaleFactor
                        scaleY = scaleFactor

                        translationX = -position * pageWidth / 2
                    }
                    else -> {
                        alpha = 0f
                    }
                }
            }
        }
    }

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
}