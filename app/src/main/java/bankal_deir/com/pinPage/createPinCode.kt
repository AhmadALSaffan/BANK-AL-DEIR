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
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityCreatePinCodeBinding
import bankal_deir.com.databinding.ActivityPinPageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlin.getValue

class createPinCode : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePinCodeBinding
    private val viewModel: PinViewModel by viewModels()
    private lateinit var userId : String
    private lateinit var mAuth: FirebaseAuth
    private lateinit var pinEdit: EditText
    private lateinit var circles: List<View>
    private val pinBuilder = StringBuilder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreatePinCodeBinding.inflate(layoutInflater)
        mAuth = FirebaseAuth.getInstance()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        userId = mAuth.currentUser?.uid ?: ""

        pinEdit = binding.pinEdit

        pinEdit.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length == 4) {
                    viewModel.savePin(userId, s.toString())
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
        val buttons = listOf(
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
        buttons.forEach { button ->
            button.setOnClickListener {
                if (pinBuilder.length < 4) {
                    pinBuilder.append(button.text)
                    updateCircles()
                }
                if (pinBuilder.length == 4) {
                    progressDialog.show()
                    viewModel.savePin(userId, pinEdit.text.toString())
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
            if (pinBuilder.length == 4) {
                viewModel.savePin(userId, pinEdit.text.toString())
            }
            if (pinBuilder.length != 4) {
                Toast.makeText(
                    this@createPinCode,
                    "Please Enter The Pin Code !",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        viewModel.pinStatus.observe(this) { success ->
            if (success) {
                progressDialog.dismiss()
                val intent = Intent(this@createPinCode, MainPage::class.java)
                startActivity(intent)
                finish()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        }
    }
    private fun updateCircles(){
        pinEdit.setText(pinBuilder)
        for (i in circles.indices){
            circles[i].setBackgroundResource(
                if(i < pinBuilder.length) R.drawable.pin_circle_filled
                else R.drawable.pin_circle_empty
            )
        }
    }
}