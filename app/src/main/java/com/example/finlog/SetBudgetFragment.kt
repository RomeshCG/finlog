package com.example.finlog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finlog.databinding.FragmentSetBudgetBinding
import com.example.finlog.databinding.ItemCategoryBudgetBinding
import com.example.finlog.MonthlyBudget

class SetBudgetFragment : Fragment() {

    private var _binding: FragmentSetBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var adapter: CategoryBudgetAdapter
    private var currentTotalBudget: Double = 0.0
    private val TAG = "SetBudgetFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetBudgetBinding.inflate(inflater, container, false)
        dataManager = DataManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupRecyclerView()
        loadCurrentBudget()

        binding.saveBudgetButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryBudgetAdapter(mutableMapOf())
        binding.categoryBudgetsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoryBudgetsRecyclerView.adapter = adapter
    }

    private fun loadCurrentBudget() {
        Log.d(TAG, "Loading current budget")
        val monthlyBudget = dataManager.getMonthlyBudget()
        currentTotalBudget = monthlyBudget.total
        binding.totalBudgetEditText.setText(String.format("%.2f", currentTotalBudget))

        val userCategories = dataManager.getUserCategories().sorted()
        val initialCategoryBudgets = userCategories.associateWith { category ->
            monthlyBudget.categories.getOrDefault(category, 0.0)
        }

        Log.d(TAG, "Loaded categories for budget adapter: ${initialCategoryBudgets.keys}")
        adapter.updateData(initialCategoryBudgets)
    }

    private fun saveBudget() {
        Log.d(TAG, "Attempting to save budget")
        val totalBudgetText = binding.totalBudgetEditText.text.toString()
        val totalBudget = totalBudgetText.toDoubleOrNull()

        if (totalBudget == null || totalBudget < 0) {
            Toast.makeText(context, "Invalid total budget amount", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Save failed: Invalid total budget amount: $totalBudgetText")
            return
        }

        val finalCategoryBudgets = adapter.getCategoryBudgets()

        val sumOfCategories = finalCategoryBudgets.values.sum()
        if (sumOfCategories > totalBudget) {
            Log.w(TAG, "Sum of category budgets ($sumOfCategories) exceeds total budget ($totalBudget)")
        }

        val newBudget = MonthlyBudget(total = totalBudget, categories = finalCategoryBudgets)
        dataManager.saveMonthlyBudget(newBudget)

        Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Budget saved: Total=$totalBudget, Categories=$finalCategoryBudgets")
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView")
    }
}

class CategoryBudgetAdapter(
    initialBudgets: MutableMap<String, Double>
) : RecyclerView.Adapter<CategoryBudgetAdapter.CategoryBudgetViewHolder>() {

    private val categoryBudgets: MutableMap<String, Double> = initialBudgets.toMutableMap()
    private var categoryKeys: List<String> = initialBudgets.keys.toList().sorted()

    fun updateData(newBudgets: Map<String, Double>) {
        categoryBudgets.clear()
        categoryBudgets.putAll(newBudgets)
        categoryKeys = newBudgets.keys.toList().sorted()
        notifyDataSetChanged()
        Log.d("CategoryBudgetAdapter", "Data updated. Keys: $categoryKeys")
    }

    fun getCategoryBudgets(): Map<String, Double> {
        return categoryBudgets.toMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryBudgetViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryBudgetViewHolder(binding, categoryBudgets)
    }

    override fun onBindViewHolder(holder: CategoryBudgetViewHolder, position: Int) {
        if (position < categoryKeys.size) {
            val categoryName = categoryKeys[position]
            holder.bind(categoryName)
        } else {
            Log.e("CategoryBudgetAdapter", "Invalid position $position for categoryKeys size ${categoryKeys.size}")
        }
    }

    override fun getItemCount(): Int = categoryKeys.size

    class CategoryBudgetViewHolder(
        private val binding: ItemCategoryBudgetBinding,
        private val categoryBudgetsMap: MutableMap<String, Double>
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCategoryName: String? = null

        init {
            binding.categoryBudgetEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val budgetValue = s?.toString()?.toDoubleOrNull() ?: 0.0
                    currentCategoryName?.let {
                        if (categoryBudgetsMap.containsKey(it)) {
                            categoryBudgetsMap[it] = budgetValue
                            Log.d("CategoryBudgetVH", "Map updated for $it: $budgetValue")
                        }
                    }
                }
            })
        }

        fun bind(categoryName: String) {
            currentCategoryName = categoryName
            binding.categoryBudgetNameTextView.text = categoryName

            val currentBudgetValue = categoryBudgetsMap.getOrDefault(categoryName, 0.0)
            binding.categoryBudgetEditText.setText(String.format("%.2f", currentBudgetValue))
            Log.d("CategoryBudgetVH", "Binding $categoryName with budget $currentBudgetValue")
        }
    }
} 