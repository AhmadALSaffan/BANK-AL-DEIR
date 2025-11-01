package bankal_deir.com

import bankal_deir.com.ViewPagerAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.databinding.ActivityMainBinding
import bankal_deir.com.pinPage.PinPage
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var mAuth: FirebaseAuth?=null
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
        val adapter = ViewPagerAdapter(images)
        binding.viewPager2.adapter = adapter
        val tabTitles = listOf("1", "2", "3")
        val tabLayout = binding.tabLayout
        val viewPager2 = binding.viewPager2
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
        binding.btnGetStarted.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (mAuth?.currentUser != null){
            val intentToMain = Intent(this, PinPage::class.java)
            startActivity(intentToMain)
            finish()
        }
    }
}