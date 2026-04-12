package bankal_deir.com

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.History.HistoryActivity
import bankal_deir.com.databinding.ActivityCardsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.mindrot.jbcrypt.BCrypt
import java.text.SimpleDateFormat
import java.util.Locale

class cards : AppCompatActivity() {
    private lateinit var binding: ActivityCardsBinding
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CardAdapter
    private lateinit var recentAdapter: MyAdapter
    private var refreshRunnable: Runnable? = null
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var cachedWalletId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardsBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Card carousel
        recycler = binding.cardsList
        adapter = CardAdapter(emptyList()) { card -> showCardOptionsDialog(card) }
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = adapter

        // Recent usage list
        recentAdapter = MyAdapter(arrayListOf())
        binding.recentUsageList.layoutManager = LinearLayoutManager(this)
        binding.recentUsageList.adapter = recentAdapter

        loadUserCards()
        loadRecentUsage()
        loadWalletBalance()

        binding.btnCreateCard.setOnClickListener {
            startActivity(Intent(this, createNewCard::class.java))
        }

        binding.btnCardPreferences.setOnClickListener {
            Toast.makeText(this, "Card preferences coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvViewAll.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Card Options Dialog ──────────────────────────────────────────────────

    private fun showCardOptionsDialog(card: CardModel) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_card_options)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val txtCardName = dialog.findViewById<TextView>(R.id.txtDialogCardName)
        val txtPinLabel = dialog.findViewById<TextView>(R.id.txtPinOptionLabel)
        val txtLockLabel = dialog.findViewById<TextView>(R.id.txtLockLabel)
        val txtLockSubLabel = dialog.findViewById<TextView>(R.id.txtLockSubLabel)
        val imgLockIcon = dialog.findViewById<ImageView>(R.id.imgLockIcon)
        val optionPin = dialog.findViewById<LinearLayout>(R.id.optionPin)
        val optionLock = dialog.findViewById<LinearLayout>(R.id.optionLock)
        val optionDelete = dialog.findViewById<LinearLayout>(R.id.optionDelete)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelOptions)

        // Show last 4 digits of card number
        val last4 = card.cardnumber.takeLast(4).ifEmpty { "????" }
        txtCardName.text = "**** **** **** $last4"

        // PIN label: "Create PIN" or "Change PIN"
        txtPinLabel.text = if (card.pin.isEmpty()) "Create PIN" else "Change PIN"

        // Lock label based on current state
        if (card.locked) {
            txtLockLabel.text = "Unlock Card"
            txtLockSubLabel.text = "Re-enable card transactions"
            imgLockIcon.setImageResource(R.drawable.ic_lock)
            androidx.core.widget.ImageViewCompat.setImageTintList(
                imgLockIcon,
                android.content.res.ColorStateList.valueOf(0xFF4edea3.toInt())
            )
        } else {
            txtLockLabel.text = "Lock Card"
            txtLockSubLabel.text = "Temporarily disable card transactions"
            imgLockIcon.setImageResource(R.drawable.ic_lock)
            androidx.core.widget.ImageViewCompat.setImageTintList(
                imgLockIcon,
                android.content.res.ColorStateList.valueOf(0xFFffb95f.toInt())
            )
        }

        optionPin.setOnClickListener {
            dialog.dismiss()
            showPinDialog(card)
        }

        optionLock.setOnClickListener {
            dialog.dismiss()
            toggleCardLock(card)
        }

        optionDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmDialog(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ── PIN Dialog ───────────────────────────────────────────────────────────

    private fun showPinDialog(card: CardModel) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pin_input)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val titleTxt = dialog.findViewById<TextView>(R.id.txtPinDialogTitle)
        titleTxt.text = if (card.pin.isEmpty()) "Create PIN" else "Change PIN"

        val dots = listOf(
            dialog.findViewById<ImageView>(R.id.pinDot1),
            dialog.findViewById<ImageView>(R.id.pinDot2),
            dialog.findViewById<ImageView>(R.id.pinDot3),
            dialog.findViewById<ImageView>(R.id.pinDot4)
        )

        var pinInput = ""

        fun updateDots() {
            dots.forEachIndexed { index, dot ->
                dot.setImageResource(
                    if (index < pinInput.length) R.drawable.pin_circle_filled
                    else R.drawable.pin_circle_empty
                )
            }
        }

        fun appendDigit(digit: String) {
            if (pinInput.length < 4) {
                pinInput += digit
                updateDots()
            }
        }

        val numButtons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        numButtons.forEach { (id, digit) ->
            dialog.findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }

