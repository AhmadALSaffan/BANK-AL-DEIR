package bankal_deir.com.Login

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import bankal_deir.com.Login.ViewModel.LoginViewModel
import bankal_deir.com.Login.ViewModel.LoginViewModelFactory
import bankal_deir.com.Login.repository.AuthRepository
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityLoginWithPhoneNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.TotpMultiFactorAssertion
import com.google.firebase.database.FirebaseDatabase

class LoginWithPhoneNumber : AppCompatActivity() {
    private lateinit var binding: ActivityLoginWithPhoneNumberBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(AuthRepository())
    }
    private var existingUserId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityLoginWithPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeViewModel()

    }

    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Toast.makeText(this@LoginWithPhoneNumber, "Auto-verified!", Toast.LENGTH_SHORT).show()
        }

        override fun onVerificationFailed(e: FirebaseException) {
            showLoading(false)
            Toast.makeText(this@LoginWithPhoneNumber, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            PhoneVerificationManager.verificationId = verificationId
            PhoneVerificationManager.resendToken = token


            showLoading(false)

            val countryCode = binding.editFirstText.text.toString().trim()
            val phoneNumber = binding.editFullPhoneText.text.toString().trim()
            val fullPhoneNumber = countryCode + phoneNumber

            Toast.makeText(this@LoginWithPhoneNumber, "Code sent to $fullPhoneNumber", Toast.LENGTH_SHORT).show()
            navigateToVerifyScreen(fullPhoneNumber)
        }
    }

    private fun setupUI() {

        binding.getOTPPhoneBtn.setOnClickListener {

            val countryCode = binding.editFirstText.text.toString().trim()
            val phoneNumber = binding.editFullPhoneText.text.toString().trim()
            val fullPhoneNumber = countryCode + phoneNumber


            if (countryCode.isEmpty()) {
                Toast.makeText(this@LoginWithPhoneNumber, "Please enter country code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this@LoginWithPhoneNumber, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!fullPhoneNumber.startsWith("+")) {
                Toast.makeText(this@LoginWithPhoneNumber, "Phone number must start with +", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fullPhoneNumber.length < 10) {
                Toast.makeText(this@LoginWithPhoneNumber, "Phone number too short", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            Toast.makeText(this@LoginWithPhoneNumber, "Sending verification code...", Toast.LENGTH_SHORT).show()
            checkPhoneAndGetUserId(fullPhoneNumber)
        }

    }

    private fun observeViewModel() {
        viewModel.phoneAuthState.observe(this) { state ->

            when (state) {
                is LoginViewModel.PhoneAuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(this@LoginWithPhoneNumber, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {
                }
            }
        }

    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.getOTPPhoneBtn.isEnabled = !isLoading
        binding.editFirstText.isEnabled = !isLoading
        binding.editFullPhoneText.isEnabled = !isLoading
    }

    private fun navigateToVerifyScreen(phoneNumber: String) {
        val intent = Intent(this, VerifyPhoneNumber::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun checkPhoneAndGetUserId(phoneNumber: String) {

        val normalizedInput = phoneNumber.replace(Regex("[^0-9]"), "")
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        showLoading(true)

        usersRef.get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)
                if (!snapshot.exists()) {
                    sendCodeForNewUser(phoneNumber)
                    return@addOnSuccessListener
                }
                var foundUserId: String? = null
                for (child in snapshot.children) {
                    val dbPhoneRaw = child.child("phoneNumber").value as? String ?: continue
                    val dbPhoneNormalized = dbPhoneRaw.replace(Regex("[^0-9]"), "")
                    if (normalizedInput == dbPhoneNormalized) {
                        foundUserId = child.key
                        break
                    }
                }

                if (foundUserId != null) {
                    Toast.makeText(
                        this@LoginWithPhoneNumber,
                        "User found! Sending verification code...",
                        Toast.LENGTH_SHORT
                    ).show()

                    PhoneVerificationManager.phoneNumber = phoneNumber
                    PhoneVerificationManager.expectedUserId = foundUserId

                    viewModel.sendPhoneVerificationCode(
                        this@LoginWithPhoneNumber,
                        phoneNumber,
                        phoneAuthCallbacks
                    )
                } else {
                    sendCodeForNewUser(phoneNumber)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this@LoginWithPhoneNumber,
                    "Database error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendCodeForNewUser(phoneNumber: String) {
        Toast.makeText(
            this@LoginWithPhoneNumber,
            "Sending verification code for new user...",
            Toast.LENGTH_SHORT
        ).show()

        PhoneVerificationManager.phoneNumber = phoneNumber
        PhoneVerificationManager.expectedUserId = null

        viewModel.sendPhoneVerificationCode(
            this@LoginWithPhoneNumber,
            phoneNumber,
            phoneAuthCallbacks
        )
    }
}
