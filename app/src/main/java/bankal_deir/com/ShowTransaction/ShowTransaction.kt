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