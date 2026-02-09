package bankal_deir.com.Fatora.UI.fragments.university

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import bankal_deir.com.R
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import bankal_deir.com.Fatora.Data.UniversityAdapter
import bankal_deir.com.Fatora.Data.University
import bankal_deir.com.Fatora.Data.UniversityType
import bankal_deir.com.databinding.FragmentUniversitySelectionBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UniversitySelectionFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentUniversitySelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UniversityAdapter
    private val allUniversities = getSyrianUniversities()
    private var paymentType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paymentType = it.getString("payment_type")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUniversitySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
    }

    private fun setupRecyclerView() {
        adapter = UniversityAdapter { university ->
            onUniversitySelected(university)
        }

        binding.universitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UniversitySelectionFragment.adapter
        }

        adapter.submitList(allUniversities)
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isEmpty()) {
                adapter.submitList(allUniversities)
            } else {
                val filtered = allUniversities.filter {
                    it.name.contains(query, ignoreCase = true)
                }
                adapter.submitList(filtered)
            }
        }
    }

    private fun onUniversitySelected(university: University) {
        // Navigate to appropriate payment fragment based on payment type
        val bundle = bundleOf("university_name" to university.name)

        when (paymentType) {
            "tuition" -> {
                findNavController().navigate(
                    R.id.action_universitySelection_to_tuitionPayment,
                    bundle
                )
            }
            "fines" -> {
                findNavController().navigate(
                    R.id.action_universitySelection_to_finesPayment,
                    bundle
                )
            }
            "hostel" -> {
                findNavController().navigate(
                    R.id.action_universitySelection_to_hostelPayment,
                    bundle
                )
            }
        }
    }

    private fun getSyrianUniversities(): List<University> {
        return listOf(
            University(1, "Damascus University", UniversityType.PUBLIC),
            University(2, "Aleppo University", UniversityType.PUBLIC),
            University(3, "Tishreen University", UniversityType.PUBLIC),
            University(4, "Al-Baath University", UniversityType.PUBLIC),
            University(5, "Al-Furat University", UniversityType.PUBLIC),
            University(6, "Hama University", UniversityType.PUBLIC),
            University(7, "Tartous University", UniversityType.PUBLIC),
            University(8, "Syrian Virtual University", UniversityType.PUBLIC),

            // Private Universities
            University(9, "International University for Science and Technology", UniversityType.PRIVATE),
            University(10, "Syrian Private University", UniversityType.PRIVATE),
            University(11, "Arab International University", UniversityType.PRIVATE),
            University(12, "University of Kalamoon", UniversityType.PRIVATE),
            University(13, "Wadi International University", UniversityType.PRIVATE),
            University(14, "Al-Andalus University for Medical Sciences", UniversityType.PRIVATE),
            University(15, "Al-Jazeera University", UniversityType.PRIVATE),
            University(16, "Al-Wataniya Private University", UniversityType.PRIVATE),
            University(17, "Al-Rashid Private University for Science and Technology", UniversityType.PRIVATE),
            University(18, "Hawash Private University", UniversityType.PRIVATE),
            University(19, "Ebla Private University", UniversityType.PRIVATE),
            University(20, "Cordoba Private University", UniversityType.PRIVATE),
            University(21, "Al-Shahba University", UniversityType.PRIVATE),
            University(22, "Antioch Syrian University", UniversityType.PRIVATE),
            University(23, "Manara University", UniversityType.PRIVATE),
            University(24, "Sham University", UniversityType.PRIVATE),
            University(25, "Al-Ittihad Private University", UniversityType.PRIVATE),
            University(26, "Arab University for Science and Technology", UniversityType.PRIVATE),

            // Institutes
            University(27, "Higher Institute for Applied Sciences and Technology", UniversityType.INSTITUTE),
            University(28, "National Institute of Administration", UniversityType.INSTITUTE)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}