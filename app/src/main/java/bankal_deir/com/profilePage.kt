package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import bankal_deir.com.Login.LoginPage
import bankal_deir.com.databinding.ActivityMainPageBinding
import bankal_deir.com.databinding.ActivityProfilePageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class profilePage : AppCompatActivity() {
    private lateinit var binding: ActivityProfilePageBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        mAuth= FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        readDataUser()
        binding.btnLogOut.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }
    }
    fun readDataUser() {
        val userID = mAuth.uid ?: return
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(userID).get().addOnSuccessListener {
            val firstName = it.child("firstName").value
            val lastName = it.child("lastName").value
            val phoneNumber = it.child("phoneNumber").value
            val email = it.child("email").value
            val accountNumber = it.child("accountNumber").value
            val fullName = firstName.toString()+" "+lastName.toString()
            binding.txtFullName.text = fullName
            binding.txtEmail.text = email.toString()
            binding.txtFirstName.text = firstName.toString()
            binding.txtLastName.text = lastName.toString()
            binding.txtPhoneNumber.text = phoneNumber.toString()
            binding.txtAccountNumber.text = accountNumber.toString()
        }
    }
}