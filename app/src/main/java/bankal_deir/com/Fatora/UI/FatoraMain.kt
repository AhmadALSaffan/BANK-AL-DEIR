package bankal_deir.com.Fatora.UI

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityFatoraMainBinding

class FatoraMain : AppCompatActivity() {
    private lateinit var binding: ActivityFatoraMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFatoraMainBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)
        bankal_deir.com.NavHelper.setup(this, "bills")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnPay.setOnClickListener {
            val intent = Intent(this, FatoraPayActivity::class.java)
            startActivity(intent)
        }
        binding.btnBack.setOnClickListener { finish() }
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