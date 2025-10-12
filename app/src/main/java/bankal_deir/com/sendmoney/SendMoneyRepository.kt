package bankal_deir.com.sendmoney

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SendMoneyRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserWalletId(
        uid: String,
        onResult: (String?) -> Unit
    ) {
        database.child("users").child(uid).child("walletId").get()
            .addOnSuccessListener { snapshot ->
                val walletId = snapshot.value?.toString()
                onResult(walletId)
            }.addOnFailureListener { onResult(null) }
    }

    fun findReceiverWalletId(
        accountNumber: String,
        onResult: (String?) -> Unit
    ) {
        database.child("users").orderByChild("accountNumber").equalTo(accountNumber).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val walletId = child.child("walletId").value?.toString()
                    if (!walletId.isNullOrEmpty()) {
                        onResult(walletId)
                        return@addOnSuccessListener
                    }
                }
                onResult(null)
            }.addOnFailureListener { onResult(null) }
    }

    fun getWalletBalance(walletId: String, onResult: (Double?) -> Unit) {
        database.child("wallets").child(walletId).child("Balance").get()
            .addOnSuccessListener { snap ->
                val balance = snap.value?.toString()?.toDoubleOrNull()
                onResult(balance)
            }.addOnFailureListener { onResult(null) }
    }

    fun performTransfer(
        senderWalletId: String,
        receiverWalletId: String,
        amount: Double,
        onResult: (Boolean, String?) -> Unit
    ) {
        val walletRef = database.child("wallets")
        walletRef.child(senderWalletId).child("Balance").get().addOnSuccessListener { senderSnap ->
            val senderBalance = senderSnap.value?.toString()?.toDoubleOrNull() ?: 0.0
            if (senderBalance < amount) {
                onResult(false, "Insufficient balance")
                return@addOnSuccessListener
            }

            walletRef.child(receiverWalletId).child("Balance").get().addOnSuccessListener { receiverSnap ->
                val receiverBalance = receiverSnap.value?.toString()?.toDoubleOrNull() ?: 0.0
                val updateMap = hashMapOf<String, Any>(
                    "$senderWalletId/Balance" to (senderBalance - amount),
                    "$receiverWalletId/Balance" to (receiverBalance + amount)
                )
                walletRef.updateChildren(updateMap).addOnSuccessListener {
                    onResult(true, null)
                }.addOnFailureListener { onResult(false, it.message) }
            }.addOnFailureListener { onResult(false, it.message) }
        }.addOnFailureListener { onResult(false, it.message) }
    }

    fun saveTransaction(
        senderWalletID: String,
        receiverWalletID: String,
        amount: Double
    ): String {
        val historyRef = database.child("history")
        val transactionNumber = "SYP${System.currentTimeMillis()}${(1000..9999).random()}"
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val data = mapOf(
            "transactionNumber" to transactionNumber,
            "senderWalletID" to senderWalletID,
            "receiverWalletID" to receiverWalletID,
            "amount" to amount,
            "date" to date,
            "senderUserId" to auth.currentUser?.uid
        )
        historyRef.child(transactionNumber).setValue(data)
        return transactionNumber
    }
}
