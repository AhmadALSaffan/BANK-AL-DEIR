package bankal_deir.com

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView

class CardTypeAdapter(
    private val types: List<Pair<String,Int>>,
    private val onSelect: (String)->Unit
): RecyclerView.Adapter<CardTypeAdapter.VH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    inner class VH(val view: View): RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgCardType)
        val overlay: View = view.findViewById(R.id.viewSelectedOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_type_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, @SuppressLint("RecyclerView") position: Int) {
        val (typeName, resId) = types[position]
        holder.img.setImageResource(resId)
        holder.overlay.visibility = if(position==selectedPos) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener {
            val old=selectedPos
            selectedPos=position
            notifyItemChanged(old); notifyItemChanged(position)
            onSelect(typeName)
        }
    }

    override fun getItemCount() = types.size
}
