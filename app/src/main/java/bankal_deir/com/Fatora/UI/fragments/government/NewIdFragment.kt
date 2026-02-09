package bankal_deir.com.Fatora.UI.fragments.government

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bankal_deir.com.R
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.Fatora.Data.PaymentUtils.isValidIdNumber
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.FragmentNewIdBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*


class NewIdFragment : Fragment() {

        private var _binding: FragmentNewIdBinding? = null
        private val binding get() = _binding!!

        private val database = FirebaseDatabase.getInstance()
        private val FIXED_AMOUNT = 20.0

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentNewIdBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setupClickListeners()
        }

        private fun setupClickListeners() {
            binding.continueButton.setOnClickListener {
                processPayment()
            }
        }

        private fun processPayment() {
            val idNumber = binding.idNumberEditText.text.toString().trim()

            if (!isValidIdNumber(idNumber)) {
                Toast.makeText(requireContext(), "ID number is required", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }

            binding.continueButton.isEnabled = false
            binding.continueButton.text = "Processing..."

            getUserWalletId(userId, idNumber)
        }

        private fun getUserWalletId(userId: String, idNumber: String) {
            val usersRef = database.getReference("users").child(userId).child("walletId")

            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val walletId = snapshot.getValue(String::class.java)

                    if (walletId.isNullOrEmpty()) {
                        showError("Wallet not found")
                        return
                    }

                    checkBalanceAndPay(userId, walletId, idNumber)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error fetching wallet: ${error.message}")
                }
            })
        }

        private fun checkBalanceAndPay(userId: String, walletId: String, idNumber: String) {
            val walletRef = database.getReference("wallets").child(walletId).child("Balance")

            walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0

                    if (currentBalance < FIXED_AMOUNT) {
                        showError("Insufficient balance. Current: $$currentBalance, Required: $$FIXED_AMOUNT")
                        return
                    }

                    val newBalance = currentBalance - FIXED_AMOUNT
                    deductBalanceAndSaveTransaction(userId, walletId, newBalance, idNumber)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error checking balance: ${error.message}")
                }
            })
        }

        private fun deductBalanceAndSaveTransaction(
            userId: String,
            walletId: String,
            newBalance: Double,
            idNumber: String
        ) {
            val transactionNumber = generateTransactionNumber("SGD")
            val orderNumber = generateOrderNumber()
            val currentDate = getCurrentDate()

            val transaction = PaymentTransaction(
                transactionNumber = transactionNumber,
                senderUserId = userId,
                amount = FIXED_AMOUNT,
                date = currentDate,
                transactionType = "SGD",
                orderNumber = orderNumber,
                idNumber = idNumber
            )

            val walletRef = database.getReference("wallets").child(walletId).child("Balance")
            val historyRef = database.getReference("history").child(transactionNumber)

            walletRef.setValue(newBalance)
                .addOnSuccessListener {
                    historyRef.setValue(transaction)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Payment successful!\nTransaction: $transactionNumber\nOrder: $orderNumber\nNew Balance: $$newBalance",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(requireContext(), MainPage::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { exception ->
                            walletRef.setValue(newBalance + FIXED_AMOUNT)
                            showError("Transaction save failed: ${exception.message}")
                        }
                }
                .addOnFailureListener { exception ->
                    showError("Balance update failed: ${exception.message}")
                }
        }

        private fun generateTransactionNumber(prefix: String): String {
            val randomNumbers = (1..17).map { ('0'..'9').random() }.joinToString("")
            return "$prefix$randomNumbers"
        }

        private fun generateOrderNumber(): String {
            return (1..10).map { ('0'..'9').random() }.joinToString("")
        }

        private fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }

        private fun showError(message: String) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            binding.continueButton.isEnabled = true
            binding.continueButton.text = "Pay $20 and Continue"
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }