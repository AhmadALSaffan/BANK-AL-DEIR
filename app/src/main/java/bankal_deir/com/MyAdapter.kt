package bankal_deir.com

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyAdapter(private var transList: ArrayList<transactions>)
    : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.trasnaction_item, parent, false)
        )

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val tx = transList[position]
        holder.tr_number.text = tx.transactionNumber
        holder.tr_amount.text = "-${tx.amount}$"
        holder.tr_date.text = tx.date
    }

    override fun getItemCount() = transList.size

    fun updateData(newList: List<transactions>) {
        transList.clear()
        transList.addAll(newList)
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tr_number: TextView = itemView.findViewById(R.id.tr_number)
        val tr_amount: TextView = itemView.findViewById(R.id.tr_amount)
        val tr_date: TextView = itemView.findViewById(R.id.tr_date)
    }
}

