package bankal_deir.com.ShowTransaction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityShowTransactionBinding

class ShowTransaction : AppCompatActivity() {
    private lateinit var binding: ActivityShowTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityShowTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val transactionNumber = intent.getStringExtra("transactionNumber") ?: ""
        val amount = intent.getStringExtra("amount") ?: "0.00"
        val date = intent.getStringExtra("date") ?: ""
        val senderWallet = intent.getStringExtra("senderWallet") ?: ""
        val receiverWallet = intent.getStringExtra("receiverWallet") ?: ""
        val university = intent.getStringExtra("university") ?: ""
        val studentNumber = intent.getStringExtra("studentNumber") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""
        val referenceNumber = intent.getStringExtra("refrenceNumber") ?: ""
        val passportType = intent.getStringExtra("passportType") ?: ""
        val orderNumber = intent.getStringExtra("orderNumber") ?: ""
        val idNumber = intent.getStringExtra("idNumber") ?: ""
        val mobileNumber = intent.getStringExtra("mobileNumber")
        val provider = intent.getStringExtra("provider")
        val company = intent.getStringExtra("company")
        val billNumber = intent.getStringExtra("billNumber")



        val formattedTransactionNumber = formatTransactionNumber(transactionNumber)

        binding.transactionNShow.text = formattedTransactionNumber

        binding.DateShow.text = date

        when {
            transactionNumber.startsWith("PLP") -> {
                binding.typeTransactionTxt.text = "PayPal TopUp"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_plp)

                binding.txtAmount.text = "+$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Success_Green))

                binding.fromOrWalletTxt.text = "FROM"
                binding.FromShow.text = "PayPal-External"

                binding.toWalletShow.text = receiverWallet
            }

            transactionNumber.startsWith("SYP") -> {

                binding.typeTransactionTxt.text = "Send Money"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Sender Wallet"
                binding.FromShow.text = senderWallet

                binding.toWalletShow.text = receiverWallet
            }
            transactionNumber.startsWith("SUN")->{
                binding.typeTransactionTxt.text = "University Tuition Payment"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "To"
                binding.FromShow.text = "($university)\n to the student number ($studentNumber)"
                binding.FromShow.textSize = 20f

                binding.toWallet.text = "From"
                binding.toWalletShow.text = userId

            }

            transactionNumber.startsWith("SUF")->{
                binding.typeTransactionTxt.text = "University Fines Payment"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "To"
                binding.FromShow.text = "($university)\n with refrence number ($referenceNumber)"
                binding.FromShow.textSize = 20f

                binding.toWallet.text = "From"
                binding.toWalletShow.text = userId

            }
            transactionNumber.startsWith("SUH")->{
                binding.typeTransactionTxt.text = "University Hostel Payment"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "To"
                binding.FromShow.text = "($university)\n with refrence number ($referenceNumber)"
                binding.FromShow.textSize = 20f

                binding.toWallet.text = "From"
                binding.toWalletShow.text = userId
            }

            transactionNumber.startsWith("SGI")-> {
                binding.typeTransactionTxt.text = "New Passport"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Type"
                binding.FromShow.text = "($passportType)\n id number ($idNumber) \n order number ($orderNumber)"
                binding.FromShow.textSize = 20f

                binding.toWallet.text = "From"
                binding.toWalletShow.text = userId
            }

            transactionNumber.startsWith("SGF")-> {
                binding.typeTransactionTxt.text = "Immigration Fines Payment"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Info"
                binding.FromShow.text = "id number ($idNumber) \nreference number ($referenceNumber)"
                binding.FromShow.textSize = 20f
            }

            transactionNumber.startsWith("SGD")->{
                binding.typeTransactionTxt.text = "Issue a new ID card"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Info"
                binding.FromShow.text = "id number ($idNumber) \norder number ($orderNumber)"
                binding.FromShow.textSize = 20f
            }

            transactionNumber.startsWith("SMP")->{
                binding.typeTransactionTxt.text = "Pay Mobile Bill"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Info"
                binding.FromShow.text = "mobile number ($mobileNumber) \nprovider $provider \norder number ($orderNumber)"
                binding.FromShow.textSize = 20f
            }

            transactionNumber.startsWith("SMR")->{
                binding.typeTransactionTxt.text = "Refill Balance"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Info"
                binding.FromShow.text = "mobile number ($mobileNumber) \nprovider $provider \norder number ($orderNumber)"
                binding.FromShow.textSize = 20f
            }

            transactionNumber.startsWith("SEL")->{
                binding.typeTransactionTxt.text = "Electricity Payment"
                binding.typeTransactionTxt.setBackgroundResource(R.drawable.back_syp)
                binding.typeTransactionTxt.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.txtAmount.text = "-$$amount"
                binding.txtAmount.setTextColor(ContextCompat.getColor(this, R.color.Danger_Red))

                binding.fromOrWalletTxt.text = "Info"
                binding.FromShow.text = "bill number ($billNumber) \nprovider($company) \nreference number ($referenceNumber)"
                binding.FromShow.textSize = 20f
            }


        }

        binding.CopyTransaction.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Transaction Number", transactionNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Transaction number copied!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackShow.setOnClickListener {
            finish()
        }
    }

    private fun formatTransactionNumber(transactionNumber: String): String {
        val formatted = StringBuilder()
        for (i in transactionNumber.indices) {
            if (i > 0 && i % 3 == 0) {
                formatted.append("-")
            }
            formatted.append(transactionNumber[i])
        }
        return formatted.toString()
    }
}