package bankal_deir.com.History

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.MyAdapter
import bankal_deir.com.R
import bankal_deir.com.databinding.ActivityHistoryBinding
import bankal_deir.com.transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var tranArrayList: ArrayList<PaymentTransaction>
    private var isSearching = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)

        recyclerView = binding.fullHistoryRec
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.progressBarHistoryFull.visibility = View.VISIBLE
        binding.fullHistoryRec.visibility = View.GONE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getTranData()
        binding.backHistory.setOnClickListener {
            finish()
        }

        // Live search as user types
        binding.searchFullHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    isSearching = false
                    if (::tranArrayList.isInitialized) updateRecyclerView(tranArrayList)
                } else {
                    isSearching = true
                    searchTransactions(query)
                }
            }
        })

        // Keep button working as well
        binding.btnSearch.setOnClickListener {
            val query = binding.searchFullHistory.text.toString().trim()
            if (query.isEmpty()) {
                isSearching = false
                getTranData()
            } else {
                isSearching = true
                searchTransactions(query)
            }
        }
    }

    private fun getTranData() {
        mAuth = FirebaseAuth.getInstance()
        val currentUserID = mAuth.currentUser?.uid ?: return

        databaseReference = FirebaseDatabase.getInstance().getReference("history")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUserTrans = mutableListOf<PaymentTransaction>()

                for (tranSnap in snapshot.children) {
                    val transaction = tranSnap.getValue(PaymentTransaction::class.java)
                    if (transaction?.senderUserId == currentUserID ||
                        transaction?.receiverWalletID == currentUserID
                    ) {
                        transaction?.let { allUserTrans.add(it) }
                    }
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val sortedByDate = allUserTrans.sortedByDescending { transaction ->
                    try {
                        dateFormat.parse(transaction.date)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }

                tranArrayList = ArrayList(sortedByDate)
                updateRecyclerView(tranArrayList)

                binding.progressBarHistoryFull.visibility = View.GONE
                binding.fullHistoryRec.visibility = View.VISIBLE

                if (recyclerView.adapter == null) {
                    recyclerView.adapter = MyAdapter(tranArrayList)
                } else {
                    (recyclerView.adapter as MyAdapter).updateData(tranArrayList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TransactionError", "Failed to read transactions: ${error.message}")
                binding.progressBarHistoryFull.visibility = View.GONE
            }
        })
    }

    private fun searchTransactions(query: String) {
        if (!::tranArrayList.isInitialized || tranArrayList.isEmpty()) {
            return
        }

        val filteredList = tranArrayList.filter { transaction ->
            transaction.transactionNumber?.contains(query, ignoreCase = true) == true ||
                    transaction.date?.contains(query, ignoreCase = true) == true
        }

        updateRecyclerView(ArrayList(filteredList))
    }

    private fun updateRecyclerView(data: ArrayList<PaymentTransaction>) {
        if (data.isEmpty()) {
            binding.progressBarHistoryFull.visibility = View.GONE
            binding.fullHistoryRec.visibility = View.GONE
        } else {
            binding.progressBarHistoryFull.visibility = View.GONE
            binding.fullHistoryRec.visibility = View.VISIBLE
        }

        if (recyclerView.adapter == null) {
            recyclerView.adapter = MyAdapter(data)
        } else {
            (recyclerView.adapter as MyAdapter).updateData(data)
        }
    }


    private val handler = android.os.Handler()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isSearching) {
                getTranData()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
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