package bankal_deir.com.Login.ViewModel

import androidx.lifecycle.ViewModel
import bankal_deir.com.Login.repository.AuthRepository

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    fun login(email: String, password: String) = repository.login(email, password)
    fun resetPassword(email: String) = repository.resetPassword(email)
}
