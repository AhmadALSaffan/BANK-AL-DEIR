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
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.ShowTransaction.ShowTransaction

class MyAdapter(private var transList: ArrayList<PaymentTransaction>)
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
            tx.transactionNumber.startsWith("SUN") -> {
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount.toDouble()}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_student)
                holder.tr_Type.text = "University Tuition Payment"
                holder.tr_Type.textSize = 18f
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )

            }
            tx.transactionNumber.startsWith("SUF") -> {
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount.toDouble()}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_fine)
                holder.tr_Type.text = "University Fines"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SUH")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_hostel)
                holder.tr_Type.text = "Hostel Fees"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SGI")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_passport)
                holder.tr_Type.text = "New Passport"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SGF")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_government)
                holder.tr_Type.text = "Immigration Fine"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SGD")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_id_card)
                holder.tr_Type.text = "Issue a new ID card"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SMP")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_bill)
                holder.tr_Type.text = "Pay Mobile Bill"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SMR")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_refill)
                holder.tr_Type.text = "Refill Balance"
                holder.tr_Type.setTextColor(Color.RED)
                ImageViewCompat.setImageTintList(
                    holder.outlineBox,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.RED))
                )
            }

            tx.transactionNumber.startsWith("SEL")->{
                holder.tr_number.setTextColor(Color.RED)
                holder.tr_amount.text = "-${tx.amount}$"
                holder.tr_amount.setTextColor(Color.RED)
                holder.outlineBox.setBackgroundResource(R.drawable.red_line_back)
                holder.outlineBox.setImageResource(R.drawable.ic_electricity)
                holder.tr_Type.text = "Electricity Payment"
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
                putExtra("university",tx.university)
                putExtra("studentNumber",tx.studentNumber)
                putExtra("userId",tx.senderUserId)
                putExtra("refrenceNumber",tx.referenceNumber)
                putExtra("passportType",tx.passportType)
                putExtra("orderNumber",tx.orderNumber)
                putExtra("idNumber",tx.idNumber)
                putExtra("mobileNumber",tx.mobileNumber)
                putExtra("provider",tx.provider)
                putExtra("company",tx.company)
                putExtra("billNumber",tx.billNumber)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = transList.size

    fun updateData(newList: List<PaymentTransaction>) {
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