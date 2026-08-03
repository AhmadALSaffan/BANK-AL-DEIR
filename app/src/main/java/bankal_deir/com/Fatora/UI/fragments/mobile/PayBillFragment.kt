package bankal_deir.com.Fatora.UI.fragments.mobile

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
import bankal_deir.com.Fatora.Data.PaymentUtils.generateOrderNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.generateTransactionNumber
import bankal_deir.com.Fatora.Data.PaymentUtils.getCurrentDate
import bankal_deir.com.Fatora.Data.PaymentUtils.isValidMobileNumber
import bankal_deir.com.MainPage
import bankal_deir.com.databinding.FragmentPayBillBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*




class PayBillFragment : Fragment() {

    private var _binding: FragmentPayBillBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseDatabase.getInstance()
    private var selectedProvider: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardMTN.setOnClickListener {
            selectProvider("MTN")
        }

        binding.cardSyriatel.setOnClickListener {
            selectProvider("Syriatel")
        }

        binding.cardSyrianTelecom.setOnClickListener {
            selectProvider("Syrian Telecom")
        }

        binding.continueButton.setOnClickListener {
            processPayment()
        }
    }

    private fun selectProvider(provider: String) {
        selectedProvider = provider

        binding.mtnCheckIcon.visibility = View.INVISIBLE
        binding.syriatelCheckIcon.visibility = View.INVISIBLE
        binding.syrianTelecomCheckIcon.visibility = View.INVISIBLE

        when (provider) {
            "MTN" -> binding.mtnCheckIcon.visibility = View.VISIBLE
            "Syriatel" -> binding.syriatelCheckIcon.visibility = View.VISIBLE
            "Syrian Telecom" -> binding.syrianTelecomCheckIcon.visibility = View.VISIBLE
        }
    }

    private fun processPayment() {
        val mobileNumber = binding.mobileNumberEditText.text.toString().trim()
        val amountText = binding.amountEditText.text.toString().trim()

        if (selectedProvider.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a provider", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidMobileNumber(mobileNumber)){
            Toast.makeText(requireContext(), "Mobile number is not Valid or Empty", Toast.LENGTH_SHORT).show()
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

        OtpGate.require(requireActivity(), onCancelled = { binding.continueButton.isEnabled = true; binding.continueButton.text = "Continue to payment" }) { getUserWalletId(userId, amount, mobileNumber) }
    }

    private fun getUserWalletId(userId: String, amount: Double, mobileNumber: String) {
        val usersRef = database.getReference("users").child(userId).child("walletId")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val walletId = snapshot.getValue(String::class.java)

                if (walletId.isNullOrEmpty()) {
                    showError("Wallet not found")
                    return
                }

                checkBalanceAndPay(userId, walletId, amount, mobileNumber)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Error fetching wallet: ${error.message}")
            }
        })
    }

    private fun checkBalanceAndPay(userId: String, walletId: String, amount: Double, mobileNumber: String) {
        val walletRef = database.getReference("wallets").child(walletId).child("Balance")

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0

                if (currentBalance < amount) {
                    showError("Insufficient balance. Current: $$currentBalance, Required: $$amount")
                    return
                }

                val newBalance = currentBalance - amount
                deductBalanceAndSaveTransaction(userId, walletId, newBalance, amount, mobileNumber)
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
        mobileNumber: String
    ) {
        val transactionNumber = generateTransactionNumber("SMP")
        val orderNumber = generateOrderNumber()
        val currentDate = getCurrentDate()

        val transaction = PaymentTransaction(
            transactionNumber = transactionNumber,
            senderUserId = userId,
            amount = amount,
            date = currentDate,
            transactionType = "SMP",
            orderNumber = orderNumber,
            mobileNumber = mobileNumber,
            provider = selectedProvider
        )

        val walletRef = database.getReference("wallets").child(walletId).child("Balance")
        val historyRef = database.getReference("history").child(transactionNumber)

        walletRef.setValue(newBalance)
            .addOnSuccessListener {
                historyRef.setValue(transaction)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Payment successful!\nProvider: $selectedProvider",
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