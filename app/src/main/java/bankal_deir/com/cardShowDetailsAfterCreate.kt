package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.databinding.ActivityCardShowDetailsAfterCreateBinding

class cardShowDetailsAfterCreate : AppCompatActivity() {
    private lateinit var binding: ActivityCardShowDetailsAfterCreateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardShowDetailsAfterCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        readCard()
        binding.btnClose.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
            finish()
        }
    }
    fun readCard(){
        val cardNumber = intent.getStringExtra("cardnumber")
        val cardExp = intent.getStringExtra("cardexp")
        val cardCvv = intent.getStringExtra("cardcvv")
        val cardHolder = intent.getStringExtra("cardholder")
        binding.txtCardNumber.text = cardNumber
        binding.txtExp.text = cardExp
        binding.txtCVV.text = cardCvv
        binding.txtCardHolder.text = cardHolder
    }
}