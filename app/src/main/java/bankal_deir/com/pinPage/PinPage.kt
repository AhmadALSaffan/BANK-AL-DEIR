package bankal_deir.com.pinPage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.MainActivity
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.ActivityPinPageBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class PinPage : AppCompatActivity() {
    private lateinit var binding: ActivityPinPageBinding
    private val viewModel: PinViewModel by viewModels()
    private lateinit var buttons: List<MaterialButton>
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
    private var verifying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        binding = ActivityPinPageBinding.inflate(layoutInflater)
        hideSystemBars()
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

        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        pinEdit = binding.pinEdit

        circles = listOf(binding.circle1, binding.circle2, binding.circle3, binding.circle4)

        buttons = listOf(
            binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6,
            binding.btn7, binding.btn8, binding.btn9,
            binding.btn0
        )

        (buttons + binding.btnDelete).forEach { PinFeedback.attachKeyPress(it) }

        failedAttempts = prefs.getInt("failedAttempts", 0)
        lockoutStartTime = prefs.getLong("lockoutStartTime", 0L)
        checkLockout()

        buttons.forEach { button ->
            button.setOnClickListener {
                if (verifying) return@setOnClickListener
                if (isLockedOut()) {
                    showMessage("Too many attempts. Try again in 15 minutes.")
                    return@setOnClickListener
                }
                if (pinBuilder.length < 4) {
                    hideMessage()
                    pinBuilder.append(button.text)
                    pinEdit.setText(pinBuilder)
                    PinFeedback.fillDot(circles[pinBuilder.length - 1])
                }
                if (pinBuilder.length == 4) submit()
            }
        }

        binding.btnDelete.setOnClickListener {
            if (verifying || pinBuilder.isEmpty()) return@setOnClickListener
            PinFeedback.clearDot(circles[pinBuilder.length - 1])
            pinBuilder.deleteCharAt(pinBuilder.length - 1)
            pinEdit.setText(pinBuilder)
        }

        viewModel.pinStatus.observe(this) { success ->
            if (success) {
                resetLockout()
                PinFeedback.acceptDots(circles) {
                    startActivity(Intent(this@PinPage, MainPage::class.java))
                    finish()
                }
            } else {
                failedAttempts++
                saveFailedAttempts()
                val remaining = maxAttempts - failedAttempts

                PinFeedback.rejectDots(circles) {
                    pinBuilder.clear()
                    pinEdit.setText("")
                    verifying = false
                    if (failedAttempts >= maxAttempts) startLockout()
                }
                showMessage(
                    if (remaining > 0) "Wrong PIN. $remaining attempts left."
                    else "Too many attempts. Locked for 15 minutes."
                )
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                verifying = false
                showMessage(message)
            }
        }
    }

    /** The 4th digit submits on its own — no OK key to hunt for. */
    private fun submit() {
        verifying = true
        viewModel.verifyPin(userId, pinBuilder.toString())
    }

    private fun showMessage(text: String) {
        binding.tvPinMessage.text = text
        binding.tvPinMessage.animate().alpha(1f).setDuration(160).start()
    }

    private fun hideMessage() {
        if (binding.tvPinMessage.alpha == 0f) return
        binding.tvPinMessage.animate().alpha(0f).setDuration(160).start()
    }

    override fun onStart() {
        super.onStart()
        val user = mAuth.currentUser
        if (user == null) {
            redirectToLogin()
        } else {
            user.getIdToken(true).addOnCompleteListener { task ->
                if (!task.isSuccessful) redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun isLockedOut(): Boolean {
        if (lockoutStartTime == 0L) return false
        val elapsed = System.currentTimeMillis() - lockoutStartTime
        if (elapsed >= lockoutDurationMillis) {
            resetLockout()
            return false
        }
        return true
    }

    private fun startLockout() {
        lockoutStartTime = System.currentTimeMillis()
        saveLockoutStartTime()
        showMessage("Too many failed attempts. Locked for 15 minutes.")
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
        binding.btnDelete.isEnabled = false
        val remaining = lockoutDurationMillis - (System.currentTimeMillis() - lockoutStartTime)
        Handler(Looper.getMainLooper()).postDelayed({ resetLockout() }, remaining)
    }

    private fun enableInput() {
        buttons.forEach { it.isEnabled = true }
        binding.btnDelete.isEnabled = true
    }

    private fun saveFailedAttempts() {
        prefs.edit().putInt("failedAttempts", failedAttempts).apply()
    }

    private fun saveLockoutStartTime() {
        prefs.edit().putLong("lockoutStartTime", lockoutStartTime).apply()
    }

    private fun checkLockout() {
        if (isLockedOut()) disableInput() else enableInput()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
