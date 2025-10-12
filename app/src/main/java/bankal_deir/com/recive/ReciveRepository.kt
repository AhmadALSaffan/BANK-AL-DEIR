package bankal_deir.com.recive

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ReciveRepository {
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserData(
        userId: String,
        onResult: (Map<String, Any?>?) -> Unit
    ) {
        database.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val data = mapOf(
                        "firstName" to snapshot.child("firstName").value,
                        "lastName" to snapshot.child("lastName").value,
                        "phoneNumber" to snapshot.child("phoneNumber").value,
                        "email" to snapshot.child("email").value,
                        "accountNumber" to snapshot.child("accountNumber").value,
                        "iban" to snapshot.child("iban").value
                    )
                    onResult(data)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }
}