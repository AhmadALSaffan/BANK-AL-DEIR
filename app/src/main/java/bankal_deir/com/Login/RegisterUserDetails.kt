package bankal_deir.com.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.Login.ViewModel.LoginViewModel
import bankal_deir.com.Login.ViewModel.LoginViewModelFactory
import bankal_deir.com.Login.repository.AuthRepository
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityRegisterUserDetailsBinding
import bankal_deir.com.pinPage.PinPage
import bankal_deir.com.pinPage.createPinCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.getValue

class RegisterUserDetails : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterUserDetailsBinding
    private var firebaseUserId: String? = null
    private var phoneNumber: String? = null
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(AuthRepository())
    }
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentId = currentUser?.uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterUserDetailsBinding.inflate(layoutInflater)
        val uId = FirebaseAuth.getInstance().currentUser!!.uid
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
        firebaseUserId = FirebaseAuth.getInstance().currentUser?.uid
        phoneNumber = intent.getStringExtra("phoneNumber")

        setupUI()
    }

    private fun setupUI() {
        binding.btnSaveDetails.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val firstName = binding.edtFirstNamePhone.text.toString().trim()
            val lastName = binding.edtLastNamePhone.text.toString().trim()
            val email = binding.edtEmailPhone.text.toString().trim()
            val phone = intent.getStringExtra("phoneNumber") ?: ""
            val check = binding.checkBox

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!check.isChecked) {
                Toast.makeText(this, "Please agree with the terms and conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.doesUserExist(userId).observe(this) { result ->
                if (result.isSuccess && result.getOrNull() == true) { navigateToPinCode() }
            }

            viewModel.saveUserProfile(userId, firstName, lastName, email).observe(this) { result ->
                result.onSuccess {
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .child("phoneNumber")
                        .setValue(phone)

                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                    navigateToCreatePinCode()
                }
                result.onFailure { e ->
                    Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToCreatePinCode() {
        val intent = Intent(this, bankal_deir.com.pinPage.createPinCode::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    fun navigateToPinCode() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("pin").get()
                .addOnSuccessListener { snapshot ->
                    val target = if (snapshot.exists())
                        PinPage::class.java else createPinCode::class.java
                    val intent = Intent(this, target)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
        }
    }
}