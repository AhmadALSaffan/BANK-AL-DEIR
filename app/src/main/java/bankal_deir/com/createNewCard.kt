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
    val visaFees = 25
    val mastercardFees = 25
    val discoverFees = 15
    val fatoraFees = 0
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
            }

            if (selectedType=="mastercard"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
            }

            if (selectedType=="discover"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
            }

            if (selectedType=="fatora"){
                val cIntent = Intent(this@createNewCard, CardVariant::class.java).apply {
                    putExtra("cardtype", selectedType)

                }
                startActivity(cIntent)
            }



            if (selectedType==null) {
                Toast.makeText(this, "Please select a card type first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

        }
    }
    fun showVisaDialog() {
        val visaDet = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(R.layout.visa_details)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val btnClose = visaDet.findViewById<Button>(R.id.btnVisaClose)
        btnClose.setOnClickListener {
            visaDet.dismiss()
        }
        visaDet.show()
    }

    fun showMasterDialog() {
        val masterDet = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(R.layout.mastercard_dialog)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val btnClose = masterDet.findViewById<Button>(R.id.btnmasterClose)
        btnClose.setOnClickListener {
            masterDet.dismiss()
        }
        masterDet.show()
    }

    fun showDiscDialog() {
        val discDet = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(R.layout.discover_dialog)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val btnClose = discDet.findViewById<Button>(R.id.btnDiscoverClose)
        btnClose.setOnClickListener {
            discDet.dismiss()
        }
        discDet.show()
    }

    fun showFatoraDialog() {
        val fatoraDet = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(R.layout.fatora_dialog)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val btnClose = fatoraDet.findViewById<Button>(R.id.btnFatoraClose)
        btnClose.setOnClickListener {
            fatoraDet.dismiss()
        }
        fatoraDet.show()
    }

}