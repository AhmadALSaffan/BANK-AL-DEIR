package bankal_deir.com.Signup

import android.R.attr.value
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import bankal_deir.com.Signup.OTP_Page
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope

private val SignUp.phoneNumber: Any get() = value
private val SignUp.lastName: Any get() = value
private val SignUp.password: Any get() = value
private val SignUp.firstName: Any get() = value

// SignUp.kt
class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
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

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnSignUp.setOnClickListener {
            val email = binding.edtEmailsign.text.toString().trim()
            val firstName = binding.edtFirstName.text.toString().trim()
            val lastName = binding.edtLastName.text.toString().trim()
            val phoneNumber = binding.edtPhoneCode.text.toString() + binding.edtPhoneNumber.text.toString()
            val password = binding.edtPasswordsign.text.toString()
            val confirmPassword = binding.edtPasswordCheck.text.toString()

            when {
                email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                binding.edtPhoneNumber.text.length != 9 -> {
                    Toast.makeText(this, "The Phone Number Should be 9 Digits", Toast.LENGTH_SHORT).show()
                }
                !binding.checkBox.isChecked -> {
                    Toast.makeText(this, "Please Check The Terms and Conditions", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val intent = Intent(this, OTP_Page::class.java).apply {
                        putExtra("email", email)
                        putExtra("firstName", firstName)
                        putExtra("lastName", lastName)
                        putExtra("phoneNumber", phoneNumber)
                        putExtra("password", password)
                    }
                    startActivity(intent)
                }
            }
        }

        viewModel.signUpState.onEach { result ->
            when (result) {
                is Resource.Success<*> -> {
                    val fullPhone = binding.edtPhoneCode.text.toString() + binding.edtPhoneNumber.text.toString()
                    val intent = Intent(this, OTP_Page::class.java).apply {
                        putExtra("email", binding.edtEmailsign.text.toString())
                        putExtra("password", binding.edtPasswordsign.text.toString())
                        putExtra("firstName", binding.edtFirstName.text.toString())
                        putExtra("lastName", binding.edtLastName.text.toString())
                        putExtra("phoneNumber", fullPhone)
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, "Sign up failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                Resource.Loading -> {}
            }
        }.launchIn(lifecycleScope)

        binding.seePasswordSignUp.setOnCheckedChangeListener { _, isChecked ->
            val inputType = if (isChecked) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.edtPasswordsign.inputType = inputType
            binding.edtPasswordCheck.inputType = inputType
        }
    }
}
