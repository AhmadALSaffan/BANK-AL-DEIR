package bankal_deir.com.Signup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _signUpState = MutableStateFlow<Resource<FirebaseUser>>(Resource.Loading)
    val signUpState: StateFlow<Resource<FirebaseUser>> = _signUpState.asStateFlow()

    fun signUp(email: String, password: String) {
        _signUpState.value = Resource.Loading
        repository.signUp(email, password) { result ->
            _signUpState.value = result
        }
    }
}
