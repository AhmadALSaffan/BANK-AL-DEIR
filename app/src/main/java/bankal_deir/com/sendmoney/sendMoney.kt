package bankal_deir.com.sendmoney

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivitySendMoneyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class sendMoney : AppCompatActivity() {
    private lateinit var binding: ActivitySendMoneyBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        binding = ActivitySendMoneyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val items = listOf("Select reason", "Personal expenses", "Family and friends expenses", "Tuition fees","individual","Other")
        val spinnerAdapter = ArrayAdapter(this, R.layout.custom_color_spinner, items)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_layout)

        binding.aspinnerReason.adapter = spinnerAdapter
        binding.aspinnerReason.setSelection(0)

        binding.aspinnerReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = items[position]
                if (position == 0) {

                } else {
                    val selectedItem = items[position]
                    Toast.makeText(this@sendMoney, "Selected: $selected", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }


        readDataUser()

        binding.btnSendMoney.setOnClickListener {
            val receiverAccountNumber = binding.edtAcountNumberWallet.text.toString().trim()
            val amountText = binding.edtAmount.text.toString().trim()

            Log.d("SendMoney", "Searching for account number: '$receiverAccountNumber'")

            if (receiverAccountNumber.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, " Please enter account number and amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, " Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = mAuth.currentUser?.uid ?: run {
                Toast.makeText(this, " User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val progressDialog = Dialog(this)
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            progressDialog.setCancelable(false)
            progressDialog.setContentView(R.layout.progress)
            progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            progressDialog.show()

            val senderUserRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            senderUserRef.child("walletId").get().addOnSuccessListener { senderSnapshot ->
                val senderWID = senderSnapshot.value?.toString() ?: ""
                if (senderWID.isEmpty()) {
                    Toast.makeText(this, " Sender wallet not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }


                findReceiverByAccountNumber(receiverAccountNumber) { receiverWID ->
                    if (receiverWID.isNotEmpty()) {
                        if (senderWID == receiverWID) {
                            progressDialog.dismiss()
                            Toast.makeText(this, " You cannot send money to yourself", Toast.LENGTH_SHORT).show()
                            return@findReceiverByAccountNumber
                        }
                        transferMoney(senderWID, receiverWID, amount,progressDialog) { transactionNumberStr ->
                            progressDialog.dismiss()
                            val dialog = Dialog(this)
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            dialog.setCancelable(false)
                            dialog.setContentView(R.layout.done)
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.show()

                            val transactionNumber = dialog.findViewById<TextView>(R.id.transactionNumber)
                            val finalAmount = dialog.findViewById<TextView>(R.id.finalAmount)
                            val txtDate = dialog.findViewById<TextView>(R.id.txtDate)
                            val btnOk = dialog.findViewById<Button>(R.id.btnOk)

                            val historyRef = FirebaseDatabase.getInstance().getReference("history")

                            historyRef.child(transactionNumberStr)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val transactionNumberVal = snapshot.child("transactionNumber").getValue(String::class.java) ?: ""
                                        val amountVal = snapshot.child("amount").getValue(Double::class.java) ?: 0.0
                                        val receiverWalletVal = snapshot.child("receiverWalletID").getValue(String::class.java) ?: ""
                                        val dateVal = snapshot.child("date").getValue(String::class.java) ?: ""
                                        transactionNumber?.text = transactionNumberVal
                                        finalAmount?.text = amountVal.toString() + "$"
                                        txtDate?.text = dateVal
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        dialog.dismiss()
                                        Toast.makeText(this@sendMoney, "Failed to read transaction: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })

                            btnOk?.setOnClickListener {
                                val intent = Intent(this, MainPage::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, " Receiver not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Log.e("SendMoney", "Failed to fetch sender: ${it.message}")
                Toast.makeText(this, " Failed to fetch sender: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun findReceiverByAccountNumber(accountNumber: String, callback: (String) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("accountNumber").equalTo(accountNumber).get()
            .addOnSuccessListener { receiverSnap ->
                if (receiverSnap.exists()) {
                    Log.d("SendMoney", "Found receiver using orderByChild")
                    for (child in receiverSnap.children) {
                        val receiverWID = child.child("walletId").value?.toString() ?: ""
                        if (receiverWID.isNotEmpty()) {
                            callback(receiverWID)
                            return@addOnSuccessListener
                        }
                    }
                    callback("")
                } else {
                    findReceiverManually(accountNumber, callback)
                }
            }
            .addOnFailureListener {
                findReceiverManually(accountNumber, callback)
            }
    }

    private fun findReceiverManually(accountNumber: String, callback: (String) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val userAccountNumber = child.child("accountNumber").value?.toString()?.trim() ?: ""
                    if (userAccountNumber == accountNumber) {
                        val walletId = child.child("walletId").value?.toString() ?: ""
                        if (walletId.isNotEmpty()) {
                            callback(walletId)
                            return
                        }
                    }
                }
                callback("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SendMoney", "Manual search cancelled: ${error.message}")
                callback("")
            }
        })
    }

    fun readDataUser() {
        val userID = mAuth.uid ?: return
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(userID).get().addOnSuccessListener {
            val wallet = it.child("walletId").value?.toString() ?: ""
            if (wallet.isNotEmpty()) readBalance(wallet)
        }
    }

    fun readBalance(walletId: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("wallets")
        databaseReference.child(walletId).get().addOnSuccessListener {
            val balance = it.child("Balance").value?.toString() ?: "0.0"
            val balanceDouble = balance?.toString()?.toDoubleOrNull() ?: 0.0
            val formattedBalance = "%.2f".format(balanceDouble)
            binding.txtBalance.text = "$formattedBalance$"
        }
    }

    fun transferMoney(senderWalletID: String, receiverWalletID: String, amount: Double, progressDialog: Dialog, onSuccess: (String) -> Unit) {
        databaseReference = FirebaseDatabase.getInstance().getReference("wallets")
        databaseReference.child(senderWalletID).get().addOnSuccessListener { senderSnapshot ->
            val senderBalance = senderSnapshot.child("Balance").getValue(Double::class.java) ?: 0.0
            if (senderBalance < amount) {
                progressDialog.dismiss()
                Toast.makeText(this, "Transfer failed: Insufficient balance", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            databaseReference.child(receiverWalletID).get().addOnSuccessListener { receiverSnapshot ->
                val receiverBalance = receiverSnapshot.child("Balance").getValue(Double::class.java) ?: 0.0
                val updateBalances = hashMapOf<String, Any>(
                    "$senderWalletID/Balance" to (senderBalance - amount),
                    "$receiverWalletID/Balance" to (receiverBalance + amount)
                )
                databaseReference.updateChildren(updateBalances).addOnSuccessListener {
                    Toast.makeText(this, " Transfer of $amount successful!", Toast.LENGTH_SHORT).show()
                    readDataUser()
                    val transactionNumber = saveTransactionHistory(senderWalletID, receiverWalletID, amount)
                    onSuccess(transactionNumber)
                }.addOnFailureListener {
                    Toast.makeText(this, " Transfer failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, " Failed to fetch receiver wallet: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, " Failed to fetch sender wallet: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTransactionHistory(senderWalletID: String, receiverWalletID: String, amount: Double): String {
        val historyRef = FirebaseDatabase.getInstance().getReference("history")
        val transactionNumber = "SYP" + System.currentTimeMillis() + (1000..9999).random()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date())
        val transactionData = mapOf(
            "transactionNumber" to transactionNumber,
            "senderWalletID" to senderWalletID,
            "receiverWalletID" to receiverWalletID,
            "amount" to amount,
            "date" to date,
            "senderUserId" to mAuth.currentUser?.uid
        )
        historyRef.child(transactionNumber).setValue(transactionData)
        return transactionNumber
    }
    override fun onResume() {
        super.onResume()
        val accountNumber = intent.getStringExtra("account_number")
        val editText = binding.edtAcountNumberWallet
        if (accountNumber != null) {
            editText.setText(accountNumber)
        }
    }
}