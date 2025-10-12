package bankal_deir.com

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.databinding.CardItemBinding
import kotlinx.coroutines.NonDisposableHandle.parent

class CardAdapter(private var cards: List<CardModel>) :
    RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val backgroundImage: ImageView = itemView.findViewById(R.id.backgroundImage)
        val cardNumber: TextView = itemView.findViewById(R.id.card_number)
        val expDate: TextView = itemView.findViewById(R.id.exp_date)
        val cvv: TextView = itemView.findViewById(R.id.cvv)
        val cardHolder: TextView = itemView.findViewById(R.id.card_holder)
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

        val type = card.cardname.trim().lowercase()
        val bgRes = when {
            "visa" in type -> R.drawable.visa
            "mastercard" in type || "master" in type -> R.drawable.mastercard
            "discover" in type -> R.drawable.discover
            "fatora" in type -> R.drawable.fatoracard
            else -> R.drawable.visa
        }
        holder.backgroundImage.setImageResource(bgRes)

        fun update(newCards: List<CardModel>) {
            cards = newCards
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = cards.size

    fun update(newCards: List<CardModel>) {
        cards = newCards
        notifyDataSetChanged()
    }
}


