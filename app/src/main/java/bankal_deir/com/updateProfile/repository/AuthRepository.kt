package bankal_deir.com.updateProfile.repository

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentId = currentUser?.uid
    fun updateProfile(phoneNumber:String,firstName:String,lastName: String): LiveData<Result<Boolean>>{
       val result = MutableLiveData<Result<Boolean>>()
        val userUpdate = mapOf(
            "firstName"   to firstName,
            "lastName"    to lastName,
            "phoneNumber" to phoneNumber
        )
        FirebaseDatabase.getInstance().getReference("users").child(currentId.toString())
            .updateChildren(userUpdate).addOnCompleteListener { task ->
                if (task.isSuccessful){
                    result.value = Result.success(true)
                }
                if (!task.isSuccessful){
                    result.value = Result.failure(task.exception?: Exception("Profile Update Failed"))
                }
            }
        return result
    }
    fun uploadProfileImage(uri: Uri,onResult: (String?)-> Unit){
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val storageReg = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
        storageReg.putFile(uri).addOnSuccessListener {
            storageReg.downloadUrl.addOnSuccessListener { downloadUrl ->
                FirebaseDatabase.getInstance().getReference("users").child(userId.toString()).child("profileImageUrl")
                    .setValue(downloadUrl.toString())
                onResult(downloadUrl.toString())
            }.addOnFailureListener {
                onResult(null)
            }
        }
    }
}