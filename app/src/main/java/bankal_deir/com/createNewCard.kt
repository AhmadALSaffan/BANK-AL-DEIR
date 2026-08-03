package bankal_deir.com

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import bankal_deir.com.databinding.ActivityCreateNewCardBinding
import com.google.firebase.database.FirebaseDatabase

class createNewCard : AppCompatActivity() {
    private lateinit var binding: ActivityCreateNewCardBinding

    private val types = listOf("visa", "mastercard", "discover", "fatora")
    private val typeLabels = mapOf(
        "visa" to "Visa", "mastercard" to "Mastercard",
        "discover" to "Discover", "fatora" to "Fatora"
    )

    private var prices: Map<String, Int> = emptyMap()
    private var selectedType = "visa"
    private var currentVariants: List<CardVariantModel> = emptyList()
    private var selectedVariant: CardVariantModel? = null
    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateNewCardBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnGetCard.setOnClickListener {
            val v = selectedVariant ?: return@setOnClickListener
            startActivity(Intent(this, finalDetailsNewCard::class.java).apply {
                putExtra("cardtype", v.type)
                putExtra("variant", v.variantName)
                putExtra("fullcardname", v.fullName)
                putExtra("fees", v.fees)
            })
        }

        // Swipe the hero card left/right to move through variants of the selected type.
        val heroGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                if (abs(dx) > 60 && abs(dx) > abs(e2.y - e1.y)) {
                    if (dx < 0) animateToVariant(selectedIndex + 1) else animateToVariant(selectedIndex - 1)
                    return true
                }
                return false
            }
        })
        binding.heroFrame.setOnTouchListener { _, ev -> heroGesture.onTouchEvent(ev) }

        // Render immediately with fallback prices, then refresh from Firebase.
        prices = defaultPrices
        buildTabs()
        fetchPrices()
    }

    private val defaultPrices = mapOf(
        "visaclassic" to 25, "visagold" to 60, "visasignature" to 150,
        "visasignaturesy" to 200, "visatravel" to 40,
        "mastercardclassic" to 25, "mastercardplatinum" to 60,
        "discoverregular" to 15, "discoversecured" to 60,
        "fatoradigital" to 0, "fatoraclassic" to 0, "fatoracashback" to 10
    )

    private fun fetchPrices() {
        FirebaseDatabase.getInstance().getReference("CardsPrice").get()
            .addOnSuccessListener { snap ->
                val map = mutableMapOf<String, Int>()
                for (c in snap.children) {
                    val k = c.key ?: continue
                    val v = c.getValue(Long::class.java)?.toInt()
                        ?: c.getValue(Int::class.java) ?: continue
                    map[k] = v
                }
                if (map.isNotEmpty()) {
                    prices = map
                    selectType(selectedType) // refresh fees with live prices
                }
            }
            .addOnFailureListener {
                Log.e("createNewCard", "prices: ${it.message}") // keep fallback prices
            }
    }

    private fun buildTabs() {
        binding.typeTabs.removeAllViews()
        types.forEach { t ->
            binding.typeTabs.addView(makeChip(typeLabels[t] ?: t) { selectType(t) })
        }
        selectType(selectedType)
    }

    private fun selectType(type: String) {
        selectedType = type
        for (i in types.indices) {
            styleChip(binding.typeTabs.getChildAt(i) as TextView, types[i] == type)
        }
        currentVariants = variantsFor(type)
        binding.variantTabs.removeAllViews()
        currentVariants.forEachIndexed { i, _ ->
            binding.variantTabs.addView(makeChip(currentVariants[i].variantName) { animateToVariant(i) })
        }
        if (currentVariants.isNotEmpty()) selectVariant(0)
    }

    private data class Feature(val icon: Int, val label: String, val colored: Boolean)

    private fun featuresFor(v: CardVariantModel): List<Feature> {
        val googlePay = Feature(R.drawable.google_pay, "Google Pay", true)
        val samsungPay = Feature(R.drawable.samsung_pay, "Samsung Pay", true)
        val contactless = Feature(R.drawable.ic_contactless, "Contactless", false)
        val languages = Feature(R.drawable.ic_language, "Arabic & English", false)
        val secure3d = Feature(R.drawable.defence, "Secure 3-D", false)
        val local = Feature(R.drawable.ic_language, "Local network", false)
        val list = mutableListOf<Feature>()
        when (v.type) {
            "visa", "mastercard" -> list.addAll(listOf(googlePay, samsungPay, contactless, languages, secure3d))
            "discover" -> list.addAll(listOf(googlePay, contactless, languages))     // Google Pay only
            "fatora" -> list.addAll(listOf(local, contactless, languages))           // local card
            else -> list.addAll(listOf(contactless, languages))
        }
        loungeFor(v)?.let { list.add(1, Feature(R.drawable.ic_lounge, "${it.count} Lounges", false)) }
        return list
    }

    private fun buildFeatureChips(v: CardVariantModel) {
        binding.featureChips.removeAllViews()
        featuresFor(v).forEach { f ->
            val tile = LinearLayout(this)
            tile.orientation = LinearLayout.VERTICAL
            tile.gravity = android.view.Gravity.CENTER
            tile.setBackgroundResource(R.drawable.chip_feature)
            tile.minimumWidth = dp(100)
            tile.setPadding(dp(14), dp(16), dp(14), dp(16))
            val tlp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tlp.marginEnd = dp(10)
            tile.layoutParams = tlp

            val icon = ImageView(this)
            icon.layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            icon.scaleType = ImageView.ScaleType.FIT_CENTER
            icon.setImageResource(f.icon)
            if (!f.colored) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.md_theme_primary))
            }

            val label = TextView(this)
            label.text = f.label
            label.setTextSize(12f)
            label.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface))
            val llp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            llp.topMargin = dp(10)
            label.layoutParams = llp

            tile.addView(icon)
            tile.addView(label)
            binding.featureChips.addView(tile)
        }
    }

    /** Sets all the hero content for [index] without animating. */
    private fun applyVariant(index: Int) {
        if (index !in currentVariants.indices) return
        selectedIndex = index
        val v = currentVariants[index]
        selectedVariant = v
        for (i in currentVariants.indices) {
            styleChip(binding.variantTabs.getChildAt(i) as TextView, i == index)
        }
        binding.imgHeroCard.setImageResource(v.imageResource)
        binding.txtHeroTitle.text = v.variantName
        binding.txtHeroFee.text = if (v.fees > 0) "$${v.fees}" else "FREE"
        binding.txtHeroDesc.text = descFor(v)
        buildFeatureChips(v)
        updateLoungeRow(v)
    }

    private data class Lounge(val count: String, val program: String, val logo: Int)

    private fun loungeFor(v: CardVariantModel): Lounge? = when (v.fullName) {
        "visagold" -> Lounge("25", "Access via Visa Airport Companion", R.drawable.visaairport)
        "visasignature", "visasignaturesy" -> Lounge("1000+", "Access via Visa Airport Companion", R.drawable.visaairport)
        "mastercardplatinum" -> Lounge("1000+", "Access via Mastercard DragonPass", R.drawable.dragonpass)
        else -> null
    }

    private fun updateLoungeRow(v: CardVariantModel) {
        val lounge = loungeFor(v)
        if (lounge == null) {
            binding.loungeRow.visibility = View.GONE
            return
        }
        binding.loungeRow.visibility = View.VISIBLE
        binding.loungeTitle.text = "${lounge.count} airport lounges"
        binding.loungeSub.text = lounge.program
        binding.loungeLogo.setImageResource(lounge.logo)
    }

    /** Initial / type-change selection: apply with a gentle fade-in. */
    private fun selectVariant(index: Int) {
        applyVariant(index)
        binding.imgHeroCard.translationX = 0f
        binding.imgHeroCard.alpha = 0f
        binding.imgHeroCard.translationY = 16f
        binding.imgHeroCard.animate().alpha(1f).translationY(0f).setDuration(240).start()
    }

    /** Tab-tap or swipe: slide the current card out and the new one in from the side. */
    private fun animateToVariant(newIndex: Int) {
        if (newIndex == selectedIndex) return
        if (newIndex !in currentVariants.indices) {
            bounceHero(newIndex > selectedIndex)
            return
        }
        val goingNext = newIndex > selectedIndex
        val w = (binding.imgHeroCard.width.takeIf { it > 0 } ?: dp(300)).toFloat()
        val outX = if (goingNext) -w else w
        binding.imgHeroCard.animate()
            .translationX(outX).alpha(0f).setDuration(150)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                applyVariant(newIndex)
                binding.imgHeroCard.translationX = -outX
                binding.imgHeroCard.animate()
                    .translationX(0f).alpha(1f).setDuration(220)
                    .setInterpolator(DecelerateInterpolator()).start()
            }.start()
    }

    /** A small rubber-band nudge when swiping past the first/last variant. */
    private fun bounceHero(next: Boolean) {
        val d = if (next) -dp(14).toFloat() else dp(14).toFloat()
        binding.imgHeroCard.animate().translationX(d).setDuration(90)
            .withEndAction {
                binding.imgHeroCard.animate().translationX(0f).setDuration(140).start()
            }.start()
    }

    private fun descFor(v: CardVariantModel): String {
        val base = when (v.type) {
            "discover" -> "Accepted worldwide with Google Pay and contactless payments."
            "fatora" -> "A local Syrian card for everyday payments across the country, with contactless support."
            else -> "Accepted worldwide with Google Pay and Samsung Pay, contactless payments, and 3-D Secure protection."
        }
        val lead = when {
            v.fullName == "visasignaturesy" ->
                "The special Syrian edition — premium Visa Signature benefits made for Syria."
            v.fullName.contains("cashback") ->
                "Earn instant cashback on every domestic and international purchase."
            v.fullName.contains("gold") || v.fullName.contains("platinum") || v.fullName.contains("signature") ->
                "Premium rewards, higher limits and priority support."
            v.fullName.contains("travel") ->
                "Travel perks, airport lounge access and fair foreign-exchange rates."
            v.fullName.contains("secured") ->
                "Build your credit history safely with a secured limit."
            v.fullName.contains("digital") ->
                "A fully virtual card, ready the moment you create it."
            else ->
                "Your secure choice for shopping, online purchases and points of sale."
        }
        return "$lead $base"
    }

    private fun variantsFor(type: String): List<CardVariantModel> {
        fun p(key: String) = prices[key] ?: defaultPrices[key] ?: 0
        return when (type) {
            "visa" -> listOf(
                CardVariantModel("Visa Classic", "Standard benefits", R.drawable.visaclassic, p("visaclassic"), "visa", "visaclassic"),
                CardVariantModel("Visa Gold", "Enhanced rewards", R.drawable.visagold, p("visagold"), "visa", "visagold"),
                CardVariantModel("Visa Signature", "Premium benefits", R.drawable.visasignature, p("visasignature"), "visa", "visasignature"),
                CardVariantModel("Visa Signature SY", "Syria edition", R.drawable.visasignaturesy, p("visasignaturesy"), "visa", "visasignaturesy"),
                CardVariantModel("Visa Travel", "Travel perks", R.drawable.visatravel, p("visatravel"), "visa", "visatravel")
            )
            "mastercard" -> listOf(
                CardVariantModel("Mastercard Classic", "Standard benefits", R.drawable.mastercardclassic, p("mastercardclassic"), "mastercard", "mastercardclassic"),
                CardVariantModel("Mastercard Platinum", "Premium rewards", R.drawable.mastercardplatinum, p("mastercardplatinum"), "mastercard", "mastercardplatinum")
            )
            "discover" -> listOf(
                CardVariantModel("Discover Regular", "Standard features", R.drawable.discoverregular, p("discoverregular"), "discover", "discoverregular"),
                CardVariantModel("Discover Secured", "Build credit", R.drawable.discoversecured, p("discoversecured"), "discover", "discoversecured")
            )
            "fatora" -> listOf(
                CardVariantModel("Fatora Digital", "Digital payments", R.drawable.fatoradigital, p("fatoradigital"), "fatora", "fatoradigital"),
                CardVariantModel("Fatora Classic", "Standard features", R.drawable.fatoraclassic, p("fatoraclassic"), "fatora", "fatoraclassic"),
                CardVariantModel("Fatora Cash Back", "Earn cash back", R.drawable.fatoracashback, p("fatoracashback"), "fatora", "fatoracashback")
            )
            else -> emptyList()
        }
    }

    private fun makeChip(text: String, onClick: () -> Unit): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextSize(14f)
        tv.setPadding(dp(18), dp(11), dp(18), dp(11))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = dp(8)
        tv.layoutParams = lp
        tv.setOnClickListener { onClick() }
        return tv
    }

    private fun styleChip(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(
            if (selected) R.drawable.chip_tab_selected else R.drawable.chip_tab_unselected
        )
        tv.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) R.color.porcelain_bright else R.color.md_theme_onSurfaceVariant
            )
        )
        tv.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
