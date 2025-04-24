package com.example.finlog

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finlog.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val TAG = "HomeFragment"

    // --- Navigation Listener Interface ---
    interface HomeFragmentListener {
        fun navigateToManageCategories()
        fun navigateToSetBudget()
    }

    private var listener: HomeFragmentListener? = null

    override fun onAttach(context: android.content.Context) { // Use android.content.Context
        super.onAttach(context)
        if (context is HomeFragmentListener) {
            listener = context
        } else {
            // Log error instead of crashing in case activity doesn't implement immediately
            Log.e(TAG, "$context must implement HomeFragmentListener")
            // throw RuntimeException("$context must implement HomeFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null // Prevent memory leaks
    }
    // --- End Navigation Listener Interface ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        // Initialize DataManager
        dataManager = DataManager(requireContext())

        // Setup initial UI
        updateUI()

        // Handle account dropdown click
        binding.accountDropdown.setOnClickListener {
            showAccountSelectionDialog()
        }

        // Direct account item clicks (removed, handled by dialog)
        // binding.accountTotal.setOnClickListener { selectAccountAndUpdate("Total") }
        // binding.accountCreditCard.setOnClickListener { selectAccountAndUpdate("Credit Card") }
        // binding.accountDebitCard.setOnClickListener { selectAccountAndUpdate("Debit Card") }
        // binding.accountCash.setOnClickListener { selectAccountAndUpdate("Cash") }

        // Handle menu icon click (placeholder)
        binding.menuIcon.setOnClickListener {
             Log.d(TAG, "Menu icon clicked")
            // Consider navigating to MenuFragment or showing a settings menu
        }

        // Handle "All Budgets" click
        binding.allBudgets.setOnClickListener {
             Log.d(TAG, "All Budgets clicked - notifying listener")
             listener?.navigateToSetBudget() // Call the listener in the activity
        }

        // Handle "Statistics" click
        binding.statistics.setOnClickListener {
            Log.d(TAG, "Statistics clicked - notifying listener")
            listener?.navigateToManageCategories() // Call the listener in the activity
        }
    }

    override fun onResume() {
        super.onResume()
         Log.d(TAG, "onResume called, updating UI")
        updateUI() // Refresh UI when returning to the fragment
    }

    private fun updateUI() {
        Log.d(TAG, "Updating UI")
        // Update date
        binding.dateText.text = dataManager.getCurrentDate()

        // Update selected account balance display
        val selectedAccount = dataManager.getSelectedAccount()
        binding.totalBalance.text = String.format("LKR %.2f", selectedAccount.balance)
        // Optionally update the dropdown indicator text if needed
        // binding.selectedAccountName.text = selectedAccount.name // Assuming you add a TextView for this
        Log.d(TAG, "Selected Account: ${selectedAccount.name}, Balance: ${selectedAccount.balance}")


        // --- Update Budget Section (Current Month) ---
        val budgetLeft = dataManager.getBudgetLeftCurrentMonth()
        val budgetSpent = dataManager.getTotalBudgetSpentCurrentMonth()
        val budgetAllocated = dataManager.getTotalBudgetAllocatedCurrentMonth()
        binding.budgetLeft.text = String.format("LKR %.2f left", budgetLeft)
        binding.budgetSpent.text = String.format("LKR %.2f spent of LKR %.2f", budgetSpent, budgetAllocated)
        Log.d(TAG, "Budget - Left: $budgetLeft, Spent: $budgetSpent, Allocated: $budgetAllocated")


        // --- Update Budget Categories (Current Month) ---
        val categories = dataManager.getBudgetCategoriesCurrentMonth()
        binding.budgetCategoryList.removeAllViews() // Clear previous category views

        if (categories.isNotEmpty()) {
             Log.d(TAG, "Displaying ${categories.size} budget categories")
            categories.forEach { category ->
                val categoryView = layoutInflater.inflate(R.layout.item_budget_category, binding.budgetCategoryList, false)
                val nameTextView: TextView = categoryView.findViewById(R.id.category_name)
                val spentTextView: TextView = categoryView.findViewById(R.id.category_spent)
                val totalTextView: TextView = categoryView.findViewById(R.id.category_total)
                // Add ProgressBar if you have one in item_budget_category.xml
                // val progressBar: ProgressBar = categoryView.findViewById(R.id.category_progress)

                nameTextView.text = category.name
                spentTextView.text = String.format("LKR %.2f", category.spent)
                totalTextView.text = String.format("of LKR %.2f", category.total)

                // Optional: Set progress bar
                // if (category.total > 0) {
                //     val progress = (category.spent / category.total * 100).toInt()
                //     progressBar.progress = progress
                // } else {
                //     progressBar.progress = 0
                // }

                binding.budgetCategoryList.addView(categoryView)
            }
        } else {
            Log.d(TAG, "No budget categories to display")
             // Optionally display a message if no categories exist
             val noCategoriesTextView = TextView(context)
             noCategoriesTextView.text = "No budget categories set up."
             noCategoriesTextView.layoutParams = LinearLayout.LayoutParams(
                 LinearLayout.LayoutParams.WRAP_CONTENT,
                 LinearLayout.LayoutParams.WRAP_CONTENT
             )
             binding.budgetCategoryList.addView(noCategoriesTextView)
        }

        // --- Update Expense Chart (Current Month) ---
        binding.expenseChart.text = String.format("Spent\nLKR %.2f", budgetSpent)
        val expenseBreakdown = categories.joinToString("\n") { category ->
            String.format("%s: LKR %.2f / LKR %.2f", category.name, category.spent, category.total)
        }
        binding.expenseBreakdown.text = expenseBreakdown
        Log.d(TAG, "Expense Breakdown: $expenseBreakdown")

        // Calculate and display expense percentage
        val percentage = if (budgetAllocated > 0) {
            (budgetSpent / budgetAllocated * 100).toInt()
        } else {
            0 // Avoid division by zero if budget is 0
        }
        binding.expensePercentage.text = "$percentage%"
        Log.d(TAG, "Expense Percentage: $percentage%")


        // --- Update Last Records ---
        val records = dataManager.getLastRecords()
        binding.record1.visibility = View.GONE // Hide all initially
        binding.record2.visibility = View.GONE
        binding.record3.visibility = View.GONE

        records.getOrNull(0)?.let { rec ->
            binding.record1.visibility = View.VISIBLE
            binding.record1Date.text = rec.date
            binding.record1Category.text = rec.category // Display category
            binding.record1Title.text = rec.title // Display title
            binding.record1Amount.text = String.format("LKR %.2f", rec.amount)
            binding.record1Amount.setTextColor( ContextCompat.getColor(requireContext(),
                 if (rec.amount >= 0) R.color.colorAccent else R.color.colorError) // Use colorAccent for income
            )
        }
        records.getOrNull(1)?.let { rec ->
            binding.record2.visibility = View.VISIBLE
            binding.record2Date.text = rec.date
            binding.record2Category.text = rec.category
            binding.record2Title.text = rec.title
            binding.record2Amount.text = String.format("LKR %.2f", rec.amount)
             binding.record2Amount.setTextColor( ContextCompat.getColor(requireContext(),
                 if (rec.amount >= 0) R.color.colorAccent else R.color.colorError) // Use colorAccent for income
            )
        }
        records.getOrNull(2)?.let { rec ->
            binding.record3.visibility = View.VISIBLE
            binding.record3Date.text = rec.date
            binding.record3Category.text = rec.category
            binding.record3Title.text = rec.title
            binding.record3Amount.text = String.format("LKR %.2f", rec.amount)
             binding.record3Amount.setTextColor( ContextCompat.getColor(requireContext(),
                 if (rec.amount >= 0) R.color.colorAccent else R.color.colorError) // Use colorAccent for income
            )
        }
         Log.d(TAG, "Displayed ${records.size} last records.")

        // --- Update Account List Display ---
        // This shows all account balances, which might be confusing with the selected balance at top
        // Consider simplifying this section or making it clearer
        val accounts = dataManager.getAccounts()
        accounts.getOrNull(0)?.let {
             binding.accountTotalName.text = it.name
             binding.accountTotalBalance.text = String.format("LKR %.2f", it.balance)
        }
        accounts.getOrNull(1)?.let {
             binding.accountCreditCardName.text = it.name
             binding.accountCreditCardBalance.text = String.format("LKR %.2f", it.balance)
        }
        accounts.getOrNull(2)?.let {
             binding.accountDebitCardName.text = it.name
             binding.accountDebitCardBalance.text = String.format("LKR %.2f", it.balance)
        }
         accounts.getOrNull(3)?.let {
             binding.accountCashName.text = it.name
             binding.accountCashBalance.text = String.format("LKR %.2f", it.balance)
        }
         Log.d(TAG, "Updated account list display.")
         Log.d(TAG, "UI Update finished")
    }

    private fun showAccountSelectionDialog() {
         Log.d(TAG, "Showing account selection dialog")
        val accounts = dataManager.getAccounts()
        val accountNames = accounts.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Account")
            .setItems(accountNames) { _, which ->
                 val selected = accounts[which]
                 Log.d(TAG, "Account selected: ${selected.name}")
                dataManager.setSelectedAccount(selected)
                updateUI() // Update UI after selection
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Helper to select account and update UI (Alternative to dialog)
    // private fun selectAccountAndUpdate(accountName: String) {
    //     val account = dataManager.getAccounts().find { it.name == accountName }
    //     if (account != null) {
    //         dataManager.setSelectedAccount(account)
    //         updateUI()
    //     }
    // }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null // Prevent memory leaks
    }
}