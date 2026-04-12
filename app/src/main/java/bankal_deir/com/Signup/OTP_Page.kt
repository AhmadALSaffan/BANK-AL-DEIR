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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    var userToken = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOtpPageBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        setContentView(binding.root)
        hideSystemBars()
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

        binding.otp1.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 1) binding.otp2.requestFocus()
        }
        binding.otp2.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.otp3.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.otp1.requestFocus()
            }
        }
        binding.otp3.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.otp4.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.otp2.requestFocus()
            }
        }
        binding.otp4.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.otp5.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.otp3.requestFocus()
            }
        }
        binding.otp5.doOnTextChanged { text, _, before, _ ->
            when {
                text?.length == 1 -> binding.otp6.requestFocus()
                text.isNullOrEmpty() && before == 1 -> binding.otp4.requestFocus()
            }
        }
        binding.otp6.doOnTextChanged { text, _, before, _ ->
            if (text.isNullOrEmpty() && before == 1) binding.otp5.requestFocus()
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
                binding.otp6.text.toString().isEmpty()
            ) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
            }
            if (!otp.equals(random.toString())) {
                Toast.makeText(this, "Wrong OTP !!", Toast.LENGTH_SHORT).show()
            } else {
                val progressDialog = Dialog(this)
                progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                progressDialog.setCancelable(false)
                progressDialog.setContentView(R.layout.progress)
                progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressDialog.show()
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser ?: return@addOnCompleteListener
                            val userId = user.uid
                            user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val userToken = tokenTask.result?.token ?: ""
                                    if (userToken == null){
                                        Toast.makeText(this, "Token is empty or null", Toast.LENGTH_SHORT).show()
                                    }
                                    val iban = generateIBAN(userId)
                                    val accountNumber = generateAccountNumber()
                                    val walletId = generateWalletId()

                                    val userMap = mapOf(
                                        "userId" to userId,
                                        "userToken" to userToken,
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
                                            databaseReference.child("wallets").child(walletId)
                                                .setValue(walletMap)
                                                .addOnCompleteListener {
                                                    Toast.makeText(
                                                        this,
                                                        "User registered successfully!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    progressDialog.dismiss()
                                                    val intent = Intent(
                                                        this@OTP_Page,
                                                        createPinCode::class.java
                                                    )
                                                    startActivity(intent)
                                                    finish()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        this,
                                                        it.message,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Failed to get ID token: ${tokenTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Signup failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
    fun random(){
        random= Random.Default.nextInt(100000..999999)
        try {
            val otpHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>Bank Al-Deir OTP</title>
</head>
<body style="margin:0;padding:0;background-color:#001711;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#001711;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="520" cellpadding="0" cellspacing="0" style="background-color:#0f2820;border-radius:24px;border:1px solid #1a3d2e;overflow:hidden;">

          <!-- Header -->
          <tr>
            <td align="center" style="padding:32px 40px 24px;border-bottom:1px solid #1a3d2e;">
              <table cellpadding="0" cellspacing="0">
                <tr>
                  <td style="background-color:#0a2218;border-radius:12px;padding:10px 14px;vertical-align:middle;">
                    <span style="font-size:22px;font-weight:900;color:#4edea3;letter-spacing:-0.5px;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">Bank Al-Deir</span>
                  </td>
                </tr>
              </table>
              <p style="margin:12px 0 0;font-size:11px;color:#3f5a50;letter-spacing:2px;font-weight:700;text-transform:uppercase;">SECURE VERIFICATION</p>
            </td>
          </tr>

          <!-- Icon + Title -->
          <tr>
            <td align="center" style="padding:36px 40px 0;">
              <table cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" style="width:72px;height:72px;background-color:#0a2218;border-radius:50%;border:1.5px solid #1a3d2e;">
                    <span style="font-size:32px;line-height:72px;">🔐</span>
                  </td>
                </tr>
              </table>
              <h1 style="margin:20px 0 6px;font-size:28px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Verification Code</h1>
              <p style="margin:0;font-size:14px;color:#7a9e8e;line-height:1.6;">
                We received a request to verify your identity.<br/>
                Use the code below to complete sign-up.
              </p>
            </td>
          </tr>

          <!-- OTP Box -->
          <tr>
            <td align="center" style="padding:32px 40px;">
              <table cellpadding="0" cellspacing="0" style="background-color:#001711;border-radius:16px;border:1.5px solid #1a3d2e;">
                <tr>
                  <td align="center" style="padding:28px 48px;">
                    <p style="margin:0 0 6px;font-size:10px;font-weight:700;color:#3f5a50;letter-spacing:3px;text-transform:uppercase;">YOUR OTP CODE</p>
                    <p style="margin:0;font-size:48px;font-weight:900;color:#4edea3;letter-spacing:12px;font-family:'Courier New',Courier,monospace;">$random</p>
                  </td>
                </tr>
              </table>
              <p style="margin:16px 0 0;font-size:13px;color:#4a7a6a;">
                This code expires in <strong style="color:#ffb95f;">10 minutes</strong>.
              </p>
            </td>
          </tr>

          <!-- Warning -->
          <tr>
            <td style="padding:0 40px 32px;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#2a1400;border-radius:12px;border:1px solid #3d2000;">
                <tr>
                  <td style="padding:16px 20px;">
                    <table cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="font-size:18px;vertical-align:top;padding-right:12px;">⚠️</td>
                        <td style="font-size:13px;color:#c2935a;line-height:1.6;">
                          <strong style="color:#ffb95f;">Security Notice:</strong> Never share this code with anyone, including Bank Al-Deir support staff. We will never ask for your OTP.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding:0 40px;">
              <div style="height:1px;background-color:#1a3d2e;"></div>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td align="center" style="padding:24px 40px 32px;">
              <p style="margin:0 0 4px;font-size:12px;color:#3f5a50;">
                If you didn't request this, you can safely ignore this email.
              </p>
              <p style="margin:0;font-size:11px;color:#2a4a3a;">
                © 2025 Bank Al-Deir · Deir Ez-Zor, Syria · All rights reserved
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
            """.trimIndent()

            val mail = SendMail(
                "bank.al.deir.sup@gmail.com",
                "pgeh xdyv cycc boyg",
                email,
                "Your Bank Al-Deir Verification Code",
                otpHtml
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
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}