package bankal_deir.com

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.ShowTransaction.ShowTransaction

class MyAdapter(private var transList: ArrayList<PaymentTransaction>)
    : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    // Enamel signal inks — legible on the porcelain ground.
    private val COLOR_INCOME  = 0xFF1E6B41.toInt()   // signal green (received)
    private val COLOR_EXPENSE = 0xFFBE4A22.toInt()   // signal orange (spent)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.trasnaction_item, parent, false)
        )

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val tx = transList[position]
        val context = holder.itemView.context

        // Flat rows: a tinted glyph and a colour-coded amount. No badge behind the icon.
        fun income(label: String, iconRes: Int) {
            holder.tr_Type.text = label
            holder.tr_amount.text = "+$%,.2f".format(tx.amount)
            holder.tr_amount.setTextColor(COLOR_INCOME)
            holder.outlineBox.setImageResource(iconRes)
            ImageViewCompat.setImageTintList(holder.outlineBox, ColorStateList.valueOf(COLOR_INCOME))
        }

        fun expense(label: String, iconRes: Int) {
            holder.tr_Type.text = label
            holder.tr_amount.text = "-$%,.2f".format(tx.amount)
            holder.tr_amount.setTextColor(COLOR_EXPENSE)
            holder.outlineBox.setImageResource(iconRes)
            ImageViewCompat.setImageTintList(holder.outlineBox, ColorStateList.valueOf(COLOR_EXPENSE))
        }

        holder.tr_number.text = "#" + tx.transactionNumber.takeLast(6)
        holder.tr_date.text = tx.date

        when {
            tx.transactionNumber.startsWith("PLP") -> {
                income("Top-up", R.drawable.google_pay)
                // Keep the Google Pay logo in its own colours instead of the income tint.
                ImageViewCompat.setImageTintList(holder.outlineBox, null)
            }

            tx.transactionNumber.startsWith("TRF") ->
                income("Card transfer", R.drawable.ic_send_transfer)

            tx.transactionNumber.startsWith("SYP") ->
                expense("Send", R.drawable.ic_send_transfer)

            tx.transactionNumber.startsWith("SUN") ->
                expense("University Tuition", R.drawable.ic_student)

            tx.transactionNumber.startsWith("SUF") ->
                expense("University Fines", R.drawable.ic_fine)

            tx.transactionNumber.startsWith("SUH") ->
                expense("Hostel Fees", R.drawable.ic_hostel)

            tx.transactionNumber.startsWith("SGI") ->
                expense("New Passport", R.drawable.ic_passport)

            tx.transactionNumber.startsWith("SGF") ->
                expense("Immigration Fine", R.drawable.ic_government)

            tx.transactionNumber.startsWith("SGD") ->
                expense("New ID Card", R.drawable.ic_id_card)

            tx.transactionNumber.startsWith("SMP") ->
                expense("Mobile Bill", R.drawable.ic_bill)

            tx.transactionNumber.startsWith("SMR") ->
                expense("Refill Balance", R.drawable.ic_refill)

            tx.transactionNumber.startsWith("SEL") ->
                expense("Electricity", R.drawable.ic_electricity)

            else -> {
                holder.tr_Type.text = "Transaction"
                holder.tr_amount.text = "$%,.2f".format(tx.amount)
                holder.tr_amount.setTextColor(COLOR_INCOME)
                holder.outlineBox.setImageResource(R.drawable.ic_send_transfer)
                ImageViewCompat.setImageTintList(holder.outlineBox, ColorStateList.valueOf(COLOR_INCOME))
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ShowTransaction::class.java).apply {
                putExtra("transactionNumber", tx.transactionNumber)
                putExtra("amount",            tx.amount.toString())
                putExtra("date",              tx.date)
                putExtra("senderWallet",      tx.senderWalletID ?: "")
                putExtra("receiverWallet",    tx.receiverWalletID ?: "")
                putExtra("university",        tx.university)
                putExtra("studentNumber",     tx.studentNumber)
                putExtra("userId",            tx.senderUserId)
                putExtra("refrenceNumber",    tx.referenceNumber)
                putExtra("passportType",      tx.passportType)
                putExtra("orderNumber",       tx.orderNumber)
                putExtra("idNumber",          tx.idNumber)
                putExtra("mobileNumber",      tx.mobileNumber)
                putExtra("provider",          tx.provider)
                putExtra("company",           tx.company)
                putExtra("billNumber",        tx.billNumber)
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
        val tr_number:  TextView  = itemView.findViewById(R.id.tr_number)
        val tr_amount:  TextView  = itemView.findViewById(R.id.tr_amount)
        val tr_date:    TextView  = itemView.findViewById(R.id.tr_date)
        val outlineBox: ImageView = itemView.findViewById(R.id.outlineBox)
        val tr_Type:    TextView  = itemView.findViewById(R.id.tr_Type)
    }
}
