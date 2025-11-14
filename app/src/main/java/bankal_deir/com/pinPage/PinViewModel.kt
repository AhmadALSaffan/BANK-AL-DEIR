package bankal_deir.com.pinPage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import bankal_deir.com.security.PinHasher
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

        // Hash the PIN before saving
        val hashedPin = PinHasher.hashPin(pin)

        databaseRef.child(userId).child("pin").setValue(hashedPin)
            .addOnSuccessListener {
                pinStatus.value = true
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to save PIN: ${e.message}"
                pinStatus.value = false
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
                    val savedHash = snapshot.value.toString()
                    // Verify the entered PIN against the stored hash
                    if (PinHasher.verifyPin(enteredPin, savedHash)) {
                        pinStatus.value = true
                    } else {
                        errorMessage.value = "PIN is not correct"
                        pinStatus.value = false
                    }
                } else {
                    errorMessage.value = "No PIN found. Please set up your PIN first"
                    pinStatus.value = false
                }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to verify PIN: ${e.message}"
                pinStatus.value = false
            }
    }
}
