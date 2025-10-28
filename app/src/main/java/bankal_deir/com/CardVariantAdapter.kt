package bankal_deir.com

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CardVariantAdapter(
    private val variants: List<CardVariantModel>,
    private val onVariantSelected: (CardVariantModel) -> Unit
) : RecyclerView.Adapter<CardVariantAdapter.VariantViewHolder>() {

    private var selectedPosition = -1

    inner class VariantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardVariantItem)
        val imgCardVariant: ImageView = itemView.findViewById(R.id.imgCardVariant)
        val txtVariantName: TextView = itemView.findViewById(R.id.txtVariantName)
        val txtVariantDescription: TextView = itemView.findViewById(R.id.txtVariantDescription)
        val overlaySelected: View = itemView.findViewById(R.id.overlaySelected)
        val imgCheckmark: ImageView = itemView.findViewById(R.id.imgCheckmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_variant_item, parent, false)
        return VariantViewHolder(view)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        val variant = variants[position]

        holder.imgCardVariant.setImageResource(variant.imageResource)
        holder.txtVariantName.text = variant.variantName

        val feeText = if (variant.fees > 0) {
            "${variant.variantDescription} • $${variant.fees}"
        } else {
            "${variant.variantDescription} • FREE"
        }
        holder.txtVariantDescription.text = feeText

        if (position == selectedPosition) {
            holder.overlaySelected.visibility = View.VISIBLE
            holder.imgCheckmark.visibility = View.VISIBLE
        } else {
            holder.overlaySelected.visibility = View.GONE
            holder.imgCheckmark.visibility = View.GONE
        }

        holder.cardView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onVariantSelected(variant)
        }
    }

    override fun getItemCount(): Int = variants.size

    fun getSelectedVariant(): CardVariantModel? {
        return if (selectedPosition != -1) variants[selectedPosition] else null
    }
}
