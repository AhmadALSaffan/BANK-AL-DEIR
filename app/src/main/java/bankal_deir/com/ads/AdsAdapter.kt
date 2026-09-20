package bankal_deir.com.ads
import bankal_deir.com.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdsAdapter(private val items: List<AdItem>) :
    RecyclerView.Adapter<AdsAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.ad_card_item, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ad = items[position]
        holder.title.text = ad.title
        holder.subtitle.text = ad.subtitle
        holder.subtitle.visibility = if (ad.subtitle.isBlank()) View.GONE else View.VISIBLE

        if (ad.imageUrl.isNotBlank()) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView).load(ad.imageUrl).into(holder.image)
        } else {
            holder.image.visibility = View.GONE
            holder.image.setImageDrawable(null)
        }
    }

    override fun getItemCount() = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.adImage)
        val title: TextView = itemView.findViewById(R.id.adTitle)
        val subtitle: TextView = itemView.findViewById(R.id.adSubtitle)
    }
}
