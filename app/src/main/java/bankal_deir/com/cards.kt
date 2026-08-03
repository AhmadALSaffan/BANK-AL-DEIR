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
import androidx.core.app.NotificationManagerCompat
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
    private var cachedMainCardKey: String = ""
    private var cachedCards: List<CardModel> = emptyList()
    private var cachedWalletBalance: Double = 0.0

    private val notifPermLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    companion object {
        private const val OTP_NOTIF_ID = 4201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardsBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)
        NavHelper.setup(this, "cards")

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

        binding.btnBack.setOnClickListener { finish() }

        // Ask for notification permission so OTP codes can be delivered.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

        val optionTransfer = dialog.findViewById<LinearLayout>(R.id.optionTransfer)
        val optionMain = dialog.findViewById<LinearLayout>(R.id.optionMain)
        val txtMainLabel = dialog.findViewById<TextView>(R.id.txtMainLabel)
        val txtMainSubLabel = dialog.findViewById<TextView>(R.id.txtMainSubLabel)

        // Reflect whether this card is already the main one.
        if (card.cardKey == cachedMainCardKey && cachedMainCardKey.isNotEmpty()) {
            txtMainLabel.text = "Main card"
            txtMainSubLabel.text = "Your balance lives on this card"
            optionMain.alpha = 0.5f
            optionMain.isEnabled = false
        }

        optionTransfer.setOnClickListener {
            dialog.dismiss()
            showTransferDialog(card)
        }

        optionMain.setOnClickListener {
            if (card.cardKey == cachedMainCardKey) return@setOnClickListener
            dialog.dismiss()
            confirmMakeMain(card)
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

        dialog.setOnShowListener { animateSheetIn(dialog.findViewById(R.id.optionsSheet)) }
        dialog.show()
    }

    /** Slides the sheet up and staggers its rows in. */
    private fun animateSheetIn(sheet: View?) {
        sheet ?: return
        val group = sheet as? android.view.ViewGroup ?: return
        sheet.translationY = 60f * resources.displayMetrics.density
        sheet.alpha = 0f
        sheet.animate().translationY(0f).alpha(1f).setDuration(260)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            child.alpha = 0f
            child.translationY = 20f * resources.displayMetrics.density
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(60L + i * 35L).setDuration(240)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.4f)).start()
        }
    }

    // ── Transfer money onto a card, from the wallet or another card ──────────

    /**
     * A place money can come from. [cardKey] is empty for the wallet's main balance,
     * otherwise it's the source card's key. [balance] is what's currently available.
     */
    private data class TransferSource(val label: String, val cardKey: String, val balance: Double)

    private fun showTransferDialog(destCard: CardModel) {
        val walletId = cachedWalletId ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_transfer_to_card)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val txtDest = dialog.findViewById<TextView>(R.id.txtTransferDest)
        val txtAvailable = dialog.findViewById<TextView>(R.id.txtTransferAvailable)
        val dropdown = dialog.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.dropdownSource)
        val edtAmount = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtTransferAmount)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmTransfer)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelTransfer)

        txtDest.text = "To card ending ${destCard.cardnumber.takeLast(4).ifEmpty { "????" }}"

        // The main card IS the main balance, so it's never listed twice.
        val destIsMain = destCard.cardKey.isNotEmpty() && destCard.cardKey == cachedMainCardKey
        val sources = mutableListOf<TransferSource>()
        FirebaseDatabase.getInstance().getReference("wallets/$walletId/Balance")
            .get().addOnSuccessListener { balSnap ->
                val walletBalance = balSnap.value?.toString()?.toDoubleOrNull() ?: 0.0
                // "Main balance" == the main card; offer it only when it isn't the destination.
                if (!destIsMain) {
                    sources.add(TransferSource("Main balance", "", walletBalance))
                }
                cachedCards
                    .filter { it.cardKey != destCard.cardKey && it.cardKey != cachedMainCardKey }
                    .forEach { c ->
                        sources.add(
                            TransferSource(
                                "Card ending ${c.cardnumber.takeLast(4)}",
                                c.cardKey,
                                c.Balnce
                            )
                        )
                    }
                val labels = sources.map { "${it.label}  ·  $%,.2f".format(it.balance) }
                dropdown.setSimpleItems(labels.toTypedArray())
                dropdown.setText(labels[0], false)
                txtAvailable.text = "Available: $%,.2f".format(sources[0].balance)

                dropdown.setOnItemClickListener { _, _, pos, _ ->
                    txtAvailable.text = "Available: $%,.2f".format(sources[pos].balance)
                }
            }

        btnConfirm.setOnClickListener {
            val selectedIndex = sources.indexOfFirst {
                dropdown.text.toString().startsWith(it.label)
            }.let { if (it < 0) 0 else it }
            val source = sources.getOrNull(selectedIndex)
            val amount = edtAmount.text.toString().toDoubleOrNull()
            when {
                source == null ->
                    Toast.makeText(this, "Pick a source", Toast.LENGTH_SHORT).show()
                amount == null || amount <= 0 ->
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                amount > source.balance ->
                    Toast.makeText(this, "Not enough in ${source.label.lowercase()}", Toast.LENGTH_SHORT).show()
                else -> {
                    dialog.dismiss()
                    startTransferWithOtp(source, destCard, amount)
                }
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener { animateSheetIn(dialog.findViewById(R.id.transferSheet)) }
        dialog.show()
    }

    // ── OTP confirmation ──────────────────────────────────────────────────────

    private val otpChannelId = "otp_channel"

    /** Sends a 4-digit code as a notification, then asks the user to enter it. */
    private fun startTransferWithOtp(source: TransferSource, destCard: CardModel, amount: Double) {
        var code = (1000..9999).random().toString()
        postOtpNotification(code)

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_otp)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val edtOtp = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtOtp)
        val btnVerify = dialog.findViewById<Button>(R.id.btnVerifyOtp)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelOtp)
        val txtResend = dialog.findViewById<TextView>(R.id.txtResendOtp)

        txtResend.setOnClickListener {
            code = (1000..9999).random().toString()
            postOtpNotification(code)
            Toast.makeText(this, "New code sent", Toast.LENGTH_SHORT).show()
        }

        btnVerify.setOnClickListener {
            if (edtOtp.text.toString() == code) {
                dialog.dismiss()
                NotificationManagerCompat.from(this).cancel(OTP_NOTIF_ID)
                performTransfer(source, destCard, amount)
            } else {
                edtOtp.error = "Incorrect code"
                dialog.findViewById<View>(R.id.otpSheet).let { shake(it) }
            }
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
            NotificationManagerCompat.from(this).cancel(OTP_NOTIF_ID)
        }

        dialog.setOnShowListener { animateSheetIn(dialog.findViewById(R.id.otpSheet)) }
        dialog.show()
    }

    private fun shake(view: View?) {
        view ?: return
        android.animation.ObjectAnimator.ofFloat(
            view, View.TRANSLATION_X, 0f, -16f, 16f, -10f, 10f, 0f
        ).apply { duration = 400; start() }
    }

    private fun postOtpNotification(code: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                otpChannelId, "Verification codes",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
        val notif = androidx.core.app.NotificationCompat.Builder(this, otpChannelId)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("Bank Al-Deir verification")
            .setContentText("Your code is $code. Do not share it with anyone.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(OTP_NOTIF_ID, notif)
        } else {
            // Notifications are off — surface the code so the user isn't locked out.
            Toast.makeText(this, "Your code: $code", Toast.LENGTH_LONG).show()
        }
    }

    /** Moves [amount] from [source] onto [destCard], atomically. The main card and
     *  the "Main balance" both resolve to the wallet Balance node. */
    private fun performTransfer(source: TransferSource, destCard: CardModel, amount: Double) {
        val walletId = cachedWalletId ?: return
        val db = FirebaseDatabase.getInstance().getReference("wallets/$walletId")
        val destIsMain = destCard.cardKey.isNotEmpty() && destCard.cardKey == cachedMainCardKey
        val sourcePath = if (source.cardKey.isEmpty()) "Balance" else "cards/${source.cardKey}/Balnce"
        val destPath = if (destIsMain) "Balance" else "cards/${destCard.cardKey}/Balnce"

        db.child(sourcePath).get().addOnSuccessListener { srcSnap ->
            val sourceBalance = srcSnap.value?.toString()?.toDoubleOrNull() ?: 0.0
            if (amount > sourceBalance) {
                Toast.makeText(this, "Not enough balance", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            db.child(destPath).get().addOnSuccessListener { destSnap ->
                val destBalance = destSnap.value?.toString()?.toDoubleOrNull() ?: 0.0
                val updates = mapOf(
                    sourcePath to sourceBalance - amount,
                    destPath to destBalance + amount
                )
                db.updateChildren(updates)
                    .addOnSuccessListener {
                        recordTransferTransaction(amount)
                        Toast.makeText(this, "Transferred $%,.2f".format(amount), Toast.LENGTH_SHORT).show()
                        loadUserCardsQuiet()
                        loadWalletBalance()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Transfer failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun recordTransferTransaction(amount: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        val tx = bankal_deir.com.AmountTopUp.transactions.createTransferTransaction(uid, amount, date)
        FirebaseDatabase.getInstance().getReference("history")
            .child(tx.transactionNumber).setValue(tx)
    }

    // ── Make main card ────────────────────────────────────────────────────────

    private fun confirmMakeMain(card: CardModel) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set as main card?")
            .setMessage("Your main balance will be shown on this card. Top-ups and payments use the same balance.")
            .setPositiveButton("Set as main") { _, _ -> makeMainCard(card) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val maxMainChanges = 3

    private fun makeMainCard(card: CardModel) {
        val walletId = cachedWalletId ?: return
        val db = FirebaseDatabase.getInstance().getReference("wallets/$walletId")
        db.get().addOnSuccessListener { snap ->
            val changes = snap.child("mainCardChanges").value?.toString()?.toIntOrNull() ?: 0
            if (changes >= maxMainChanges) {
                Toast.makeText(
                    this,
                    "You can only change your main card $maxMainChanges times.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }
            val walletBalance = snap.child("Balance").value?.toString()?.toDoubleOrNull() ?: 0.0
            // Absorb this card's own balance into the main balance so nothing is lost,
            // then the card mirrors the main balance from here on.
            val updates = mapOf(
                "Balance" to walletBalance + card.Balnce,
                "cards/${card.cardKey}/Balnce" to 0.0,
                "mainCardKey" to card.cardKey,
                "mainCardChanges" to changes + 1
            )
            db.updateChildren(updates)
                .addOnSuccessListener {
                    cachedMainCardKey = card.cardKey
                    val left = maxMainChanges - (changes + 1)
                    Toast.makeText(this, "This is now your main card · $left changes left", Toast.LENGTH_SHORT).show()
                    loadUserCardsQuiet()
                    loadWalletBalance()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Couldn't update main card", Toast.LENGTH_SHORT).show()
                }
        }
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
                            cachedMainCardKey = walletSnap.child("mainCardKey").value?.toString() ?: ""
                            // The wallet Balance is the one main balance; the main card mirrors it.
                            val balance = walletSnap.child("Balance").value?.toString()?.toDoubleOrNull() ?: 0.0
                            cachedWalletBalance = balance
                            binding.tvAvailableCredit.text = "$%,.2f".format(balance)
                            adapter.setMainInfo(cachedMainCardKey, balance)
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
                                cachedCards = cardList
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
        binding.tvTotalCards.text = if (count == 1) "1 card" else "$count cards"
        updateCarouselDots(count)
    }

    private fun updateCarouselDots(count: Int) {
        val on = R.drawable.onboarding_dot_active
        val off = R.drawable.onboarding_dot
        binding.dot1.setBackgroundResource(if (count >= 1) on else off)
        binding.dot2.setBackgroundResource(if (count >= 2) on else off)
        binding.dot3.setBackgroundResource(if (count >= 3) on else off)
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
                                cachedCards = cardList
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
