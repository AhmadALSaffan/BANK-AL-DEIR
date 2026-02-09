package bankal_deir.com.Fatora

import bankal_deir.com.Fatora.Data.PaymentTransaction
import com.google.firebase.database.*

class PaymentRepository {
    private val database = FirebaseDatabase.getInstance()
    private val historyRef = database.getReference("history")

    fun saveTransaction(
        transaction: PaymentTransaction,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        historyRef
            .child(transaction.transactionNumber)
            .setValue(transaction)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserTransactions(
        userId: String,
        onSuccess: (List<PaymentTransaction>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        historyRef
            .orderByChild("senderUserId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = mutableListOf<PaymentTransaction>()
                    snapshot.children.forEach {
                        it.getValue(PaymentTransaction::class.java)?.let { tx ->
                            transactions.add(tx)
                        }
                    }
                    onSuccess(transactions.sortedByDescending { it.date })
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.toException())
                }
            })
    }


    fun getTransactionsByType(
        userId: String,
        transactionType: String,
        onSuccess: (List<PaymentTransaction>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        historyRef
            .orderByChild("senderUserId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = mutableListOf<PaymentTransaction>()
                    snapshot.children.forEach {
                        it.getValue(PaymentTransaction::class.java)?.let { tx ->
                            if (tx.transactionType == transactionType) {
                                transactions.add(tx)
                            }
                        }
                    }
                    onSuccess(transactions.sortedByDescending { it.date })
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.toException())
                }
            })
    }

    fun getTransaction(
        transactionNumber: String,
        onSuccess: (PaymentTransaction?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        historyRef
            .child(transactionNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transaction = snapshot.getValue(PaymentTransaction::class.java)
                    onSuccess(transaction)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.toException())
                }
            })
    }
}