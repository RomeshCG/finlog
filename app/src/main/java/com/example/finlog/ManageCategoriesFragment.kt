package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finlog.databinding.FragmentManageCategoriesBinding
import com.example.finlog.databinding.ItemCategoryManageBinding // We'll create this layout next

class ManageCategoriesFragment : Fragment() {

    private var _binding: FragmentManageCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageCategoriesBinding.inflate(inflater, container, false)
        dataManager = DataManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadCategories()

        binding.addCategoryButton.setOnClickListener {
            val newCategoryName = binding.newCategoryEditText.text.toString().trim()
            if (newCategoryName.isNotEmpty()) {
                addCategory(newCategoryName)
            } else {
                Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onDeleteClick = { categoryName ->
                showDeleteConfirmationDialog(categoryName)
            }
        )
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoriesRecyclerView.adapter = adapter
    }

    private fun loadCategories() {
        val categories = dataManager.getUserCategories()
        adapter.submitList(categories)
    }

    private fun addCategory(name: String) {
        if (dataManager.addCategory(name)) {
            Toast.makeText(context, "Category '$name' added", Toast.LENGTH_SHORT).show()
            binding.newCategoryEditText.text.clear()
            loadCategories() // Refresh the list
        } else {
            Toast.makeText(context, "Category '$name' already exists or is invalid", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(categoryName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete the category '$categoryName'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(categoryName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(name: String) {
        if (dataManager.removeCategory(name)) {
            Toast.makeText(context, "Category '$name' deleted", Toast.LENGTH_SHORT).show()
            loadCategories() // Refresh the list
        } else {
            Toast.makeText(context, "Failed to delete category '$name'", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// --- RecyclerView Adapter for Categories ---
class CategoryAdapter(
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories: List<String> = emptyList()

    fun submitList(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged() // Simple refresh, consider DiffUtil for efficiency later
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryManageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(
        private val binding: ItemCategoryManageBinding,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryName: String) {
            binding.categoryNameTextView.text = categoryName
            binding.deleteCategoryButton.setOnClickListener {
                onDeleteClick(categoryName)
            }
        }
    }
} 