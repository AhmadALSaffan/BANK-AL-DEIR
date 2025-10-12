package bankal_deir.com.Signup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class OTPViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _otpSent = MutableStateFlow(false)
    val otpSent : StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _verifyOtpState = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val verifyOtpState : StateFlow<Resource<Boolean>> = _verifyOtpState.asStateFlow()

    private val _signUpState = MutableStateFlow<Resource<FirebaseUser>>(Resource.Loading)
    val signUpState : StateFlow<Resource<FirebaseUser>> = _signUpState.asStateFlow()

    private var currentOtp: Int = 0
    private var email: String = ""
    private var password: String = ""
    private var firstName: String = ""
    private var lastName: String = ""
    private var phoneNumber: String = ""

    fun setUserData(email: String, password: String, firstName:String, lastName:String, phoneNumber:String) {
        this.email = email
        this.password = password
        this.firstName = firstName
        this.lastName = lastName
        this.phoneNumber = phoneNumber
    }

    fun sendOtp() {
        currentOtp = Random.nextInt(100000, 999999)
        // TODO: Implement your mail sending logic asynchronously here
        // For now, we assume it's sent successfully:
        _otpSent.value = true
    }

    fun verifyOtp(enteredOtp: Int) {
        if (enteredOtp == currentOtp) {
            _verifyOtpState.value = Resource.Success(true)
            signUp()
        } else {
            _verifyOtpState.value = Resource.Error("Wrong OTP")
        }
    }

    private fun signUp() {
        _signUpState.value = Resource.Loading
        repository.signUp(email, password) { result ->
            if (result is Resource.Success) {

                val firebaseUser = result.data
                val userId = firebaseUser?.uid ?: ""
                val iban = "SYR" + System.currentTimeMillis() + userId.take(5)
                val accountNumber = "ACC" + (1000000000..9999999999).random().toString()
                val walletId = "WAL" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)

                val userMap = mapOf(
                    "userId" to userId,
                    "iban" to iban,
                    "accountNumber" to accountNumber,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "phoneNumber" to phoneNumber,
                    "email" to email,
                    "walletId" to walletId
                )

                repository.saveUserData(userId, userMap) { success, error ->
                    if (success) {
                        val walletMap = mapOf(
                            "userId" to userId,
                            "walletNumber" to walletId,
                            "Balance" to 0
                        )
                        repository.saveWalletData(walletId, walletMap) { walletSuccess, walletError ->
                            if (walletSuccess) {
                                _signUpState.value = Resource.Success(firebaseUser)
                            } else {
                                _signUpState.value = Resource.Error(walletError ?: "Wallet saving error")
                            }
                        }
                    } else {
                        _signUpState.value = Resource.Error(error ?: "User data saving error")
                    }
                }
            } else {
                _signUpState.value = result
            }
        }
    }
}
