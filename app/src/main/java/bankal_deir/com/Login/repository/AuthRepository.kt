package bankal_deir.com.Login.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import bankal_deir.com.Login.ViewModel.LoginViewModel


class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String): LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) result.value = Result.success(true)
                else result.value = Result.failure(it.exception ?: Exception("Login failed"))
            }
        return result
    }

    fun resetPassword(email: String): LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener {
                if (it.isSuccessful) result.value = Result.success(true)
                else result.value = Result.failure(it.exception ?: Exception("Reset failed"))
            }
        return result
    }
}
