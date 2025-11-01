package bankal_deir.com.Login.ViewModel

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import bankal_deir.com.Login.repository.AuthRepository
import bankal_deir.com.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import com.google.firebase.auth.OAuthProvider

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String) = repository.login(email, password)
    fun resetPassword(email: String) = repository.resetPassword(email)

    fun signInWithGoogle(credential: AuthCredential): LiveData<Result<Unit>> {
        val liveData = MutableLiveData<Result<Unit>>()
        liveData.value = Result.failure(Exception("Loading"))

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    val user = firebaseAuth.currentUser
                    if (isNewUser && user != null) {
                        createUserInDatabase(user).addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                liveData.value = Result.success(Unit)
                            } else {
                                liveData.value = Result.failure(profileTask.exception ?: Exception("Create profile failed"))
                            }
                        }
                    } else {
                        liveData.value = Result.success(Unit)
                    }
                } else {
                    val exception = task.exception
                    Log.e("FirebaseAuth", "Google sign in failed", exception)
                    liveData.value = Result.failure(exception ?: Exception("Authentication failed"))
                }
            }
        return liveData
    }

    fun signInWithGitHub(activity: Activity): LiveData<Result<Unit>> {
        val liveData = MutableLiveData<Result<Unit>>()
        val dialog = Dialog(activity).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.progress)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }


        val provider = OAuthProvider.newBuilder("github.com")
        provider.scopes = listOf("user:email")

        val pendingResultTask = firebaseAuth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    handleAuthResult(authResult, liveData)
                }
                .addOnFailureListener { e ->
                    liveData.value = Result.failure(e)
                }
        } else {
            firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener { authResult ->
                    checkAndCreateUser(authResult, liveData)
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    liveData.value = Result.failure(e)
                    dialog.dismiss()
                }
        }
        return liveData
    }

    private fun handleAuthResult(authResult: AuthResult, liveData: MutableLiveData<Result<Unit>>) {
        val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
        val user = firebaseAuth.currentUser
        if (isNewUser && user != null) {
            createUserInDatabase(user).addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                    liveData.value = Result.success(Unit)
                } else {
                    liveData.value = Result.failure(profileTask.exception ?: Exception("Create profile failed"))
                }
            }
        } else {
            liveData.value = Result.success(Unit)
        }
    }

    private fun checkAndCreateUser(authResult: AuthResult, liveData: MutableLiveData<Result<Unit>>) {
        val user = authResult.user ?: run {
            liveData.value = Result.failure(Exception("User is null"))
            return
        }

        val usersRef = FirebaseDatabase.getInstance().reference.child("users")
        usersRef.orderByChild("email").equalTo(user.email).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                liveData.value = Result.success(Unit)
            } else {
                createUserInDatabase(user).addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        liveData.value = Result.success(Unit)
                    } else {
                        liveData.value = Result.failure(profileTask.exception ?: Exception("Create profile failed"))
                    }
                }
            }
        }.addOnFailureListener { e ->
            liveData.value = Result.failure(e)
        }
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

        val userMap = mapOf(
            "userId" to uid,
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
