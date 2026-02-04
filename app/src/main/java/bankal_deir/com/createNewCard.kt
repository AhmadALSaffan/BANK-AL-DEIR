package bankal_deir.com

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import bankal_deir.com.databinding.ActivityCreateNewCardBinding
import bankal_deir.com.databinding.VisaDetailsBinding


class createNewCard : AppCompatActivity() {
    private lateinit var binding: ActivityCreateNewCardBinding
     var selectedType: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding  = ActivityCreateNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnVisa.setOnClickListener {
            if (selectedType==null) {
                binding.linVisa.setBackgroundResource(R.drawable.selected_border)
                selectedType = "visa"
            }

        }
        binding.btnMasterCard.setOnClickListener {
            if (selectedType==null) {
                binding.linMasterCard.setBackgroundResource(R.drawable.selected_border)
                selectedType = "mastercard"
            }

        }
        binding.btnDiscover.setOnClickListener {
            if (selectedType==null) {
                binding.linDiscover.setBackgroundResource(R.drawable.selected_border)
                selectedType = "discover"
            }

        }
        binding.btnFatora.setOnClickListener {
            if (selectedType == null) {
                binding.linFatora.setBackgroundResource(R.drawable.selected_border)
                selectedType = "fatora"
            }

        }
        binding.btnSelectedCard.setOnClickListener {

            if (selectedType=="visa"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            if (selectedType=="mastercard"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            if (selectedType=="discover"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            if (selectedType=="fatora"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }



            if (selectedType==null) {
                Toast.makeText(this, "Please select a card type first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

        }
    }


}