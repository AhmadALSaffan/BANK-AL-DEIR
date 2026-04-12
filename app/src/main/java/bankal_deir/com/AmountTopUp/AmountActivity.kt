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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.*
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.ActivityAmountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.braintreepayments.api.*
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import java.util.concurrent.TimeUnit
import bankal_deir.com.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AmountActivity : AppCompatActivity(), PayPalListener, GooglePayListener {
    private lateinit var binding: ActivityAmountBinding
    private var userId: String = ""
    private var walletId: String = ""
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var payPalClient: PayPalClient
    private lateinit var googlePayClient: GooglePayClient
    private var pendingAmount: Double = 0.0
    private lateinit var progressDialog: Dialog
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAmountBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        mAuth = FirebaseAuth.getInstance()

        val rawToken = intent.getStringExtra("braintree_token")
            ?: "sandbox_qzp3nw4q_mshysrbxdfskrz9v"

        // Strip explicit :443 port from all URLs inside the client token.
        // We do this via raw string replace (not JSONObject) to avoid JSONObject.toString()
        // escaping '/' as '\/' and corrupting the JWT authorizationFingerprint.
        val braintreeToken = if (!rawToken.startsWith("sandbox_")) {
            try {
                val decoded = String(android.util.Base64.decode(rawToken, android.util.Base64.DEFAULT))
                if (decoded.contains(":443")) {
                    val fixed = android.util.Base64.encodeToString(
                        decoded.replace(":443", "").toByteArray(Charsets.UTF_8),
                        android.util.Base64.NO_WRAP
                    )
                    Log.d("BraintreeSetup", "Stripped :443 from token (raw)")
                    fixed
                } else {
                    Log.d("BraintreeSetup", "No :443 found in token")
                    rawToken
                }
            } catch (e: Exception) {
                Log.e("BraintreeSetup", "Token patch failed: ${e.message}")
                rawToken
            }
        } else {
            rawToken
        }

        // Config is pre-cached by MainPage before this Activity starts.
        // Call setupBraintree synchronously here (onCreate = pre-RESUMED) so that
        // PayPalClient/GooglePayClient can safely register their ActivityResult launchers.
        setupProgressDialog()
        setupBraintree(braintreeToken)

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

        binding.btnGooglePay.setOnClickListener {
            val amountText = binding.etAmount.text.toString()
            val amount = amountText.toDoubleOrNull()
            if (amount != null && amount > 0) {
                pendingAmount = amount
                showProgressDialog()
                startGooglePayCheckout(amount)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
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
    private fun setupBraintree(authorization: String) {
        braintreeClient = BraintreeClient(applicationContext, authorization)
        payPalClient = PayPalClient(this, braintreeClient)
        payPalClient.setListener(this)
        googlePayClient = GooglePayClient(this, braintreeClient)
        googlePayClient.setListener(this)
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


    private fun startGooglePayCheckout(amount: Double) {
        val request = GooglePayRequest().apply {
            transactionInfo = TransactionInfo.newBuilder()
                .setTotalPrice(String.format(Locale.US, "%.2f", amount))
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build()
            isBillingAddressRequired = true
            googleMerchantName = "BANK AL DEIR"
            environment = "TEST"
        }
        googlePayClient.requestPayment(this, request)
    }

    override fun onGooglePaySuccess(paymentMethodNonce: PaymentMethodNonce) {
        if (walletId.isEmpty()) {
            val prefs = getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE)
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
            hideProgressDialog()
            saveTopUpTransaction(pendingAmount)
            Toast.makeText(this, "Google Pay successful! Balance updating…", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainPage::class.java))
            finish()
        }
    }

    override fun onGooglePayFailure(error: Exception) {
        hideProgressDialog()
        Log.e("GooglePay", "Error type: ${error.javaClass.simpleName}")
        Log.e("GooglePay", "Error message: ${error.message}")
        Log.e("GooglePay", "Stack trace: ", error)
        Toast.makeText(this, "Google Pay failed: ${error.javaClass.simpleName}: ${error.message}", Toast.LENGTH_LONG).show()
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
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}