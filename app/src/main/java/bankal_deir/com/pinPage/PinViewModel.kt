package bankal_deir.com.pinPage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class PinViewModel : ViewModel() {
    private val databaseRef = FirebaseDatabase.getInstance().getReference("users")
    val pinStatus = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    fun savePin(userId: String, pin: String) {
        if (userId.isEmpty()) {
            errorMessage.value = "User ID is empty"
            return
        }

        databaseRef.child(userId).child("pin").setValue(pin)
            .addOnSuccessListener {
                pinStatus.value = true
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to save PIN: ${e.message}"
            }
    }

    fun verifyPin(userId: String, enteredPin: String) {
        if (userId.isEmpty()) {
            errorMessage.value = "User ID is empty"
            return
        }

        if (enteredPin.isEmpty() || enteredPin.length != 4) {
            errorMessage.value = "Please enter a valid 4-digit PIN"
            return
        }

        databaseRef.child(userId).child("pin").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.value != null) {
                    val savedPin = snapshot.value.toString()
                    if (enteredPin == savedPin) {
                        pinStatus.value = true
                    } else {
                        errorMessage.value = "PIN is not correct"
                    }
                } else {
                    errorMessage.value = "No PIN found. Please set up your PIN first."
                }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to verify PIN: ${e.message}"
            }
    }
}
