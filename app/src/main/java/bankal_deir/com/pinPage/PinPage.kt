package bankal_deir.com.pinPage

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.MainActivity
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityPinPageBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Button


class PinPage : AppCompatActivity() {
    private lateinit var binding: ActivityPinPageBinding
    private val viewModel: PinViewModel by viewModels()
    private lateinit var buttons: List<Button>
    private lateinit var userId: String
    private lateinit var mAuth: FirebaseAuth
    private lateinit var pinEdit: EditText
    private lateinit var circles: List<View>
    private val pinBuilder = StringBuilder()
    private val maxAttempts = 5
    private val lockoutDurationMillis = 15 * 60 * 1000L
    private val prefs by lazy { getSharedPreferences("PinPrefs", Context.MODE_PRIVATE) }
    private var failedAttempts = 0
    private var lockoutStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("CRASH_HANDLER", "Uncaught Exception", exception)
            exception.printStackTrace()
        }

        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        binding = ActivityPinPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = mAuth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        pinEdit = binding.pinEdit

        pinEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length == 4) {
                    viewModel.verifyPin(userId, s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        circles = listOf(
            binding.circle1,
            binding.circle2,
            binding.circle3,
            binding.circle4
        )

         buttons = listOf(
            binding.btn1,
            binding.btn2,
            binding.btn3,
            binding.btn4,
            binding.btn5,
            binding.btn6,
            binding.btn7,
            binding.btn8,
            binding.btn9,
            binding.btn0,
        )
        failedAttempts = prefs.getInt("failedAttempts", 0)
        lockoutStartTime = prefs.getLong("lockoutStartTime", 0L)

        checkLockout()

        buttons.forEach { button ->
            button.setOnClickListener {
                if (isLockedOut()) {
                    Toast.makeText(this, "Too many attempts. Please wait 15 minutes.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (pinBuilder.length < 4) {
                    pinBuilder.append(button.text)
                    updateCircles()
                }

                if (pinBuilder.length == 4) {
                    progressDialog.show()
                    viewModel.verifyPin(userId, pinBuilder.toString())
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updateCircles()
            }
        }

        binding.btnOk.setOnClickListener {
            if (isLockedOut()) {
                Toast.makeText(this, "Too many attempts. Please wait 15 minutes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pinBuilder.length == 4) {
                progressDialog.show()
                viewModel.verifyPin(userId, pinBuilder.toString())
            } else {
                Toast.makeText(this@PinPage, "Please Enter The Pin Code !", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.pinStatus.observe(this) { success ->
            if (success) {
                resetLockout()
                progressDialog.dismiss()
                val intent = Intent(this@PinPage, MainPage::class.java)
                startActivity(intent)
                finish()
            }
            if (!success){
                failedAttempts++
                saveFailedAttempts()
                if (failedAttempts >= maxAttempts) {
                    startLockout()
                }
                pinBuilder.clear()
                updateCircles()
                progressDialog.dismiss()
                Toast.makeText(this@PinPage,"The PIN CODE is not correct please write it again !", Toast.LENGTH_SHORT).show()
            }
        }


        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this@PinPage, message, Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = mAuth.currentUser
        if (user == null) {
            redirectToLogin()
        } else {
            user.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    null
                } else {
                    redirectToLogin()
                }
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }



    private fun updateCircles() {
        pinEdit.setText(pinBuilder)
        for (i in circles.indices) {
            circles[i].setBackgroundResource(
                if (i < pinBuilder.length) R.drawable.pin_circle_filled
                else R.drawable.pin_circle_empty
            )
        }
    }
    private fun isLockedOut(): Boolean {
        if (lockoutStartTime == 0L) {
            android.util.Log.d("PinPage", "No lockout set")
            return false
        }
        val elapsed = System.currentTimeMillis() - lockoutStartTime
        android.util.Log.d("PinPage", "Lockout elapsed time: $elapsed ms")

        if (elapsed >= lockoutDurationMillis) {
            android.util.Log.d("PinPage", "Lockout expired, resetting")
            resetLockout()
            return false
        }
        android.util.Log.d("PinPage", "Still locked out")
        return true
    }


    private fun startLockout() {
        lockoutStartTime = System.currentTimeMillis()
        saveLockoutStartTime()
        Toast.makeText(this, "Too many failed attempts. Locked for 15 minutes.", Toast.LENGTH_LONG).show()
        disableInput()
    }

    private fun resetLockout() {
        failedAttempts = 0
        lockoutStartTime = 0L
        saveFailedAttempts()
        saveLockoutStartTime()
        enableInput()
    }

    private fun disableInput() {
        buttons.forEach { it.isEnabled = false }
        binding.btnOk.isEnabled = false
        val remaining = lockoutDurationMillis - (System.currentTimeMillis() - lockoutStartTime)
        Handler(Looper.getMainLooper()).postDelayed({
            resetLockout()
        }, remaining)
    }

    private fun enableInput() {
        buttons.forEach { it.isEnabled = true }
        binding.btnOk.isEnabled = true
    }

    private fun saveFailedAttempts() {
        prefs.edit().putInt("failedAttempts", failedAttempts).apply()
    }

    private fun saveLockoutStartTime() {
        prefs.edit().putLong("lockoutStartTime", lockoutStartTime).apply()
    }

    private fun checkLockout() {
        if (isLockedOut()) {
            disableInput()
        } else {
            enableInput()
        }
    }
}