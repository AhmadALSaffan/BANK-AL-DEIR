package bankal_deir.com.recive

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityReciveBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class Recive : AppCompatActivity() {
    private lateinit var binding: ActivityReciveBinding
    private val viewModel: ReciveViewModel by viewModels()
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReciveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeViewModel()
        viewModel.loadUserData()

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnCopy1.setOnClickListener {
            val IbanNumber = binding.textIBAN.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Plain Text", IbanNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
        binding.btnCopy2.setOnClickListener {
            val accountNumber = binding.textAccountNumber.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Plain Text", accountNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.loading.collect { isLoading ->
                if(isLoading) showProgressDialog() else hideProgressDialog()
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.userData.collect { data ->
                data?.let {
                    binding.textFirstName.text = it["firstName"]?.toString() ?: ""
                    binding.textLastName.text = it["lastName"]?.toString() ?: ""
                    binding.textPhoneNumber.text = it["phoneNumber"]?.toString() ?: ""
                    binding.textEmail.text = it["email"]?.toString() ?: ""
                    binding.textAccountNumber.text = it["accountNumber"]?.toString() ?: ""
                    binding.textIBAN.text = it["iban"]?.toString() ?: ""
                    binding.accountQr.text = it["accountNumber"]?.toString() ?: ""
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.qrBitmap.collect { bitmap ->
                binding.qrCodeImg.setImageBitmap(bitmap)
            }
        }
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = Dialog(this@Recive).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.progress)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                show()
            }
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
