package bankal_deir.com.Login.repository

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import bankal_deir.com.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.auth.OAuthProvider

class AuthRepository {
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentId = currentUser?.uid

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDb = FirebaseDatabase.getInstance().reference
    fun login(email: String, password: String) = liveData(Dispatchers.IO) {
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun saveUserProfile(userId: String, firstName: String, lastName: String, email: String) = liveData(Dispatchers.IO) {
        try {
            val iban = generateIBAN(userId)
            val accountNumber = generateAccountNumber()
            val walletId = generateWalletId()
            val userMap = mapOf(
                "userId" to userId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "accountNumber" to accountNumber,
                "iban" to iban,
                "walletId" to walletId
            )
            val walletMap = mapOf(
                "userId" to userId,
                "walletNumber" to walletId,
                "Balance" to 0
            )
            firebaseDb.child("wallets").child(walletId).setValue(walletMap).await()


            firebaseDb.child("users").child(userId).setValue(userMap).await()
            emit(Result.success(true))

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun generateIBAN(userId: String): String {
        return "SYR${System.currentTimeMillis()}${userId.take(5)}"
    }

    private fun generateAccountNumber(): String {
        return "ACC" + (1000000000..9999999999).random().toString()
    }

    private fun generateWalletId(): String {
        return "WAL" + UUID.randomUUID().toString().replace("-", "").take(12)
    }


    fun resetPassword(email: String) = liveData(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    //========== Google Login ==========
    fun signInWithGoogle(activity: Activity,credential: AuthCredential): LiveData<Result<Unit>> {
        val liveData = MutableLiveData<Result<Unit>>()
        liveData.value = Result.failure(Exception("Loading"))
        val dialog = Dialog(activity).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.progress)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    val user = firebaseAuth.currentUser
                    if (isNewUser && user != null) {
                        createUserInDatabase(user).addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                liveData.value = Result.success(Unit)
                                dialog.dismiss()
                            } else {
                                liveData.value = Result.failure(profileTask.exception ?: Exception("Create profile failed"))
                                dialog.dismiss()
                            }
                        }
                    } else {
                        liveData.value = Result.success(Unit)
                        dialog.dismiss()
                    }
                } else {
                    val exception = task.exception
                    Log.e("FirebaseAuth", "Google sign in failed", exception)
                    liveData.value = Result.failure(exception ?: Exception("Authentication failed"))
                }
            }
        return liveData
    }



    // ========== PHONE LOGIN ==========
    //function to check if the user exists in the database
    fun checkUserExists(phoneNumber: String) = liveData(Dispatchers.IO) {
        try {
            val snapshot = firebaseDb.child("users").get().await()

            var userExists = false
            for (userSnapshot in snapshot.children) {
                val phone = userSnapshot.child("phoneNumber").getValue(String::class.java)
                if (phone == phoneNumber) {
                    userExists = true
                    break
                }
            }

            emit(Result.success(userExists))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    // function to verify OTP code and sign in user with phone credential
    fun verifyPhoneCode(credential: PhoneAuthCredential) = liveData(Dispatchers.IO) {
        try {
            firebaseAuth.signInWithCredential(credential).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    //Fetch user data from Realtime Database after successful phone authentication
    fun getUserData(userId: String) = liveData(Dispatchers.IO) {
        try {
            val snapshot = firebaseDb.child("users").child(userId).get().await()

            if (snapshot.exists()) {
                val userData = snapshot.value as? Map<String, Any>
                emit(Result.success(userData))
            }
            if (!snapshot.exists()) {
                emit(Result.failure(Exception("User profile not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    //Get user by phone number from Realtime Database
    fun getUserByPhoneNumber(phoneNumber: String) = liveData(Dispatchers.IO) {
        try {
            val snapshot = firebaseDb.child("users").get().await()

            var userData: Map<String, Any>? = null
            var userId: String? = null

            for (userSnapshot in snapshot.children) {
                val phone = userSnapshot.child("phoneNumber").getValue(String::class.java)
                if (phone == phoneNumber) {
                    userId = userSnapshot.key
                    userData = userSnapshot.value as? Map<String, Any>
                    break
                }
            }

            if (userData != null && userId != null) {
                val result = mapOf("userId" to userId, "userData" to userData)
                emit(Result.success(result))
            } else {
                emit(Result.failure(Exception("User not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    fun signOut() {
        firebaseAuth.signOut()
    }


    private fun createUserInDatabase(user: FirebaseUser): Task<Void> {
        val uid = user.uid
        val email = user.email ?: ""
        val nameParts = user.displayName?.split(" ") ?: listOf("", "")
        val firstName = nameParts.getOrElse(0) { "" }
        val lastName = nameParts.getOrElse(1) { "" }
        val iban = "SYR${System.currentTimeMillis()}${uid.take(5)}"
        val accountNumber = "ACC" + (1000000000..9999999999).random().toString()
        val walletId = "WAL" + UUID.randomUUID().toString().replace("-", "").take(12)
        val userToken = user.getIdToken(true)

        val userMap = mapOf(
            "userId" to uid,
            "userToekn" to userToken,
            "iban" to iban,
            "accountNumber" to accountNumber,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to "",
            "email" to email,
            "walletId" to walletId
        )

        return FirebaseDatabase.getInstance().reference.child("users").child(uid).setValue(userMap).continueWithTask { task ->
            if (task.isSuccessful) {
                val walletMap = mapOf(
                    "userId" to uid,
                    "walletNumber" to walletId,
                    "Balance" to 0
                )
                FirebaseDatabase.getInstance().reference.child("wallets").child(walletId).setValue(walletMap)
            } else {
                throw task.exception ?: Exception("Failed to create user profile")
            }
        }
    }

}