package bankal_deir.com

import android.graphics.Color
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private var cards: List<CardModel>,
    private val onCardClick: (CardModel) -> Unit = {}
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    private var mainCardKey: String = ""
    private var walletBalance: Double = 0.0

    /** The main card shows the wallet's main balance instead of its own. */
    fun setMainInfo(mainKey: String, balance: Double) {
        mainCardKey = mainKey
        walletBalance = balance
        notifyDataSetChanged()
    }

    fun update(newCards: List<CardModel>) {
        val currentStates = this.cards.associate { it.cardnumber to it.isVisible }

        newCards.forEach { newCard ->
            newCard.isVisible = currentStates[newCard.cardnumber] ?: false
        }

        this.cards = newCards
        notifyDataSetChanged()
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val backgroundImage: ImageView = itemView.findViewById(R.id.backgroundImage)
        val lockOverlay: FrameLayout = itemView.findViewById(R.id.lockOverlay)
        val cardNumber: TextView = itemView.findViewById(R.id.card_number)
        val expDate: TextView = itemView.findViewById(R.id.exp_date)
        val cvv: TextView = itemView.findViewById(R.id.cvv)
        val cardHolder: TextView = itemView.findViewById(R.id.card_holder)
        val cardBalance: TextView = itemView.findViewById(R.id.card_blance)
        val cardView: ImageView = itemView.findViewById(R.id.card_view)
        val googlePayWallet: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnGooglePayWallet)
        val samsungPayWallet: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnSamsungPayWallet)

        fun bind(card: CardModel) {
            applyState(card.isVisible)
            applyLockedState(card.locked)

            cardView.setOnClickListener {
                card.isVisible = !card.isVisible
                applyState(card.isVisible)
            }

            googlePayWallet.setOnClickListener { comingSoon("Google Pay") }
            samsungPayWallet.setOnClickListener { comingSoon("Samsung Pay") }

            itemView.setOnClickListener {
                onCardClick(card)
            }
        }

        private fun comingSoon(wallet: String) {
            android.widget.Toast.makeText(
                itemView.context,
                "Adding to $wallet is coming soon",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        private fun applyState(visible: Boolean) {
            if (visible) {
                setShownState()
                cardView.setImageResource(R.drawable.hide)
                ImageViewCompat.setImageTintList(cardView, ColorStateList.valueOf(0xFF4edea3.toInt()))
            } else {
                setHiddenState()
                cardView.setImageResource(R.drawable.view)
                ImageViewCompat.setImageTintList(cardView, ColorStateList.valueOf(0xFF4a7a6a.toInt()))
            }
        }

        fun applyLockedState(locked: Boolean) {
            lockOverlay.visibility = if (locked) View.VISIBLE else View.GONE
            itemView.alpha = if (locked) 0.7f else 1.0f
        }

        private fun setShownState() {
            cardNumber.transformationMethod = null
            expDate.transformationMethod = null
            cvv.transformationMethod = null
            cardBalance.transformationMethod = null
        }

        private fun setHiddenState() {
            val passwordFilter = PasswordTransformationMethod.getInstance()
            cardNumber.transformationMethod = passwordFilter
            cvv.transformationMethod = passwordFilter

            val starTransform = object : PasswordTransformationMethod() {
                override fun getTransformation(source: CharSequence, view: View): CharSequence = "****"
            }
            expDate.transformationMethod = starTransform
            cardBalance.transformationMethod = starTransform
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.cardNumber.text = card.cardnumber
        holder.expDate.text = card.cardexp
        holder.cvv.text = card.cardcvv
        holder.cardHolder.text = card.cardholder
        // The main card mirrors the wallet's main balance; others show their own.
        val isMain = card.cardKey.isNotEmpty() && card.cardKey == mainCardKey
        val shownBalance = if (isMain) walletBalance else card.Balnce
        holder.cardBalance.text = "$ %,.2f".format(shownBalance)

        holder.bind(card)

        fun getCardBackground(cardType: String): Int {
            return when {
                "visa classic" in cardType || "visaclassic" in cardType -> R.drawable.visaclassic
                "visa gold" in cardType || "visagold" in cardType -> R.drawable.visagold
                "visa signature sy" in cardType || "visasignaturesy" in cardType -> R.drawable.visasignaturesy
                "visa signature" in cardType || "visasignature" in cardType -> R.drawable.visasignature
                "visa travel" in cardType || "visa travle" in cardType || "visatravel" in cardType -> R.drawable.visatravel
                "visa" in cardType -> R.drawable.visaclassic

                "mastercard platinum" in cardType || "mastercardplatinum" in cardType -> R.drawable.mastercardplatinum
                "mastercard classic" in cardType || "mastercardclassic" in cardType -> R.drawable.mastercardclassic
                "mastercard" in cardType || "master" in cardType -> R.drawable.mastercardclassic

                "discover secured" in cardType || "discoversecured" in cardType -> R.drawable.discoversecured
                "discover regular" in cardType || "discover reguler" in cardType || "discoverregular" in cardType -> R.drawable.discoverregular
                "discover" in cardType -> R.drawable.discoverregular

                "fatora digital" in cardType || "fatoradigital" in cardType -> R.drawable.fatoradigital
                "fatora cash back" in cardType || "fatoracashback" in cardType -> R.drawable.fatoracashback
                "fatora classic" in cardType || "fatoraclassic" in cardType -> R.drawable.fatoraclassic
                "fatora" in cardType -> R.drawable.fatoraclassic

                else -> R.drawable.fatoraclassic
            }
        }

        val cardType = card.cardname?.trim()?.lowercase() ?: ""
        val bgRes = getCardBackground(cardType)
        holder.backgroundImage.setImageResource(bgRes)
        val textColor = getCardTextColor(cardType, holder.itemView.context)
        holder.cardNumber.setTextColor(textColor)
        holder.expDate.setTextColor(textColor)
        holder.cvv.setTextColor(textColor)
        holder.cardHolder.setTextColor(textColor)
    }

    private fun getCardTextColor(cardType: String, context: android.content.Context): Int {
        return when {
            "visaclassic" in cardType -> Color.WHITE
            "visagold" in cardType -> Color.WHITE
            "visasignature" in cardType && "sy" !in cardType -> Color.WHITE
            "visasignaturesy" in cardType -> ContextCompat.getColor(context, R.color.Golden)
            "visatravel" in cardType -> Color.WHITE

            "mastercardclassic" in cardType -> Color.WHITE
            "mastercardplatinum" in cardType -> Color.WHITE

            "discoverregular" in cardType -> Color.WHITE
            "discoversecured" in cardType -> Color.WHITE

            "fatoradigital" in cardType -> Color.WHITE
            "fatoraclassic" in cardType -> Color.WHITE
            "fatoracashback" in cardType -> Color.WHITE

            else -> Color.WHITE
        }
    }

    override fun getItemCount(): Int = cards.size
}
