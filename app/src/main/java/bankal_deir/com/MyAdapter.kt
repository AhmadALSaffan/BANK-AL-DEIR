package bankal_deir.com

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.ShowTransaction.ShowTransaction

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
        holder.tr_date.text = tx.date
        val context = holder.itemView.context
        when {
            tx.transactionNumber.startsWith("PLP") -> {
                holder.tr_number.setTextColor(Color.GREEN)
                holder.tr_amount.text = "+${tx.amount}$"
                holder.tr_amount.setTextColor(Color.GREEN)
                holder.outlineBox.setBackgroundResource(R.drawable.green_line_back)
                holder.outlineBox.setImageResource(R.drawable.topup)
                holder.tr_amount.setTextColor(Color.GREEN)
                holder.tr_Type.text = "Top-up"
                holder.tr_Type.setTextColor(Color.GREEN)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rec_icon))
                )
            }
            tx.transactionNumber.startsWith("SYP") -> {
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount.toDouble()}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.send64)
                holder.tr_Type.text = "SEND"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }
            else -> {
                holder.tr_amount.text = "${tx.amount}$"
                holder.outlineBox.setBackgroundResource(R.drawable.green_line_back)
                holder.outlineBox.setImageResource(R.drawable.send64)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ShowTransaction::class.java).apply {
                putExtra("transactionNumber", tx.transactionNumber)
                putExtra("amount", tx.amount.toString())
                putExtra("date", tx.date)

                putExtra("senderWallet", tx.senderWalletID ?: "")
                putExtra("receiverWallet", tx.receiverWalletID ?: "")
            }
            context.startActivity(intent)
        }
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
        val outlineBox: ImageView = itemView.findViewById(R.id.outlineBox)
        val tr_Type: TextView = itemView.findViewById(R.id.tr_Type)
    }
}