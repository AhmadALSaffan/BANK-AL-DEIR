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
import bankal_deir.com.pinPage.PinPage
import bankal_deir.com.pinPage.createPinCode
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider


class LoginPage : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
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

        val repository = AuthRepository()
        val factory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginWithGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.loginWithGitHub.setOnClickListener {
            val liveData = viewModel.signInWithGitHub(this)
            val dialog = Dialog(this).apply {
                setContentView(R.layout.progress)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(false)
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.progress)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                show()
            }
            dialog.show()
            binding.root.postDelayed({
                dialog.dismiss()
            }, 3000)
            liveData.observe(this) { result ->
                dialog.dismiss()
                result.onSuccess {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        FirebaseDatabase.getInstance().reference.child("users").child(userId)
                            .child("pin").get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val intent = Intent(this, PinPage::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    val intent = Intent(this, createPinCode::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            }
                    }
                    result.onFailure { e ->
                        Toast.makeText(
                            this,
                            "GitHub sign-in error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId).child("pin").get()
                                        .addOnSuccessListener { snapshot ->
                                            if (snapshot.exists()) {
                                                val intent = Intent(this, PinPage::class.java)
                                                intent.flags =
                                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                val intent = Intent(this, createPinCode::class.java)
                                                intent.flags =
                                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }
                                        }
                                }
                            }
                            result.onFailure { e ->
                                Toast.makeText(this, e.message ?: "Login error", Toast.LENGTH_SHORT)
                                    .show()
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
                    val forgetViewBinding =
                        bankal_deir.com.databinding.ForgetpasswordBinding.inflate(layoutInflater)
                    builder.setView(forgetViewBinding.root)
                    val dialog = builder.create()

                    forgetViewBinding.btnReset.setOnClickListener {
                        val emailInput = forgetViewBinding.edtEmailForget.text.toString()
                        if (emailInput.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailInput)
                                .matches()
                        ) {
                            Toast.makeText(this, "Please enter a valid email!", Toast.LENGTH_SHORT)
                                .show()
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
                                Toast.makeText(this, e.message ?: "Reset error", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        dialog.dismiss()
                    }
                    forgetViewBinding.btnCancelForget.setOnClickListener { dialog.dismiss() }
                    dialog.show()
                }
            }
        }
         override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                val dialog = Dialog(this).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setContentView(R.layout.progress)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }

                viewModel.signInWithGoogle(credential).observe(this) { result ->
                    dialog.dismiss()
                    result.onSuccess {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(userId).child("pin").get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.exists()) {
                                        val intent = Intent(this, PinPage::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        val intent = Intent(this, createPinCode::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                        }
                    }
                    result.onFailure { e ->
                        Toast.makeText(this, e.message ?: "Google sign-in error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}