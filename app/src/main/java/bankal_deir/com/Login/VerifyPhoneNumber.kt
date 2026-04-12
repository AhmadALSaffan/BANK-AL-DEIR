package bankal_deir.com.Login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import bankal_deir.com.Login.ViewModel.LoginViewModel
import bankal_deir.com.Login.ViewModel.LoginViewModelFactory
import bankal_deir.com.Login.repository.AuthRepository
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityVerifyPhoneNumberBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class VerifyPhoneNumber : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyPhoneNumberBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(AuthRepository())
    }
    private var phoneNumber: String = ""
    private var resendCountdownTimer: CountDownTimer? = null
    private val RESEND_TIMEOUT_SECONDS = 60L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityVerifyPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""

        setupUI()
        observeViewModel()
    }

    private fun navigateAfterVerification() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = firebaseUser.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        showLoading(true)

        usersRef.get().addOnSuccessListener { snapshot ->
            showLoading(false)
            if (snapshot.exists()) {
                val intent = Intent(this, bankal_deir.com.pinPage.PinPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, RegisterUserDetails::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    fun startResendCountdown() {
        resendCountdownTimer?.cancel()
        resendCountdownTimer = object : CountDownTimer(RESEND_TIMEOUT_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.btnResendOtpPhone.text = "Resend Code(${secondsRemaining}s)"
                binding.btnResendOtpPhone.isEnabled = false
            }

            override fun onFinish() {
                binding.btnResendOtpPhone.text = "Resend Code"
                binding.btnResendOtpPhone.isEnabled = true
            }
        }.start()
    }

    private fun observeViewModel() {
        viewModel.phoneAuthState.observe(this) { state ->
            when(state) {
                is LoginViewModel.PhoneAuthState.Loading -> {
                    showLoading(true)
                }
                is LoginViewModel.PhoneAuthState.CodeVerified -> {
                    showLoading(false)
                    Toast.makeText(this@VerifyPhoneNumber,"Verified!", Toast.LENGTH_SHORT).show()
                    navigateAfterVerification()
                }
                is LoginViewModel.PhoneAuthState.CodeSent -> {
                    showLoading(false)
                    Toast.makeText(this@VerifyPhoneNumber,"Code resent successfully!", Toast.LENGTH_SHORT).show()
                    startResendCountdown()
                }
                is LoginViewModel.PhoneAuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(this@VerifyPhoneNumber," ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun setupUI() {

        binding.phoneOtp1.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 1) binding.phoneOtp2.requestFocus()
        }
        binding.phoneOtp2.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.phoneOtp3.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.phoneOtp1.requestFocus()
            }
        }
        binding.phoneOtp3.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.phoneOtp4.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.phoneOtp2.requestFocus()
            }
        }
        binding.phoneOtp4.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.phoneOtp5.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.phoneOtp3.requestFocus()
            }
        }
        binding.phoneOtp5.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.phoneOtp6.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.phoneOtp4.requestFocus()
            }
        }
        binding.phoneOtp6.doOnTextChanged { text, _, before, _ ->
            if (text.isNullOrEmpty() && before == 1) binding.phoneOtp5.requestFocus()
        }


        binding.showPhone.text = phoneNumber

        binding.btnResendOtpPhone.setOnClickListener {
            binding.btnResendOtpPhone.isEnabled = false
            Toast.makeText(this@VerifyPhoneNumber,"resending code...", Toast.LENGTH_SHORT).show()
            viewModel.resendVerificationCode(this, phoneNumber)
            startResendCountdown()
        }

        binding.btnLoginAfterOTPPhone.setOnClickListener {

            val otp1 = binding.phoneOtp1.text.toString()
            val otp2 = binding.phoneOtp2.text.toString()
            val otp3 = binding.phoneOtp3.text.toString()
            val otp4 = binding.phoneOtp4.text.toString()
            val otp5 = binding.phoneOtp5.text.toString()
            val otp6 = binding.phoneOtp6.text.toString()

            val code = "$otp1$otp2$otp3$otp4$otp5$otp6"

            val verificationId = PhoneVerificationManager.verificationId

            if (code.isEmpty()) {
                Toast.makeText(this@VerifyPhoneNumber, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length != 6) {
                Toast.makeText(this@VerifyPhoneNumber, "verification code must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (verificationId == null) {
                Toast.makeText(this@VerifyPhoneNumber, "No verification ID. Request code again!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showLoading(true)
            Toast.makeText(this@VerifyPhoneNumber, "verifying code...", Toast.LENGTH_SHORT).show()

            viewModel.verificationId = verificationId
            viewModel.verifyCode(code)
        }

        startResendCountdown()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar2.visibility = View.VISIBLE
            binding.btnLoginAfterOTPPhone.isEnabled = false
            binding.phoneOtp1.isEnabled = false
            binding.phoneOtp2.isEnabled = false
            binding.phoneOtp3.isEnabled = false
            binding.phoneOtp4.isEnabled = false
            binding.phoneOtp5.isEnabled = false
            binding.phoneOtp6.isEnabled = false
        } else {
            binding.progressBar2.visibility = View.GONE
            binding.btnLoginAfterOTPPhone.isEnabled = true
            binding.phoneOtp1.isEnabled = true
            binding.phoneOtp2.isEnabled = true
            binding.phoneOtp3.isEnabled = true
            binding.phoneOtp4.isEnabled = true
            binding.phoneOtp5.isEnabled = true
            binding.phoneOtp6.isEnabled = true
        }
    }
}