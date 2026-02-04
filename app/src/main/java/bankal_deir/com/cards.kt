package bankal_deir.com

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.databinding.ActivityCardsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.logging.Handler

class cards : AppCompatActivity() {
    private lateinit var binding: ActivityCardsBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseReference : FirebaseDatabase
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CardAdapter
    private var refreshRunnable: Runnable? = null
    private val handler = android.os.Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recycler = binding.cardsList
        adapter = CardAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        recycler.adapter = adapter
        loadUserCards()
        binding.btnCreateCard.setOnClickListener {
            val intent = Intent(this, createNewCard::class.java)
            startActivity(intent)
        }
    }
    private fun loadUserCards() {
        val progressDialog = Dialog(this)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(R.layout.progress)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.show()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("CardsActivity", "No user signed in")
            progressDialog.dismiss()
            return
        }

        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        userRef.child("walletId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val walletId = userSnapshot.value?.toString()

                if (walletId.isNullOrEmpty()) {
                    progressDialog.dismiss()
                    adapter.update(emptyList())
                    return
                }

                val cardsRef = FirebaseDatabase.getInstance()
                    .getReference("wallets")
                    .child(walletId)
                    .child("cards")

                cardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cardList = mutableListOf<CardModel>()
                        for (cardSnap in snapshot.children) {
                            val card = cardSnap.getValue(CardModel::class.java)
                            if (card != null) {
                                cardList.add(card)
                            } else {
                                Log.w("CardsActivity", "Null card at ${cardSnap.key}")
                            }
                        }
                        Log.d("CardsActivity", "Fetched ${cardList.size} cards from wallet $walletId")
                        adapter.update(cardList)
                        progressDialog.dismiss()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CardsActivity", "Database error: ${error.message}")
                        adapter.update(emptyList())
                        progressDialog.dismiss()
                        scheduleRefresh()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CardsActivity", "Error fetching walletId: ${error.message}")
                adapter.update(emptyList())
                progressDialog.dismiss()
                scheduleRefresh()
            }
        })
    }


    private fun scheduleRefresh() {
        refreshRunnable?.let { handler.removeCallbacks(it) }

        refreshRunnable = Runnable {
            loadUserCardsQuiet()
        }

        handler.postDelayed(refreshRunnable!!, 3000)
    }

    private fun loadUserCardsQuiet() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("CardsActivity", "No user signed in")
            return
        }

        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        userRef.child("walletId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val walletId = userSnapshot.value?.toString()

                if (walletId.isNullOrEmpty()) {
                    Log.e("CardsActivity", "No walletId found for user")
                    scheduleRefresh()
                    return
                }

                val cardsRef = FirebaseDatabase.getInstance()
                    .getReference("wallets")
                    .child(walletId)
                    .child("cards")

                cardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cardList = mutableListOf<CardModel>()
                        for (cardSnap in snapshot.children) {
                            val card = cardSnap.getValue(CardModel::class.java)
                            if (card != null) {
                                cardList.add(card)
                            }
                        }
                        Log.d("CardsActivity", "Auto-refreshed: ${cardList.size} cards")
                        adapter.update(cardList)

                        scheduleRefresh()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CardsActivity", "Auto-refresh error: ${error.message}")
                        scheduleRefresh()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CardsActivity", "Auto-refresh error fetching walletId: ${error.message}")
                scheduleRefresh()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onPause() {
        super.onPause()
        refreshRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        scheduleRefresh()
    }

}