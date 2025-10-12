package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var mAuth: FirebaseAuth?=null
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
        binding.btnGetStarted.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (mAuth?.currentUser != null){
            val intentToMain = Intent(this, MainPage::class.java)
            startActivity(intentToMain)
            finish()
        }
    }
}