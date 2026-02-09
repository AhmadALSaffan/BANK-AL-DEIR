package bankal_deir.com.Fatora.UI.fragments.mobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import bankal_deir.com.R
import bankal_deir.com.databinding.FragmentMobileMenuBinding


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MobileMenuFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentMobileMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMobileMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardPayBill.setOnClickListener {
            findNavController().navigate(R.id.action_mobile_to_payBill)
        }

        binding.cardRefillBalance.setOnClickListener {
            findNavController().navigate(R.id.action_mobile_to_refill)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}