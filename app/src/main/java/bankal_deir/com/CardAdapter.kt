package bankal_deir.com

import android.graphics.Color
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.databinding.CardItemBinding
import kotlinx.coroutines.NonDisposableHandle.parent

class CardAdapter(private var cards: List<CardModel>) :
    RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

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
        val cardNumber: TextView = itemView.findViewById(R.id.card_number)
        val expDate: TextView = itemView.findViewById(R.id.exp_date)
        val cvv: TextView = itemView.findViewById(R.id.cvv)
        val cardHolder: TextView = itemView.findViewById(R.id.card_holder)
        val cardBalance: TextView = itemView.findViewById(R.id.card_blance)
        val cardView: ImageView = itemView.findViewById(R.id.card_view)


        fun bind(card: CardModel) {
            // Set the UI state based on the data
            applyState(card.isVisible)

            // Set click listener once
            cardView.setOnClickListener {
                card.isVisible = !card.isVisible
                applyState(card.isVisible)
            }
        }

        private fun applyState(visible: Boolean) {
            if (visible) {
                setShownState()
                cardView.setImageResource(R.drawable.hide)
            } else {
                setHiddenState()
                cardView.setImageResource(R.drawable.view)
            }
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
        holder.cardBalance.text = "$ ${card.Balnce}"

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

            "visaclassic" in cardType -> Color.BLACK
            "visagold" in cardType -> Color.BLACK
            "visasignature" in cardType && "sy" !in cardType -> Color.BLACK
            "visasignaturesy" in cardType -> ContextCompat.getColor(context, R.color.Golden)
            "visatravel" in cardType -> Color.BLACK


            "mastercardclassic" in cardType -> Color.BLACK
            "mastercardplatinum" in cardType -> Color.BLACK


            "discoverregular" in cardType -> Color.BLACK
            "discoversecured" in cardType -> Color.BLACK


            "fatoradigital" in cardType -> Color.BLACK
            "fatoraclassic" in cardType -> Color.BLACK
            "fatoracashback" in cardType -> Color.BLACK

            else -> Color.BLACK
        }
    }

    override fun getItemCount(): Int = cards.size

}


