package bankal_deir.com.updateProfile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityUpdateProfileBinding
import bankal_deir.com.profilePage
import bankal_deir.com.updateProfile.ViewModel.updateViewModel
import bankal_deir.com.updateProfile.repository.AuthRepository
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.View


class updateProfile : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding
    private lateinit var viewModel: updateViewModel
    private var selectedImageUri: Uri? = null
    private lateinit var databaseReference: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }


        mAuth = FirebaseAuth.getInstance()
        val repository = AuthRepository()
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return updateViewModel(repository) as T
                }
            }
        )[updateViewModel::class.java]
        readData()

        // Set IME options for smooth "Next" navigation
        binding.edtFirstNameUpdate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.edtLastNameUpdate.requestFocus()
                scrollToView(binding.edtLastNameUpdate)
                true
            } else false
        }

        binding.edtLastNameUpdate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.edtPhoneNumberUpdate.requestFocus()
                scrollToView(binding.edtPhoneNumberUpdate)
                true
            } else false
        }

        binding.edtPhoneNumberUpdate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else false
        }



        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        binding.btnSaveUpdate.setOnClickListener {
            val progressDialog = Dialog(this)
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            progressDialog.setCancelable(false)
            progressDialog.setContentView(R.layout.progress)
            progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            progressDialog.show()
            val firstName = binding.edtFirstNameUpdate.text.toString()
            val lastName = binding.edtLastNameUpdate.text.toString()
            val phoneNumber = binding.edtPhoneNumberUpdate.text.toString()

            if (selectedImageUri!=null){
                viewModel.uploadProfileImage(selectedImageUri!!)
            }
            viewModel.updateProfile(firstName,lastName,phoneNumber).observe(this@updateProfile){result ->
                result.onSuccess {
                    progressDialog.dismiss()
                    val intent = Intent(this@updateProfile, MainPage::class.java)
                    startActivity(intent)
                    finish()
                }
                result.onFailure {exception->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Update failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }


        }
        binding.btnCancelUpdate.setOnClickListener {
            val intent = Intent(this, profilePage::class.java)
            startActivity(intent)
            finish()
        }
        viewModel.uploadImageResult.observe(this) { result ->
            result.onSuccess { imageUri ->
                Glide.with(this@updateProfile).load(imageUri).into(binding.profileImage)
                val intent = Intent(this@updateProfile, MainPage::class.java)
                startActivity(intent)
                finish()
            }
            result.onFailure {
                Toast.makeText(this@updateProfile, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scrollToView(view: View) {
        binding.ScrollView.post {
            binding.ScrollView.smoothScrollTo(0, view.top)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    fun readData(){
        val progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.show()
        val userID = mAuth.uid ?: return
       val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(userID).get().addOnSuccessListener {
            val firstName = it.child("firstName").value
            val lastName = it.child("lastName").value
            val phoneNumber = it.child("phoneNumber").value
            val email = it.child("email").value
            val profileImageUrl = it.child("profileImageUrl").value
            val fullName = firstName.toString()+" "+lastName.toString()
            binding.edtFirstNameUpdate.setText(firstName?.toString() ?: "")
            binding.edtLastNameUpdate.setText(lastName?.toString() ?: "")
            binding.edtPhoneNumberUpdate.setText(phoneNumber?.toString() ?: "")
            binding.txtName.setText(fullName)
            binding.txtEmail.setText(email?.toString() ?: "")
            if (profileImageUrl != null) {
                Glide.with(this)
                    .load(profileImageUrl.toString())
                    .into(binding.profileImage)
            }
            progressDialog.dismiss()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            Glide.with(this@updateProfile)
                .load(selectedImageUri)
                .into(binding.profileImage)
        }
    }
    companion object {
        private const val IMAGE_PICK_CODE = 101
    }

}
