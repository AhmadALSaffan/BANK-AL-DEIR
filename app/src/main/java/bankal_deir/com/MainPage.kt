package bankal_deir.com

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.databinding.ActivityMainPageBinding
import bankal_deir.com.recive.Recive
import bankal_deir.com.sendmoney.sendMoney
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class MainPage : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var tranArrayList : ArrayList<transactions>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth= FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        readDataUser()

        binding.btnRecive.setOnClickListener {
            val intent = Intent(this, Recive::class.java)
            startActivity(intent)
        }
        binding.btnSend.setOnClickListener {
            val intent = Intent(this, sendMoney::class.java)
            startActivity(intent)
        }
        binding.payWithQrCode.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan QR-Code")
            options.setOrientationLocked(true)
            options.setBeepEnabled(false)
            options.setCaptureActivity(CaptureActivity::class.java)
            barcodeluncher.launch(options)
        }
        binding.quickProfile.setOnClickListener {
            val intent = Intent(this, profilePage::class.java)
            startActivity(intent)
        }
        binding.transferQuick.setOnClickListener {
            val intent = Intent(this, cards::class.java)
            startActivity(intent)
        }


        recyclerView = binding.userList
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        tranArrayList = arrayListOf<transactions>()
        getTranData()
    }
    var barcodeluncher = registerForActivityResult(ScanContract()){ result ->
        if (result.contents != null){
            if (result.contents.startsWith("ACC")){
                val intent = Intent(this, sendMoney::class.java)
                intent.putExtra("account_number", result.contents)
                startActivity(intent)
            }else{

            }
        }
    }

    private fun getTranData() {
        val mAuth = FirebaseAuth.getInstance()
        val currentUserID = mAuth.currentUser?.uid ?: return

        databaseReference = FirebaseDatabase.getInstance().getReference("history")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUserTrans = mutableListOf<Pair<String, transactions>>()

                for (tranSnap in snapshot.children) {
                    val transaction = tranSnap.getValue(transactions::class.java)
                    if (transaction?.senderUserId == currentUserID || transaction?.receiverWalletID == currentUserID) {
                        transaction?.let {
                            allUserTrans.add(Pair(tranSnap.key ?: "", it))
                        }
                    }
                }

                val sortedByKey = allUserTrans.sortedByDescending { it.first }

                val newestFive = sortedByKey.take(5).map { it.second }

                if (recyclerView.adapter == null) {
                    recyclerView.adapter = MyAdapter(ArrayList(newestFive))
                } else {
                    binding.progressBartran.visibility = View.GONE
                    binding.userList.visibility = View.VISIBLE
                    (recyclerView.adapter as MyAdapter).updateData(ArrayList(newestFive))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TransactionError", "Failed to read transactions: ${error.message}")
            }
        })

    }


    private val handler = android.os.Handler()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            readDataUser()
            getTranData()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDataUser()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    fun readDataUser() {
        val userID = mAuth.uid ?: return
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(userID).get().addOnSuccessListener {
            val firstName = it.child("firstName").value
            val accountNumber = it.child("accountNumber").value
            val wallet = it.child("walletId").value
            val profileImageUrl = it.child("profileImageUrl").value
            binding.progressBarName.visibility= View.GONE
            binding.firstNamett.visibility = View.VISIBLE
            binding.firstNamett.text = firstName?.toString() ?: ""
            readBalance(wallet?.toString() ?: "")
            if (profileImageUrl != null) {
                Glide.with(this)
                    .load(profileImageUrl.toString())
                    .into(binding.profileImageMain)
                binding.progressBarProfileImage.visibility = View.GONE
                binding.profileImageMain.visibility = View.VISIBLE
            }
        }
    }

    fun readBalance(walletId: String) {
        if (walletId.isEmpty()) return
        databaseReference = FirebaseDatabase.getInstance().getReference("wallets")
        databaseReference.child(walletId).get().addOnSuccessListener {
            binding.progressBarBalance.visibility = View.GONE
            binding.balance.visibility = View.VISIBLE
            val balance = it.child("Balance").value
            val balanceDouble = balance?.toString()?.toDoubleOrNull() ?: 0.0
            val formattedBalance = "%.2f".format(balanceDouble)
            binding.balance.text = "$formattedBalance$"
        }
    }
}