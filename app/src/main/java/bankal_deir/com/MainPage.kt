package bankal_deir.com

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.AmountTopUp.AmountActivity
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.Fatora.UI.FatoraMain
import bankal_deir.com.History.HistoryActivity
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
import java.text.SimpleDateFormat
import java.util.Locale

class MainPage : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var tranArrayList: ArrayList<transactions>
    private var currentUserWalletId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        hideSystemBars()
        setContentView(binding.root)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        readDataUser()
        showBanner()
        loadAds()
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
            options.setCaptureActivity(CustomCaptureActivity::class.java)
            barcodeluncher.launch(options)
        }
        binding.quickProfile.setOnClickListener {
            val intent = Intent(this, profilePage::class.java)
            startActivity(intent)
        }
        binding.myCards.setOnClickListener {
            val intent = Intent(this, cards::class.java)
            startActivity(intent)
        }

        binding.payWithFatora.setOnClickListener {
            val intent = Intent(this@MainPage, FatoraMain::class.java)
            startActivity(intent)
        }

        // Shared floating bottom nav (Home selected here).
        NavHelper.setup(this, "home")
        // Arrived via the nav's Scan action? open the scanner.
        if (intent.getBooleanExtra("startScan", false)) {
            binding.payWithQrCode.post { binding.payWithQrCode.performClick() }
        }


        binding.btnPayPal.setOnClickListener {
            binding.progressBarBalance.visibility = android.view.View.VISIBLE
            Thread {
                // Step 1: fetch client token from Cloud Function
                val token = try {
                    val url = java.net.URL("https://us-central1-bank-al-deir.cloudfunctions.net/getBraintreeToken")
                    val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    org.json.JSONObject(conn.inputStream.bufferedReader().readText()).getString("token")
                } catch (e: Exception) {
                    android.util.Log.e("Braintree", "Token fetch failed: ${e.message}")
                    "sandbox_qzp3nw4q_mshysrbxdfskrz9v"
                }

                // Step 2: pre-populate Braintree's config cache so AmountActivity's SDK
                // call finds an immediate cache hit instead of failing its network requests.
                if (!token.startsWith("sandbox_")) {
                    try {
                        val decoded = String(android.util.Base64.decode(token, android.util.Base64.DEFAULT))
                        val configUrl = org.json.JSONObject(decoded)
                            .getString("configUrl").replace(":443", "")
                        val fingerprint = org.json.JSONObject(decoded)
                            .getString("authorizationFingerprint")

                        val fullUrl = android.net.Uri.parse(configUrl)
                            .buildUpon().appendQueryParameter("configVersion", "3")
                            .build().toString()

                        val conn = java.net.URL(fullUrl).openConnection() as javax.net.ssl.HttpsURLConnection
                        conn.connectTimeout = 15_000
                        conn.readTimeout = 15_000
                        conn.setRequestProperty("Authorization", "Bearer $fingerprint")

                        if (conn.responseCode == 200) {
                            val configJson = conn.inputStream.bufferedReader().readText()
                            val cacheKey = android.util.Base64.encodeToString(
                                fullUrl.toByteArray(Charsets.UTF_8),
                                android.util.Base64.DEFAULT
                            )
                            applicationContext
                                .getSharedPreferences("BraintreeApi", android.content.Context.MODE_PRIVATE)
                                .edit().putString(cacheKey, configJson).commit()
                            android.util.Log.d("Braintree", "Config pre-cached in MainPage")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Braintree", "Config pre-cache failed: ${e.message}")
                    }
                }

                // Step 3: launch AmountActivity — cache is ready
                runOnUiThread {
                    binding.progressBarBalance.visibility = android.view.View.GONE
                    val intent = Intent(this@MainPage, AmountActivity::class.java)
                    intent.putExtra("braintree_token", token)
                    startActivity(intent)
                }
            }.start()
        }

        binding.quickHistory.setOnClickListener {
            val intent = Intent(this@MainPage, HistoryActivity::class.java)
            startActivity(intent)
        }

        recyclerView = binding.userList
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        tranArrayList = arrayListOf<transactions>()
        getTranData()
    }

    var barcodeluncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            if (result.contents.startsWith("ACC")) {
                val intent = Intent(this, sendMoney::class.java)
                intent.putExtra("account_number", result.contents)
                startActivity(intent)
            } else {

            }
        }
    }

    private fun getTranData() {
        val mAuth = FirebaseAuth.getInstance()
        val currentUserID = mAuth.currentUser?.uid ?: return

        databaseReference = FirebaseDatabase.getInstance().getReference("history")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUserTrans = mutableListOf<PaymentTransaction>()


                for (tranSnap in snapshot.children) {
                    val transaction = tranSnap.getValue(PaymentTransaction::class.java)


                    if (transaction?.senderUserId == currentUserID ||
                        transaction?.receiverWalletID == currentUserID) {
                        transaction?.let {
                            allUserTrans.add(it)
                        }
                    }
                }


                // Compute Wealth Analytics from all user transactions
                var totalIncome = 0.0
                var totalExpenses = 0.0
                for (t in allUserTrans) {
                    val isTopUp = t.transactionType == "PLP"
                    if (t.senderUserId == currentUserID && !isTopUp) {
                        totalExpenses += t.amount
                    } else {
                        totalIncome += t.amount
                    }
                }
                updateAnalytics(totalIncome, totalExpenses, allUserTrans.size)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val sortedByDate = allUserTrans.sortedByDescending { transaction ->
                    try {
                        dateFormat.parse(transaction.date)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }


                val lastTenTransactions = sortedByDate.take(10)


                binding.progressBartran.visibility = View.GONE
                binding.userList.visibility = View.VISIBLE
                if (recyclerView.adapter == null) {
                    recyclerView.adapter = MyAdapter(ArrayList(lastTenTransactions))
                } else {
                    (recyclerView.adapter as MyAdapter).updateData(ArrayList(lastTenTransactions))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TransactionError", "Failed to read transactions: ${error.message}")
            }
        })
    }


    private val handler = Handler()
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
        binding.shieldView.visibility = View.GONE
        binding.main.visibility = View.VISIBLE
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
        binding.shieldView.visibility = View.VISIBLE
        binding.main.visibility = View.INVISIBLE
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
            currentUserWalletId = wallet?.toString() ?: ""
            binding.progressBarName.visibility = View.GONE
            binding.firstNamett.visibility = View.VISIBLE
            binding.firstNamett.text = firstName?.toString() ?: ""
            val accStr = accountNumber?.toString().orEmpty()
            binding.txtHeaderAccount.text =
                if (accStr.length >= 4) "•••• " + accStr.takeLast(4) else "•••• ••••"
            readBalance(currentUserWalletId)
            if (profileImageUrl != null) {
                Glide.with(this)
                    .load(profileImageUrl.toString())
                    .into(binding.profileImageMain)
                binding.progressBarProfileImage.visibility = View.GONE
                binding.profileImageMain.visibility = View.VISIBLE
            }
            if (profileImageUrl == null) {
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
            val balanceDouble = it.child("Balance").value?.toString()?.toDoubleOrNull() ?: 0.0
            val formattedBalance = "%.2f".format(balanceDouble)
            binding.balance.text = "$formattedBalance$"
        }
    }

    fun showBanner() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("banners")
        databaseRef.child("bannersMain").child("banner1").get().addOnSuccessListener {
            val bannerUrl = it.value?.toString()
            binding.progressBarBanner.visibility = View.GONE
            if (!bannerUrl.isNullOrEmpty()) {
                Glide.with(this@MainPage).load(bannerUrl).into(binding.bannerImg)
                binding.bannerImg.visibility = View.VISIBLE
            }
        }.addOnFailureListener {
            binding.progressBarBanner.visibility = View.GONE
        }
    }

    // Promotional cards above the balance nameplate, read from the `ads` node.
    private fun loadAds() {
        binding.adsList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        FirebaseDatabase.getInstance().getReference("homeCards")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("HomeCards", "onDataChange exists=${snapshot.exists()} count=${snapshot.childrenCount}")
                    // Parse defensively so values typed as strings in the console still work.
                    val ads = snapshot.children.mapNotNull { child ->
                        val title = child.child("title").value?.toString().orEmpty()
                        if (title.isBlank()) return@mapNotNull null
                        val subtitle = child.child("subtitle").value?.toString().orEmpty()
                        val imageUrl = child.child("imageUrl").value?.toString().orEmpty()
                        val order = child.child("order").value?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                        val active = when (val a = child.child("active").value) {
                            is Boolean -> a
                            is String -> !a.equals("false", true)
                            null -> true
                            else -> true
                        }
                        AdItem(title, subtitle, imageUrl, order, active)
                    }.filter { it.active }.sortedBy { it.order }
                    Log.d("HomeCards", "usable cards=${ads.size}")
                    if (ads.isEmpty()) {
                        binding.adsList.visibility = View.GONE
                    } else {
                        binding.adsList.adapter = AdsAdapter(ads)
                        binding.adsList.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("HomeCards", "cancelled: ${error.message}")
                    binding.adsList.visibility = View.GONE
                }
            })
    }

    private fun updateAnalytics(income: Double, expenses: Double, count: Int) {
        binding.analyticsProgressBar.visibility = View.GONE
        binding.tvIncome.text = "$%.2f".format(income)
        binding.tvExpenses.text = "$%.2f".format(expenses)

        val max = maxOf(income, expenses)
        if (max > 0) {
            binding.incomeProgressBar.progress = ((income / max) * 100).toInt()
            binding.expenseProgressBar.progress = ((expenses / max) * 100).toInt()
        } else {
            binding.incomeProgressBar.progress = 0
            binding.expenseProgressBar.progress = 0
        }

        binding.tvTotalTransactions.text = "$count total transactions"
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