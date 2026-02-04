package bankal_deir.com.AmountTopUp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.ActivityAmountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.braintreepayments.api.*
import java.util.concurrent.TimeUnit
import bankal_deir.com.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AmountActivity : AppCompatActivity(), PayPalListener {
    private lateinit var binding: ActivityAmountBinding
    private var userId: String = ""
    private var walletId: String = ""
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var payPalClient: PayPalClient
    private var pendingAmount: Double = 0.0
    private lateinit var progressDialog: Dialog
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        mAuth = FirebaseAuth.getInstance()

        setupBraintree()
        setupProgressDialog()

        val prefs = getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE)
        pendingAmount = prefs.getFloat("pending_amount", 0f).toDouble()
        walletId = prefs.getString("wallet_id", "") ?: ""

        if (walletId.isEmpty()) loadUserWallet()

        binding.btnPayPal.setOnClickListener {
            val amountText = binding.etAmount.text.toString()
            val amount = amountText.toDoubleOrNull()
            showProgressDialog()
            if (amount != null && amount > 0) {
                binding.btnPayPal.isEnabled = false
                pendingAmount = amount

                getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE).edit().apply {
                    putFloat("pending_amount", amount.toFloat())
                    putString("wallet_id", walletId)
                    apply()
                }

                startPayPalCheckout(amount)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }
    private fun showProgressDialog() {
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
    private fun setupProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    private fun setupBraintree() {
        braintreeClient = BraintreeClient(this, "sandbox_qzp3nw4q_mshysrbxdfskrz9v", "bankaldeir.braintree")
        payPalClient = PayPalClient(this, braintreeClient)
        payPalClient.setListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun startPayPalCheckout(amount: Double) {
        val request = PayPalCheckoutRequest(String.format("%.2f", amount)).apply {
            currencyCode = "USD"
            userAction = PayPalCheckoutRequest.USER_ACTION_COMMIT
        }
        payPalClient.tokenizePayPalAccount(this, request)
    }

    override fun onPayPalSuccess(payPalAccountNonce: PayPalAccountNonce) {
        Log.d("PayPal_Debug", "PayPal Success Callback")

        if (pendingAmount <= 0 || walletId.isEmpty()) {
            val prefs = getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE)
            pendingAmount = prefs.getFloat("pending_amount", 0f).toDouble()
            walletId = prefs.getString("wallet_id", "") ?: ""
        }

        if (pendingAmount > 0 && walletId.isNotEmpty()) {
            val data = Data.Builder()
                .putDouble("amount", pendingAmount)
                .putString("walletId", walletId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<UpdateWalletWorker>()
                .setInputData(data)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)

            getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE).edit().clear().apply()
            hideProgressDialog()
            saveTopUpTransaction(pendingAmount)
            Toast.makeText(this, "Payment successful! Balance updating...", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainPage::class.java))
            finish()
        }
    }

    override fun onPayPalFailure(error: Exception) {
        hideProgressDialog()
        binding.btnPayPal.isEnabled = true
        if (error is UserCanceledException) {
            Log.d("PayPal_Debug", "User cancelled or browser session lost")

        } else {
            Log.e("PayPal_Debug", "Error: ${error.message}")
            Toast.makeText(this, "Payment failed: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserWallet() {
        FirebaseDatabase.getInstance().reference.child("users").child(userId).child("walletId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    walletId = snapshot.getValue(String::class.java) ?: ""
                    readBalance(walletId)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun readBalance(walletId: String) {
        showProgressDialog()
        if (walletId.isEmpty()) return
        databaseReference = FirebaseDatabase.getInstance().getReference("wallets")
        databaseReference.child(walletId).get().addOnSuccessListener {
            val balance = it.child("Balance").value
            val balanceDouble = balance?.toString()?.toDoubleOrNull() ?: 0.0
            val formattedBalance = "%.2f".format(balanceDouble)
            binding.tvCurrentBalance.text = "Current Balance: $${formattedBalance}"
            hideProgressDialog()
        }
    }


    private fun saveTopUpTransaction(amount: Double) {
        val currentUserId = mAuth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val databaseRef = FirebaseDatabase.getInstance().reference
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val transaction = transactions.createTopUpTransaction(
            userId = currentUserId,
            amount = amount,
            date = currentDate
        )
        databaseRef.child("history")
            .child(transaction.transactionNumber)
            .setValue(transaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Top-up successful!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save transaction: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


}