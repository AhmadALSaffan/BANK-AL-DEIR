package bankal_deir.com.Login

import com.google.firebase.auth.PhoneAuthProvider

object PhoneVerificationManager {
    var verificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    var phoneNumber: String? = null
    var expectedUserId: String? = null

    fun clear() {
        verificationId = null
        resendToken = null
        phoneNumber = null
        expectedUserId = null
    }
}