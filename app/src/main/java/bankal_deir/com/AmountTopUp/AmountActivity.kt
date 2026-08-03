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
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import java.util.concurrent.TimeUnit
import bankal_deir.com.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AmountActivity : AppCompatActivity(), GooglePayListener {
    private lateinit var binding: ActivityAmountBinding
    private var userId: String = ""
    private var walletId: String = ""
    private lateinit var braintreeClient: BraintreeClient
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

        setupGooglePayButton()

        binding.btnBack.setOnClickListener { finish() }

        mapOf(
            binding.chip10 to "10",
            binding.chip25 to "25",
            binding.chip50 to "50",
            binding.chip100 to "100"
        ).forEach { (chip, value) ->
            chip.setOnClickListener {
                binding.etAmount.setText(value)
                binding.etAmount.setSelection(value.length)
            }
        }

        binding.btnGooglePay.setOnClickListener {
            val amount = binding.etAmount.text.toString().toDoubleOrNull()
            if (amount != null && amount > 0) {
                pendingAmount = amount

                getSharedPreferences("PaymentPrefs", Context.MODE_PRIVATE).edit().apply {
                    putFloat("pending_amount", amount.toFloat())
                    putString("wallet_id", walletId)
                    apply()
                }

                binding.btnGooglePay.isEnabled = false
                showProgressDialog()
                startGooglePayCheckout(amount)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Renders the official Google Pay mark. The allowed payment methods only drive
    // the button's appearance; the real request is built in startGooglePayCheckout.
    private fun setupGooglePayButton() {
        val allowedPaymentMethods = org.json.JSONArray().put(
            org.json.JSONObject()
                .put("type", "CARD")
                .put(
                    "parameters", org.json.JSONObject()
                        .put("allowedAuthMethods", org.json.JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                        .put(
                            "allowedCardNetworks",
                            org.json.JSONArray(listOf("VISA", "MASTERCARD", "AMEX", "DISCOVER"))
                        )
                )
        ).toString()

        binding.btnGooglePay.initialize(
            ButtonOptions.newBuilder()
                .setButtonTheme(ButtonConstants.ButtonTheme.LIGHT)
                .setButtonType(ButtonConstants.ButtonType.PAY)
                .setCornerRadius((18 * resources.displayMetrics.density).toInt())
                .setAllowedPaymentMethods(allowedPaymentMethods)
                .build()
        )
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
        googlePayClient = GooglePayClient(this, braintreeClient)
        googlePayClient.setListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // Sends the tokenized payment method to the backend, which creates the actual
    // Braintree transaction. The wallet is only credited if the charge succeeds.
    private fun chargeNonce(nonce: String, amount: Double) {
        showProgressDialog()
        Thread {
            val error: String? = try {
                val url = java.net.URL("https://us-central1-bank-al-deir.cloudfunctions.net/processPayment")
                val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Content-Type", "application/json")
                val body = org.json.JSONObject()
                    .put("nonce", nonce)
                    .put("amount", String.format(Locale.US, "%.2f", amount))
                    .toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode == 200) {
                    null
                } else {
                    val stream = conn.errorStream ?: conn.inputStream
                    try {
                        org.json.JSONObject(stream.bufferedReader().readText())
                            .optString("error", "charge declined")
                    } catch (e: Exception) {
                        "charge declined (HTTP ${conn.responseCode})"
                    }
                }
            } catch (e: Exception) {
                e.message ?: "network error"
            }
            runOnUiThread {
                if (error == null) {
                    creditWallet(amount)
                } else {
                    hideProgressDialog()
                    binding.btnGooglePay.isEnabled = true
                    Log.e("GooglePay", "Charge failed: $error")
                    Toast.makeText(this, "Payment failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun creditWallet(amount: Double) {
        val data = Data.Builder()
            .putDouble("amount", amount)
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
        saveTopUpTransaction(amount)
        Toast.makeText(this, "Payment successful! Balance updating...", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainPage::class.java))
        finish()
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
            binding.tvCurrentBalance.text = "$%,.2f".format(balanceDouble)
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
            chargeNonce(paymentMethodNonce.string, pendingAmount)
        } else {
            hideProgressDialog()
            Toast.makeText(this, "Payment session lost, please try again", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGooglePayFailure(error: Exception) {
        hideProgressDialog()
        binding.btnGooglePay.isEnabled = true
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