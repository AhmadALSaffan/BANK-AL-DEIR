package bankal_deir.com

import android.animation.Animator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.ActivityFinalDetailsNewCardBinding
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.properties.Delegates

class finalDetailsNewCard : AppCompatActivity() {
    private lateinit var binding: ActivityFinalDetailsNewCardBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var name: String
    private lateinit var fName: String
    private lateinit var currentUserWalletId: String
    private var amount by Delegates.notNull<Double>()

    private var cardType: String? = null
    private var variant: String? = null
    private var fullCardName: String? = null
    private var fees: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        binding = ActivityFinalDetailsNewCardBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cardType = intent.getStringExtra("cardtype")
        variant = intent.getStringExtra("variant")
        fullCardName = intent.getStringExtra("fullcardname")
        fees = intent.getIntExtra("fees", 0)

        Log.d("finalDetailsNewCard", "Received - cardType: $cardType, variant: $variant, fullCardName: $fullCardName, fees: $fees")

        readData()
        calculateTax()

        binding.btnCreate.setOnClickListener {
            if (binding.checkBox2.isChecked) {
                readWalletId { walletId ->
                    if (walletId.isNotEmpty()) {
                        readBalance(walletId, amount)
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Please Agree with the Terms and Conditions !!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnCancelCreate.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    fun calculateTax() {
        val fee = fees.toDouble()
        val taxRate = 0.15
        val tax = fee * taxRate
        val totalPrice = fee + tax

        binding.txtFirstPrice.text = String.format("%.2f", fee)
        binding.txtTax.text = String.format("%.2f", tax)
        binding.txtTotalPrice.text = String.format("%.2f", totalPrice)
        amount = totalPrice
    }

    fun readBalance(walletId: String, amount: Double) {
        databaseReference = FirebaseDatabase.getInstance().getReference("wallets")

        databaseReference.child(walletId).get().addOnSuccessListener { snapshot ->
            val balanceValue = snapshot.child("Balance").value
            if (balanceValue != null) {
                val userBalance = balanceValue.toString().toDouble()
                if (userBalance >= amount) {
                    databaseReference.child(walletId).child("Balance").setValue(userBalance - amount)
                        .addOnSuccessListener {
                            createRandomCard()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this, "Failed to update balance: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Insufficient balance", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Balance not found for wallet: $walletId", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error reading balance: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun readWalletId(callback: (String) -> Unit) {
        val uID = mAuth.currentUser?.uid
        if (uID == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            callback("")
            return
        }
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(uID).child("walletId").get().addOnSuccessListener { snapshot ->
            val walletId = snapshot.value?.toString() ?: ""
            currentUserWalletId = walletId
            callback(walletId)
        }.addOnFailureListener {
            Toast.makeText(this, "Error reading wallet ID: ${it.message}", Toast.LENGTH_SHORT).show()
            callback("")
        }
    }

    fun readData() {
        val uID = mAuth.currentUser?.uid
        if (uID == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(uID).get().addOnSuccessListener {
            fName = it.child("firstName").value.toString() + " " + it.child("lastName").value.toString()

            binding.txtCardType.text = cardType?.uppercase() ?: "CARD"
            binding.txtCardVariant.text = variant ?: "Classic"
            binding.txtCardName.text = fName
        }.addOnFailureListener {
            Toast.makeText(this, "Error reading data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createRandomCard() {
        val progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.show()

        val uId = mAuth.currentUser?.uid ?: return

        // First, get the user's walletId from users node
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uId)

        userRef.get().addOnSuccessListener { userSnapshot ->
            // Get user name
            val name = userSnapshot.child("firstName").value.toString() + " " +
                    userSnapshot.child("lastName").value.toString()

            // Get the wallet ID from user data
            val walletId = userSnapshot.child("walletId").value?.toString()

            if (walletId.isNullOrEmpty()) {
                progressDialog.dismiss()
                Toast.makeText(this, "No wallet found for this user", Toast.LENGTH_SHORT).show()
                Log.e("finalDetailsNewCard", "No walletId found in user data")
                return@addOnSuccessListener
            }

            Log.d("finalDetailsNewCard", "Found wallet ID: $walletId")

            // Generate card details
            val cardTypeForNumber = cardType?.lowercase() ?: "visa"
            val firstDigitCard = when (cardTypeForNumber) {
                "visa" -> '4'
                "mastercard" -> '5'
                "discover" -> '3'
                "fatora" -> '9'
                else -> '4'
            }

            val remaining = (1..15).map { ('0'..'9').random() }.joinToString("")
            val fullNumber = firstDigitCard + remaining
            val grouped = listOf(
                fullNumber.substring(0, 4),
                fullNumber.substring(4, 8),
                fullNumber.substring(8, 12),
                fullNumber.substring(12, 16)
            ).joinToString(" ")

            val month = (1..12).random().toString().padStart(2, '0')
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val year = (currentYear + (1..5).random()).toString().takeLast(2)
            val expireDate = "$month/$year"
            val cvv = (100..999).random().toString()

            val newCard = mapOf(
                "cardnumber" to grouped,
                "cardholder" to name,
                "cardexp" to expireDate,
                "cardcvv" to cvv,
                "cardname" to (fullCardName ?: cardTypeForNumber),
                "cardtype" to cardTypeForNumber,
                "variant" to (variant ?: "Classic"),
                "fees" to fees,
                "Balnce" to 0.0
            )

            Log.d("finalDetailsNewCard", "Saving card: $newCard")

            // Now save the card in: wallets/{walletId}/cards
            val walletCardsRef = FirebaseDatabase.getInstance()
                .getReference("wallets")
                .child(walletId)
                .child("cards")

            walletCardsRef.get().addOnSuccessListener { cardsSnapshot ->
                val nextIndex = cardsSnapshot.childrenCount.toInt() + 1
                val cardKey = "card$nextIndex"

                Log.d("finalDetailsNewCard", "Saving as: $cardKey in path: wallets/$walletId/cards/$cardKey")

                walletCardsRef.child(cardKey).setValue(newCard)
                    .addOnSuccessListener {
                        progressDialog.dismiss()

                        val animationDialog = Dialog(this)
                        animationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        animationDialog.setCancelable(false)
                        animationDialog.setContentView(R.layout.suc)
                        animationDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        animationDialog.show()

                        lifecycleScope.launch {
                            delay(3000)
                            animationDialog.dismiss()
                        }

                        animationDialog.setOnDismissListener {
                            Toast.makeText(this, "Card Created Successfully!", Toast.LENGTH_SHORT).show()

                            val nextIntent = Intent(
                                this@finalDetailsNewCard,
                                cardShowDetailsAfterCreate::class.java
                            ).apply {
                                putExtra("cardnumber", grouped)
                                putExtra("cardholder", name)
                                putExtra("cardexp", expireDate)
                                putExtra("cardcvv", cvv)
                                putExtra("cardname", fullCardName)
                                putExtra("variant", variant)
                            }
                            startActivity(nextIntent)
                            finish()
                        }
                    }
                    .addOnFailureListener { error ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error saving card: ${error.message}", Toast.LENGTH_SHORT).show()
                        Log.e("finalDetailsNewCard", "Error: ${error.message}")
                    }
            }.addOnFailureListener { error ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error reading cards: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("finalDetailsNewCard", "Error reading cards: ${error.message}")
            }

        }.addOnFailureListener { error ->
            progressDialog.dismiss()
            Toast.makeText(this, "Error reading user: ${error.message}", Toast.LENGTH_SHORT).show()
            Log.e("finalDetailsNewCard", "Error reading user: ${error.message}")
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
