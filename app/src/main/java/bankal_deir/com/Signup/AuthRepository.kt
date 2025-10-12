package bankal_deir.com.Signup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun signUp(email: String, password: String, onComplete: (Resource<FirebaseUser>) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            onComplete(Resource.Error("Email and password are required"))
            return
        }
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { user ->
                        onComplete(Resource.Success(user))
                    }
                } else {
                    onComplete(Resource.Error(task.exception?.message ?: "Unknown error"))
                }
            }
    }

    fun saveUserData(
        userId: String,
        userMap: Map<String, Any>,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        database.child("users").child(userId).setValue(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun saveWalletData(
        walletId: String,
        walletMap: Map<String, Any>,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        database.child("wallets").child(walletId).setValue(walletMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}
