package bankal_deir.com.Fatora.UI.fragments.university

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import bankal_deir.com.R
import bankal_deir.com.databinding.FragmentUniversityMenuBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UniversityMenuFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentUniversityMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUniversityMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardTuition.setOnClickListener {
            val bundle = bundleOf("payment_type" to "tuition")
            findNavController().navigate(R.id.action_university_to_universitySelection, bundle)
        }

        binding.cardFines.setOnClickListener {
            val bundle = bundleOf("payment_type" to "fines")
            findNavController().navigate(R.id.action_university_to_universitySelection, bundle)
        }

        binding.cardHostel.setOnClickListener {
            val bundle = bundleOf("payment_type" to "hostel")
            findNavController().navigate(R.id.action_university_to_universitySelection, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}