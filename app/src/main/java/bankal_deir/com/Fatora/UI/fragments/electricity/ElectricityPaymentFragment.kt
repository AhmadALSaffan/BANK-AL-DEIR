package bankal_deir.com.Fatora.UI.fragments.electricity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bankal_deir.com.R
import bankal_deir.com.OtpGate
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import bankal_deir.com.Fatora.Data.PaymentTransaction
import bankal_deir.com.Fatora.Data.PaymentUtils.generateReferenceNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.generateTransactionNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.getCurrentDate
import bankal_deir.com.Fatora.Data.PaymentUtils.isBillValid
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.FragmentElectricityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ElectricityPaymentFragment : Fragment() {
    private var _binding: FragmentElectricityPaymentBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseDatabase.getInstance()
    private var selectedCompany: String = "" // "Green Energy" or "Ministry of Electricity"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElectricityPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Green Energy Selection
        binding.cardGreenEnergy.setOnClickListener {
            selectCompany("Green Energy")
        }

        // Ministry of Electricity Selection
        binding.cardMinistryElectricity.setOnClickListener {
            selectCompany("Ministry of Electricity")
        }

        // Continue Button
        binding.continueButton.setOnClickListener {
            processPayment()
        }
    }

    private fun selectCompany(company: String) {
        selectedCompany = company

        // Reset all check icons
        binding.greenEnergyCheckIcon.visibility = View.INVISIBLE
        binding.ministryCheckIcon.visibility = View.INVISIBLE

        // Show selected check icon
        when (company) {
            "Green Energy" -> binding.greenEnergyCheckIcon.visibility = View.VISIBLE
            "Ministry of Electricity" -> binding.ministryCheckIcon.visibility = View.VISIBLE
        }
    }

    private fun processPayment() {
        val billNumber = binding.billNumberEditText.text.toString().trim()
        val amountText = binding.amountEditText.text.toString().trim()

        if (selectedCompany.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a company", Toast.LENGTH_SHORT).show()
            return
        }

        if (billNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Bill number is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isBillValid(billNumber)) {
            Toast.makeText(requireContext(), "Bill number must be exactly 9 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Bill amount is required", Toast.LENGTH_SHORT).show()
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


        OtpGate.require(requireActivity(), onCancelled = { binding.continueButton.isEnabled = true; binding.continueButton.text = "Continue to payment" }) { getUserWalletId(userId, amount, billNumber) }
    }

    private fun getUserWalletId(userId: String, amount: Double, billNumber: String) {
        val usersRef = database.getReference("users").child(userId).child("walletId")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val walletId = snapshot.getValue(String::class.java)

                if (walletId.isNullOrEmpty()) {
                    showError("Wallet not found")
                    return
                }

                checkBalanceAndPay(userId, walletId, amount, billNumber)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Error fetching wallet: ${error.message}")
            }
        })
    }

    private fun checkBalanceAndPay(userId: String, walletId: String, amount: Double, billNumber: String) {
        val walletRef = database.getReference("wallets").child(walletId).child("Balance")

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0

                if (currentBalance < amount) {
                    showError("Insufficient balance. Current: $$currentBalance, Required: $$amount")
                    return
                }

                val newBalance = currentBalance - amount
                deductBalanceAndSaveTransaction(userId, walletId, newBalance, amount, billNumber)
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
        billNumber: String
    ) {
        val transactionNumber = generateTransactionNumber("SEL")
        val referenceNumber = generateReferenceNumber()
        val currentDate = getCurrentDate()

        val transaction = PaymentTransaction(
            transactionNumber = transactionNumber,
            senderUserId = userId,
            amount = amount,
            date = currentDate,
            transactionType = "SEL",
            referenceNumber = referenceNumber,
            company = selectedCompany,
            billNumber = billNumber
        )

        val walletRef = database.getReference("wallets").child(walletId).child("Balance")
        val historyRef = database.getReference("history").child(transactionNumber)

        walletRef.setValue(newBalance)
            .addOnSuccessListener {
                historyRef.setValue(transaction)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Payment successful!\nCompany: $selectedCompany\nTransaction: $transactionNumber\nReference: $referenceNumber\nNew Balance: $$newBalance",
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