package bankal_deir.com.Login.repository

import androidx.lifecycle.liveData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    fun login(email: String, password: String) = liveData(Dispatchers.IO) {
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun resetPassword(email: String) = liveData(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}