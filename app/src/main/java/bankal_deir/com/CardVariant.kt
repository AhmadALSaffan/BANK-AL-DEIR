package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import bankal_deir.com.databinding.ActivityCardVariantBinding
import com.google.firebase.database.FirebaseDatabase

class CardVariant : AppCompatActivity() {
    private lateinit var adapter: CardVariantAdapter
    private var cardType: String? = null
    private lateinit var binding: ActivityCardVariantBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardVariantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cardType = intent.getStringExtra("cardtype")?.lowercase()?.trim()

        fetchPricesAndSetup()

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

    // Fallback prices used when the database is unreachable
    private val defaultPrices = mapOf(
        "visaclassic"       to 25,
        "visagold"          to 60,
        "visasignature"     to 150,
        "visasignaturesy"   to 200,
        "visatravel"        to 40,
        "mastercardclassic" to 25,
        "mastercardplatinum" to 60,
        "discoverregular"   to 15,
        "discoversecured"   to 60,
        "fatoradigital"     to 0,
        "fatoraclassic"     to 0,
        "fatoracashback"    to 10
    )

    private fun fetchPricesAndSetup() {
        binding.btnProceedToDetails.isEnabled = false

        FirebaseDatabase.getInstance().getReference("CardsPrice")
            .get()
            .addOnSuccessListener { snapshot ->
                val prices = mutableMapOf<String, Int>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.getValue(Long::class.java)?.toInt()
                        ?: child.getValue(Int::class.java)
                        ?: continue
                    prices[key] = value
                }
                setupRecyclerView(if (prices.isNotEmpty()) prices else defaultPrices)
                binding.btnProceedToDetails.isEnabled = true
            }
            .addOnFailureListener { e ->
                Log.e("CardVariant", "Failed to fetch CardsPrice: ${e.message}")
                setupRecyclerView(defaultPrices)
                binding.btnProceedToDetails.isEnabled = true
            }
    }

    private fun setupRecyclerView(prices: Map<String, Int>) {
        val variants = getVariantsForCardType(cardType ?: "", prices)

        if (variants.isEmpty()) {
            Toast.makeText(this, "No variants available for $cardType", Toast.LENGTH_LONG).show()
            return
        }

        adapter = CardVariantAdapter(variants) { selectedVariant ->
            Toast.makeText(this, "Selected: ${selectedVariant.variantName}  $${selectedVariant.fees}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerCardVariants.apply {
            layoutManager = LinearLayoutManager(this@CardVariant)
            adapter = this@CardVariant.adapter
        }
    }

    private fun getVariantsForCardType(type: String, prices: Map<String, Int>): List<CardVariantModel> {
        fun price(key: String) = prices[key] ?: defaultPrices[key] ?: 0
        return when (type.lowercase()) {
            "visa" -> listOf(
                CardVariantModel("Visa Classic",       "Standard benefits",  R.drawable.visaclassic,       price("visaclassic")),
                CardVariantModel("Visa Gold",          "Enhanced rewards",   R.drawable.visagold,          price("visagold")),
                CardVariantModel("Visa Signature",     "Premium benefits",   R.drawable.visasignature,     price("visasignature")),
                CardVariantModel("Visa Signature SY",  "Syria edition",      R.drawable.visasignaturesy,   price("visasignaturesy")),
                CardVariantModel("Visa Travel",        "Travel perks",       R.drawable.visatravel,        price("visatravel"))
            )
            "mastercard" -> listOf(
                CardVariantModel("Mastercard Classic",   "Standard benefits", R.drawable.mastercardclassic,   price("mastercardclassic")),
                CardVariantModel("Mastercard Platinum",  "Premium rewards",   R.drawable.mastercardplatinum,  price("mastercardplatinum"))
            )
            "discover" -> listOf(
                CardVariantModel("Discover Regular", "Standard features", R.drawable.discoverregular, price("discoverregular")),
                CardVariantModel("Discover Secured", "Build credit",      R.drawable.discoversecured, price("discoversecured"))
            )
            "fatora" -> listOf(
                CardVariantModel("Fatora Digital",    "Digital payments", R.drawable.fatoradigital,  price("fatoradigital")),
                CardVariantModel("Fatora Classic",    "Standard features", R.drawable.fatoraclassic, price("fatoraclassic")),
                CardVariantModel("Fatora Cash Back",  "Earn cash back",   R.drawable.fatoracashback, price("fatoracashback"))
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
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
