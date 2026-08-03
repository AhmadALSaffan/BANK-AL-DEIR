package bankal_deir.com.Fatora.UI.fragments.university

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import bankal_deir.com.Fatora.Data.PaymentConstants
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.Fatora.Data.PaymentUtils
import bankal_deir.com.Fatora.Data.PaymentUtils.generateReferenceNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.generateTransactionNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.getCurrentDate
import bankal_deir.com.Fatora.Data.PaymentUtils.isValidStudentNumber
import bankal_deir.com.MainPage
import bankal_deir.com.R
import bankal_deir.com.OtpGate
import bankal_deir.com.databinding.FragmentTuitionPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class TuitionPaymentFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentTuitionPaymentBinding? = null
    private val binding get() = _binding!!
    private var universityName: String? = null
    private val database = FirebaseDatabase.getInstance()


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            arguments?.let {
                universityName = it.getString("university_name")
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentTuitionPaymentBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // Display university name
            binding.universityNameTextView.text = universityName ?: "Not selected"

            setupClickListeners()
        }

        private fun setupClickListeners() {
            binding.continueButton.setOnClickListener {
                processPayment()
            }
        }

        private fun processPayment() {
            val studentNumber = binding.studentNumberEditText.text.toString().trim()
            val amountText = binding.amountEditText.text.toString().trim()

            if (!isValidStudentNumber(studentNumber)) {
                Toast.makeText(requireContext(), "Student number is required", Toast.LENGTH_SHORT).show()
                return
            }

            if (amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Amount is required", Toast.LENGTH_SHORT).show()
                return
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                return
            }


            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }


            binding.continueButton.isEnabled = false
            binding.continueButton.text = "Processing..."


            OtpGate.require(requireActivity(), onCancelled = { binding.continueButton.isEnabled = true; binding.continueButton.text = "Continue to payment" }) { getUserWalletId(userId, amount, studentNumber) }
        }

        private fun getUserWalletId(userId: String, amount: Double, studentNumber: String) {
            val usersRef = database.getReference("users").child(userId).child("walletId")

            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val walletId = snapshot.getValue(String::class.java)

                    if (walletId.isNullOrEmpty()) {
                        showError("Wallet not found")
                        return
                    }


                    checkBalanceAndPay(userId, walletId, amount, studentNumber)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error fetching wallet: ${error.message}")
                }
            })
        }

        private fun checkBalanceAndPay(userId: String, walletId: String, amount: Double, studentNumber: String) {
            val walletRef = database.getReference("wallets").child(walletId).child("Balance")

            walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0

                    if (currentBalance < amount) {
                        showError("Insufficient balance. Current: $$currentBalance, Required: $$amount")
                        return
                    }


                    val newBalance = currentBalance - amount
                    deductBalanceAndSaveTransaction(userId, walletId, newBalance, amount, studentNumber)
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
            amount: Double,
            studentNumber: String
        ) {
            val transactionNumber = generateTransactionNumber("SUN")
            val referenceNumber = generateReferenceNumber()
            val currentDate = getCurrentDate()


            val transaction = PaymentTransaction(
                transactionNumber = transactionNumber,
                senderUserId = userId,
                amount = amount,
                date = currentDate,
                transactionType = "SUN",
                referenceNumber = referenceNumber,
                university = universityName ?: "",
                studentNumber = studentNumber
            )

            val walletRef = database.getReference("wallets").child(walletId).child("Balance")
            val historyRef = database.getReference("history").child(transactionNumber)

            walletRef.setValue(newBalance)
                .addOnSuccessListener {
                    historyRef.setValue(transaction)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Payment successful!\nTransaction: $transactionNumber\nNew Balance: $$newBalance",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(requireContext(), MainPage::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { exception ->
                            walletRef.setValue(newBalance + amount)
                            showError("Transaction save failed: ${exception.message}")
                        }
                }
                .addOnFailureListener { exception ->
                    showError("Balance update failed: ${exception.message}")
                }
        }


        private fun showError(message: String) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            binding.continueButton.isEnabled = true
            binding.continueButton.text = "Continue to Payment"
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }