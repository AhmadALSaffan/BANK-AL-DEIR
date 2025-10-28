package bankal_deir.com.Signup

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.SendMail
import bankal_deir.com.databinding.ActivityOtpPageBinding
import bankal_deir.com.pinPage.createPinCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

class OTP_Page : AppCompatActivity() {
    private lateinit var binding: ActivityOtpPageBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    var email = ""
    var password = ""
    var firstName = ""
    var lastName = ""
    var phoneNumber = ""
    var random : Int=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOtpPageBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
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
        email = intent.getStringExtra("email").toString()?:""
        password = intent.getStringExtra("password").toString()?:""
        firstName = intent.getStringExtra("firstName").toString()?:""
        lastName = intent.getStringExtra("lastName").toString()?:""
        phoneNumber = intent.getStringExtra("phoneNumber").toString()?:""
        random()

        binding.showEmail.text = email.toString()

        binding.resendOTP.setOnClickListener {
            random()
        }

        binding.otp1.doOnTextChanged { text, start, before, count ->
            if (!binding.otp1.text.toString().isEmpty()){
                binding.otp1.requestFocus()
            }
            if (!binding.otp2.text.toString().isEmpty()){
                binding.otp2.requestFocus()
            }
        }

        binding.otp2.doOnTextChanged{text, start, before, count ->
            if (!binding.otp2.text.toString().isEmpty()){
                binding.otp3.requestFocus()
            }else{
                binding.otp1.requestFocus()
            }

        }

        binding.otp3.doOnTextChanged{text, start, before, count ->
            if (!binding.otp3.text.toString().isEmpty()){
                binding.otp4.requestFocus()
            }else{
                binding.otp2.requestFocus()
            }

        }

        binding.otp4.doOnTextChanged{text, start, before, count ->
            if (!binding.otp4.text.toString().isEmpty()){
                binding.otp5.requestFocus()
            }else{
                binding.otp3.requestFocus()
            }

        }

        binding.otp5.doOnTextChanged{text, start, before, count ->
            if (!binding.otp5.text.toString().isEmpty()){
                binding.otp6.requestFocus()
            }else{
                binding.otp4.requestFocus()
            }

        }

        binding.otp6.doOnTextChanged{text, start, before, count ->
            if (binding.otp6.text.toString().isEmpty()){
                binding.otp5.requestFocus()
            }

        }
        binding.btnSignUpAfterOTP.setOnClickListener {
            var otp1 = binding.otp1.text.toString()
            var otp2 = binding.otp2.text.toString()
            var otp3 = binding.otp3.text.toString()
            var otp4 = binding.otp4.text.toString()
            var otp5 = binding.otp5.text.toString()
            var otp6 = binding.otp6.text.toString()

            var otp = "$otp1$otp2$otp3$otp4$otp5$otp6"

            if (binding.otp1.text.toString().isEmpty() ||
                binding.otp2.text.toString().isEmpty() ||
                binding.otp3.text.toString().isEmpty() ||
                binding.otp4.text.toString().isEmpty() ||
                binding.otp5.text.toString().isEmpty() ||
                binding.otp6.text.toString().isEmpty()){
                Toast.makeText(this,"Enter OTP", Toast.LENGTH_SHORT).show()
            }
            if (!otp.equals(random.toString())){
                Toast.makeText(this,"Wrong OTP !!", Toast.LENGTH_SHORT).show()
            }else{
                val progressDialog = Dialog(this)
                progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                progressDialog.setCancelable(false)
                progressDialog.setContentView(R.layout.progress)
                progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressDialog.show()
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId =
                                firebaseAuth.currentUser?.uid
                                    ?: return@addOnCompleteListener
                            val iban = generateIBAN(userId)
                            val accountNumber = generateAccountNumber()
                            val walletId = generateWalletId()


                            val userMap = mapOf(
                                "userId" to userId,
                                "iban" to iban,
                                "accountNumber" to accountNumber,
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "phoneNumber" to phoneNumber,
                                "email" to email,
                                "walletId" to walletId
                            )

                            databaseReference.child("users").child(userId)
                                .setValue(userMap)
                                .addOnCompleteListener {
                                    val walletMap = mapOf(
                                        "userId" to userId,
                                        "walletNumber" to walletId,
                                        "Balance" to 0
                                    )
                                    databaseReference.child("wallets").child(walletId).setValue(walletMap).addOnCompleteListener {
                                        Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                                        progressDialog.dismiss()
                                        val intent = Intent(this@OTP_Page, createPinCode::class.java)
                                        startActivity(intent)
                                        finish()
                                    }.addOnFailureListener {
                                        Toast.makeText(this,it.message, Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT)
                                        .show()
                                }

                        } else {
                            Toast.makeText(this,"Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }

    }
    fun random(){
        random= Random.Default.nextInt(100000..999999)
        try {
            val mail = SendMail(
                "bank.al.deir.sup@gmail.com",
                "pgeh xdyv cycc boyg",
                email,
                "BANK AL-DEIR's OTP",
                "Please Don't Share this OTP with anyone \n Your OTP is -> $random"
            )
            mail.execute()
            Toast.makeText(this, "OTP sent to $email", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("OTP_Page", "SendMail error", e)
        }
    }
    private fun generateIBAN(userId:String): String{
        return "SYR${System.currentTimeMillis()}${userId.take(5)}"
    }
    private fun generateAccountNumber(): String {
        return "ACC"+(1000000000..9999999999).random().toString()
    }
    private fun generateWalletId(): String {
        return "WAL" + UUID.randomUUID().toString().replace("-", "").take(12)
    }
}