        dialog.findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (pinInput.isNotEmpty()) {
                pinInput = pinInput.dropLast(1)
                updateDots()
            }
        }

        dialog.findViewById<Button>(R.id.btnClearPin).setOnClickListener {
            pinInput = ""
            updateDots()
        }

        dialog.findViewById<Button>(R.id.btnConfirmPin).setOnClickListener {
            if (pinInput.length < 4) {
                Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            savePin(card, pinInput)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btnCancelPin).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun savePin(card: CardModel, rawPin: String) {
        val walletId = cachedWalletId ?: return
        val hashed = BCrypt.hashpw(rawPin, BCrypt.gensalt())
        FirebaseDatabase.getInstance()
            .getReference("wallets/$walletId/cards/${card.cardKey}/pin")
            .setValue(hashed)
            .addOnSuccessListener {
                Toast.makeText(this, "PIN saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save PIN", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Lock / Unlock ────────────────────────────────────────────────────────

    private fun toggleCardLock(card: CardModel) {
        val walletId = cachedWalletId ?: return
        val newState = !card.locked
        FirebaseDatabase.getInstance()
            .getReference("wallets/$walletId/cards/${card.cardKey}/locked")
            .setValue(newState)
            .addOnSuccessListener {
                val msg = if (newState) "Card locked" else "Card unlocked"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                loadUserCardsQuiet()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update card", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Delete Card ──────────────────────────────────────────────────────────

    private fun showDeleteConfirmDialog(card: CardModel) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_card_options)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Reuse options layout but hide pin/lock and re-purpose delete as confirm
        dialog.findViewById<TextView>(R.id.txtDialogCardName).text =
            "Delete this card permanently?"
        dialog.findViewById<LinearLayout>(R.id.optionPin).visibility = View.GONE
        dialog.findViewById<LinearLayout>(R.id.optionLock).visibility = View.GONE

        dialog.findViewById<LinearLayout>(R.id.optionDelete).setOnClickListener {
            dialog.dismiss()
            deleteCard(card)
        }

        dialog.findViewById<Button>(R.id.btnCancelOptions).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteCard(card: CardModel) {
        val walletId = cachedWalletId ?: return
        FirebaseDatabase.getInstance()
            .getReference("wallets/$walletId/cards/${card.cardKey}")
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
                loadUserCardsQuiet()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete card", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private fun loadWalletBalance() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("walletId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val walletId = snapshot.value?.toString() ?: return
                    FirebaseDatabase.getInstance().getReference("wallets").child(walletId)
                        .get().addOnSuccessListener { walletSnap ->
                            val balance = walletSnap.child("Balance").value
                                ?.toString()?.toDoubleOrNull() ?: 0.0
                            binding.tvAvailableCredit.text = "$%.2f".format(balance)
                        }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CardsActivity", "Balance load error: ${error.message}")
                }
            })
    }

    private fun loadRecentUsage() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("history")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PaymentTransaction>()
                    for (child in snapshot.children) {
                        val tx = child.getValue(PaymentTransaction::class.java) ?: continue
                        if (tx.senderUserId == uid || tx.receiverWalletID == uid) {
                            list.add(tx)
                        }
                    }
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    val recent = list.sortedByDescending {
                        try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
                    }.take(5)
                    recentAdapter.updateData(ArrayList(recent))
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CardsActivity", "Recent usage error: ${error.message}")
                }
            })
    }

    private fun loadUserCards() {
        val progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.show()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("CardsActivity", "No user signed in")
            progressDialog.dismiss()
            return
        }

        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .child("walletId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val walletId = userSnapshot.value?.toString()
                    if (walletId.isNullOrEmpty()) {
                        progressDialog.dismiss()
                        adapter.update(emptyList())
                        updateCardStats(0)
                        return
                    }
                    cachedWalletId = walletId

                    FirebaseDatabase.getInstance().getReference("wallets")
                        .child(walletId).child("cards")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val cardList = mutableListOf<CardModel>()
                                for (cardSnap in snapshot.children) {
                                    val card = cardSnap.getValue(CardModel::class.java)
                                    if (card != null) {
                                        card.cardKey = cardSnap.key ?: ""
                                        cardList.add(card)
                                    } else Log.w("CardsActivity", "Null card at ${cardSnap.key}")
                                }
                                Log.d("CardsActivity", "Fetched ${cardList.size} cards from wallet $walletId")
                                adapter.update(cardList)
                                updateCardStats(cardList.size)
                                progressDialog.dismiss()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("CardsActivity", "Database error: ${error.message}")
                                adapter.update(emptyList())
                                progressDialog.dismiss()
                                scheduleRefresh()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CardsActivity", "Error fetching walletId: ${error.message}")
                    adapter.update(emptyList())
                    progressDialog.dismiss()
                    scheduleRefresh()
                }
            })
    }

    private fun updateCardStats(count: Int) {
        binding.tvTotalCards.text = if (count == 1) "1 Card" else "$count Cards"
        binding.tvSecurityStatus.text = if (count > 0) "Protected" else "No Cards"
        updateCarouselDots(count)
    }

    private fun updateCarouselDots(count: Int) {
        val active = 0xFF4edea3.toInt()
        val inactive = 0xFF143b30.toInt()
        binding.dot1.setBackgroundColor(if (count >= 1) active else inactive)
        binding.dot2.setBackgroundColor(if (count >= 2) active else inactive)
        binding.dot3.setBackgroundColor(if (count >= 3) active else inactive)
    }

    private fun scheduleRefresh() {
        refreshRunnable?.let { handler.removeCallbacks(it) }
        refreshRunnable = Runnable { loadUserCardsQuiet() }
        handler.postDelayed(refreshRunnable!!, 3000)
    }

    private fun loadUserCardsQuiet() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .child("walletId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val walletId = userSnapshot.value?.toString()
                    if (walletId.isNullOrEmpty()) {
                        scheduleRefresh()
                        return
                    }
                    cachedWalletId = walletId

                    FirebaseDatabase.getInstance().getReference("wallets")
                        .child(walletId).child("cards")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val cardList = mutableListOf<CardModel>()
                                for (cardSnap in snapshot.children) {
                                    val card = cardSnap.getValue(CardModel::class.java)
                                    if (card != null) {
                                        card.cardKey = cardSnap.key ?: ""
                                        cardList.add(card)
                                    }
                                }
                                Log.d("CardsActivity", "Auto-refreshed: ${cardList.size} cards")
                                adapter.update(cardList)
                                updateCardStats(cardList.size)
                                scheduleRefresh()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("CardsActivity", "Auto-refresh error: ${error.message}")
                                scheduleRefresh()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CardsActivity", "Auto-refresh walletId error: ${error.message}")
                    scheduleRefresh()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        scheduleRefresh()
        loadWalletBalance()
        loadRecentUsage()
    }

    override fun onPause() {
        super.onPause()
        refreshRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshRunnable?.let { handler.removeCallbacks(it) }
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
