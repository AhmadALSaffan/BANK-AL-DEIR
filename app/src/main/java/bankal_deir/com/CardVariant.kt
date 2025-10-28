package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import bankal_deir.com.databinding.ActivityCardVariantBinding

class CardVariant : AppCompatActivity() {
    private lateinit var adapter: CardVariantAdapter
    private var cardType: String? = null
    private lateinit var binding: ActivityCardVariantBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardVariantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cardType = intent.getStringExtra("cardtype")?.lowercase()?.trim()


        setupRecyclerView()

        binding.btnProceedToDetails.setOnClickListener {
            val selectedVariant = adapter.getSelectedVariant()
            if (selectedVariant == null) {
                Toast.makeText(this, "Please select a card variant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullCardName = createFullCardName(selectedVariant.variantName)


            val intent = Intent(this, finalDetailsNewCard::class.java).apply {
                putExtra("cardtype", cardType)
                putExtra("variant", selectedVariant.variantName)
                putExtra("fullcardname", fullCardName)
                putExtra("fees", selectedVariant.fees)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val variants = getVariantsForCardType(cardType ?: "")


        if (variants.isEmpty()) {
            Toast.makeText(this, "No variants available for $cardType", Toast.LENGTH_LONG).show()
            return
        }

        adapter = CardVariantAdapter(variants) { selectedVariant ->
            Toast.makeText(this, "Selected: ${selectedVariant.variantName}  $${selectedVariant.fees}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerCardVariants.apply {
            layoutManager = GridLayoutManager(this@CardVariant, 2)
            adapter = this@CardVariant.adapter
        }
    }

    private fun getVariantsForCardType(type: String): List<CardVariantModel> {
        return when (type.lowercase()) {
            "visa" -> listOf(
                CardVariantModel("Visa Classic", "Standard benefits", R.drawable.visaclassic, 25),
                CardVariantModel("Visa Gold", "Enhanced rewards", R.drawable.visagold, 60),
                CardVariantModel("Visa Signature", "Premium benefits", R.drawable.visasignature, 150),
                CardVariantModel("Visa Signature SY", "Syria edition", R.drawable.visasignaturesy, 200),
                CardVariantModel("Visa Travel", "Travel perks", R.drawable.visatravel, 40)
            )
            "mastercard" -> listOf(
                CardVariantModel("Mastercard Classic", "Standard benefits", R.drawable.mastercardclassic, 25),
                CardVariantModel("Mastercard Platinum", "Premium rewards", R.drawable.mastercardplatinum, 60)
            )
            "discover" -> listOf(
                CardVariantModel("Discover Regular", "Standard features", R.drawable.discoverregular, 15),
                CardVariantModel("Discover Secured", "Build credit", R.drawable.discoversecured, 60)
            )
            "fatora" -> listOf(
                CardVariantModel("Fatora Digital", "Digital payments", R.drawable.fatoradigital, 0),
                CardVariantModel("Fatora Classic", "Standard features", R.drawable.fatoraclassic, 0),
                CardVariantModel("Fatora Cash Back", "Earn cash back", R.drawable.fatoracashback, 10)
            )
            else -> emptyList()
        }
    }

    private fun createFullCardName(variantName: String): String {
        return when (variantName.lowercase()) {

            "visa classic" -> "visaclassic"
            "visa gold" -> "visagold"
            "visa signature" -> "visasignature"
            "visa signature sy" -> "visasignaturesy"
            "visa travel" -> "visatravel"

            "mastercard classic" -> "mastercardclassic"
            "mastercard platinum" -> "mastercardplatinum"


            "discover regular" -> "discoverregular"
            "discover secured" -> "discoversecured"

            "fatora digital" -> "fatoradigital"
            "fatora classic" -> "fatoraclassic"
            "fatora cash back" -> "fatoracashback"

            else -> variantName.lowercase().replace(" ", "")
        }
    }
}
