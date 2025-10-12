package bankal_deir.com.Login

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import bankal_deir.com.Login.ViewModel.LoginViewModel
import bankal_deir.com.Login.ViewModel.LoginViewModelFactory
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.Signup.SignUp
import bankal_deir.com.databinding.ActivityLoginPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import bankal_deir.com.Login.repository.AuthRepository

class LoginPage : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var viewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val repository = AuthRepository()
        val factory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                val dialog = Dialog(this).apply {
                    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setContentView(R.layout.progress)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }
                viewModel.login(email, password).observe(this) { result ->
                    dialog.dismiss()
                    result.onSuccess {
                        startActivity(Intent(this, MainPage::class.java))
                    }
                    result.onFailure { e ->
                        Toast.makeText(this, e.message ?: "Login error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        binding.seePassword.setOnClickListener {
            binding.edtPassword.inputType = if (binding.seePassword.isChecked)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        binding.btnForget.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val forgetViewBinding = bankal_deir.com.databinding.ForgetpasswordBinding.inflate(layoutInflater)
            builder.setView(forgetViewBinding.root)
            val dialog = builder.create()

            forgetViewBinding.btnReset.setOnClickListener {
                val emailInput = forgetViewBinding.edtEmailForget.text.toString()
                if (emailInput.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    Toast.makeText(this, "Please enter a valid email!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val progress = Dialog(this).apply {
                    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setContentView(R.layout.progress)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }
                viewModel.resetPassword(emailInput).observe(this) { resetResult ->
                    progress.dismiss()
                    resetResult.onSuccess {
                        Toast.makeText(this, "Check your email", Toast.LENGTH_SHORT).show()
                    }
                    resetResult.onFailure { e ->
                        Toast.makeText(this, e.message ?: "Reset error", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            forgetViewBinding.btnCancelForget.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}