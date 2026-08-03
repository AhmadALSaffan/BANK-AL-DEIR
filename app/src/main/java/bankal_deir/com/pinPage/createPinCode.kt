package bankal_deir.com.pinPage

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityCreatePinCodeBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class createPinCode : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePinCodeBinding
    private val viewModel: PinViewModel by viewModels()
    private lateinit var userId: String
    private lateinit var mAuth: FirebaseAuth
    private lateinit var circles: List<View>
    private lateinit var buttons: List<MaterialButton>
    private val pinBuilder = StringBuilder()
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreatePinCodeBinding.inflate(layoutInflater)
        hideSystemBars()
        mAuth = FirebaseAuth.getInstance()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = mAuth.currentUser?.uid ?: ""

        circles = listOf(binding.circle1, binding.circle2, binding.circle3, binding.circle4)

        buttons = listOf(
            binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6,
            binding.btn7, binding.btn8, binding.btn9,
            binding.btn0
        )

        (buttons + binding.btnDelete).forEach { PinFeedback.attachKeyPress(it) }

        buttons.forEach { button ->
            button.setOnClickListener {
                if (saving) return@setOnClickListener
                if (pinBuilder.length < 4) {
                    hideMessage()
                    pinBuilder.append(button.text)
                    binding.pinEdit.setText(pinBuilder)
                    PinFeedback.fillDot(circles[pinBuilder.length - 1])
                }
                if (pinBuilder.length == 4) {
                    saving = true
                    viewModel.savePin(userId, pinBuilder.toString())
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (saving || pinBuilder.isEmpty()) return@setOnClickListener
            PinFeedback.clearDot(circles[pinBuilder.length - 1])
            pinBuilder.deleteCharAt(pinBuilder.length - 1)
            binding.pinEdit.setText(pinBuilder)
        }

        viewModel.pinStatus.observe(this) { success ->
            if (success) {
                PinFeedback.acceptDots(circles) {
                    startActivity(Intent(this@createPinCode, MainPage::class.java))
                    finish()
                }
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                PinFeedback.rejectDots(circles) {
                    pinBuilder.clear()
                    binding.pinEdit.setText("")
                    saving = false
                }
                showMessage(message)
            }
        }
    }

    private fun showMessage(text: String) {
        binding.tvPinMessage.text = text
        binding.tvPinMessage.animate().alpha(1f).setDuration(160).start()
    }

    private fun hideMessage() {
        if (binding.tvPinMessage.alpha == 0f) return
        binding.tvPinMessage.animate().alpha(0f).setDuration(160).start()
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
