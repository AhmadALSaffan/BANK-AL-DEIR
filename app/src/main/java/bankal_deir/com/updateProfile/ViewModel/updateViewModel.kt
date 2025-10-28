package bankal_deir.com.updateProfile.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import bankal_deir.com.updateProfile.repository.AuthRepository

class updateViewModel(private val repository: AuthRepository): ViewModel() {
    fun updateProfile(firstName:String,lastName:String,phoneNumber: String) = repository.updateProfile(phoneNumber,firstName,lastName)
    private val _uploadImageResult = MutableLiveData<Result<String>>()
    val uploadImageResult : LiveData<Result<String>> = _uploadImageResult
    fun uploadProfileImage(imageUri: Uri){
        repository.uploadProfileImage(imageUri){ downloadUri ->
            if (downloadUri != null){
                _uploadImageResult.value = Result.success(downloadUri)
            }
            if (downloadUri == null){
                _uploadImageResult.value = Result.failure(Exception("Image upload failed"))
            }
        }
    }
}