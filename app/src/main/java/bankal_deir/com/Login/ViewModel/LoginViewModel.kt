package bankal_deir.com.Login.ViewModel

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import bankal_deir.com.Login.repository.AuthRepository
import bankal_deir.com.R
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    fun login(email: String, password: String) = repository.login(email, password)
    fun resetPassword(email: String) = repository.resetPassword(email)



    fun checkUserExists(phoneNumber: String) = repository.checkUserExists(phoneNumber)
    fun verifyPhoneCode(credential: AuthCredential) =
        repository.verifyPhoneCode(credential as PhoneAuthCredential)

    fun saveUserProfile(userId: String, firstName: String, lastName: String, email: String) =
        repository.saveUserProfile(userId, firstName, lastName, email)


    private val _phoneAuthState = MutableLiveData<PhoneAuthState>()
    val phoneAuthState: LiveData<PhoneAuthState> get() = _phoneAuthState

    private val _phoneNumber = MutableLiveData<String>()
    var verificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    sealed class PhoneAuthState {
        object Loading : PhoneAuthState()
        object Ready : PhoneAuthState()
        data class CodeSent(val verificationId: String) : PhoneAuthState()
        object CodeVerified : PhoneAuthState()
        data class Error(val message: String) : PhoneAuthState()
    }

    fun doesUserExist(uid: String) = liveData(Dispatchers.IO) {
        try {
            val firebaseDb = FirebaseDatabase.getInstance().reference
            val snapshot = firebaseDb.child("users").child(uid).get().await()
            emit(Result.success(snapshot.exists()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun signInWithGoogle(activity: Activity, credential: AuthCredential): LiveData<Result<Unit>> {
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
                                liveData.value = Result.failure(
                                    profileTask.exception ?: Exception("Create profile failed")
                                )
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

    fun signInWithGitHub(activity: Activity): LiveData<Result<Unit>> {
        val liveData = MutableLiveData<Result<Unit>>()
        liveData.value = Result.failure(Exception("Loading"))


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
                }
                .addOnFailureListener { e ->
                    liveData.value = Result.failure(e)
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
                    liveData.value =
                        Result.failure(profileTask.exception ?: Exception("Create profile failed"))
                }
            }
        } else {
            liveData.value = Result.success(Unit)
        }
    }

    fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    ) {
        _phoneAuthState.value = PhoneAuthState.Loading
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks ?: phoneAuthCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun setCodeSent(verificationId: String) {
        this.verificationId = verificationId
        _phoneAuthState.postValue(PhoneAuthState.CodeSent(verificationId))
    }

    fun setPhoneAuthError(msg: String) {
        _phoneAuthState.postValue(PhoneAuthState.Error(msg))
    }

    fun verifyCode(code: String) {
        val vid = verificationId
        if (vid == null) {
            _phoneAuthState.value =
                PhoneAuthState.Error("No verification ID. Please request code again.")
            return
        }
        if (code.isEmpty() || code.length != 6) {
            _phoneAuthState.value = PhoneAuthState.Error("Please enter a valid 6-digit code")
            return
        }
        _phoneAuthState.value = PhoneAuthState.Loading

        try {
            val credential = PhoneAuthProvider.getCredential(vid, code)

            // Sign in with credential
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _phoneAuthState.value = PhoneAuthState.CodeVerified
                    } else {
                        _phoneAuthState.value = PhoneAuthState.Error(
                            task.exception?.message ?: "Code verification failed"
                        )
                    }
                }
        } catch (e: Exception) {
            _phoneAuthState.value = PhoneAuthState.Error("Invalid code: ${e.message}")
        }
    }


    fun resendVerificationCode(activity: Activity, phoneNumber: String) {
        if (resendToken == null) {
            _phoneAuthState.value = PhoneAuthState.Error("Cannot resend. Please try again later.")
            return
        }

        _phoneAuthState.value = PhoneAuthState.Loading

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(phoneAuthCallbacks)
            .setForceResendingToken(resendToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val phoneAuthCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}
            override fun onVerificationFailed(e: FirebaseException) {}
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {}
        }

    fun checkPhoneExists(phoneNumber: String) {
        _phoneAuthState.value = PhoneAuthState.Loading

        FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("phone")
            .equalTo(phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {
                    _phoneAuthState.value = PhoneAuthState.Ready
                } else {
                    _phoneAuthState.value = PhoneAuthState.Ready
                }
            }
            .addOnFailureListener { e ->
                _phoneAuthState.value = PhoneAuthState.Error("Error checking user: ${e.message}")
            }
    }


    private fun checkAndCreateUser(authResult: AuthResult, liveData: MutableLiveData<Result<Unit>>) {
        val user = authResult.user ?: run {
            liveData.value = Result.failure(Exception("User is null"))
            return
        }
        val usersRef = FirebaseDatabase.getInstance().reference.child("users")
        usersRef.orderByChild("email").equalTo(user.email).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    liveData.value = Result.success(Unit)
                } else {
                    createUserInDatabase(user)
                        .addOnSuccessListener {
                            liveData.value = Result.success(Unit)
                        }
                        .addOnFailureListener { e ->
                            liveData.value = Result.failure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
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
        val accountNumber = "ACC${(1000000000..9999999999).random()}"
        val walletId = "WAL${UUID.randomUUID().toString().replace("-", "").take(12)}"

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

        val databaseRef = FirebaseDatabase.getInstance().reference

        return databaseRef.child("users").child(uid).setValue(userMap)
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    val walletMap = mapOf(
                        "userId" to uid,
                        "walletNumber" to walletId,
                        "balance" to 0
                    )
                    databaseRef.child("wallets").child(walletId).setValue(walletMap)
                } else {
                    throw task.exception ?: Exception("Failed to create user profile")
                }
            }
    }
}