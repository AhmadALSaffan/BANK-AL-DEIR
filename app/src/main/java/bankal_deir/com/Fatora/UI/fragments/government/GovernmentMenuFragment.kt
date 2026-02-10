package bankal_deir.com.Fatora.UI.fragments.government

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import bankal_deir.com.R
import bankal_deir.com.databinding.FragmentGovernmentMenuBinding
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale


class GovernmentMenuFragment : Fragment() {
    private var _binding: FragmentGovernmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGovernmentMenuBinding.inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardNewPassport.setOnClickListener {
            findNavController().navigate(R.id.action_government_to_passport)
        }

        binding.cardImmigrationFine.setOnClickListener {
            findNavController().navigate(R.id.action_government_to_immigration)
        }

        binding.cardNewId.setOnClickListener {
            findNavController().navigate(R.id.action_government_to_newId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}