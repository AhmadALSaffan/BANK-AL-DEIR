package bankal_deir.com.Fatora.Data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import bankal_deir.com.Fatora.Data.University
import bankal_deir.com.Fatora.Data.UniversityType
import bankal_deir.com.databinding.ItemUniversityBinding

class UniversityAdapter(
    private val onUniversityClick: (University) -> Unit
) : ListAdapter<University, UniversityAdapter.UniversityViewHolder>(UniversityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UniversityViewHolder {
        val binding = ItemUniversityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UniversityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UniversityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UniversityViewHolder(
        private val binding: ItemUniversityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(university: University) {
            binding.universityNameTextView.text = university.name
            binding.universityTypeTextView.text = when (university.type) {
                UniversityType.PUBLIC -> "Public University"
                UniversityType.PRIVATE -> "Private University"
                UniversityType.INSTITUTE -> "Institute"
            }

            binding.root.setOnClickListener {
                onUniversityClick(university)
            }
        }
    }

    class UniversityDiffCallback : DiffUtil.ItemCallback<University>() {
        override fun areItemsTheSame(oldItem: University, newItem: University): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: University, newItem: University): Boolean {
            return oldItem == newItem
        }
    }
}