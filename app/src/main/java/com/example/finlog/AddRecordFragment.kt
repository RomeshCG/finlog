package com.example.finlog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finlog.databinding.FragmentAddRecordBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddRecordFragment : Fragment() {

    private var _binding: FragmentAddRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize DataManager
        dataManager = DataManager(requireContext())

        // Toggle sections
        binding.btnExpense.setOnClickListener {
            binding.expenseSection.visibility = View.VISIBLE
            binding.incomeSection.visibility = View.GONE
            binding.transferSection.visibility = View.GONE
        }
        binding.btnIncome.setOnClickListener {
            binding.expenseSection.visibility = View.GONE
            binding.incomeSection.visibility = View.VISIBLE
            binding.transferSection.visibility = View.GONE
        }
        binding.btnTransfer.setOnClickListener {
            binding.expenseSection.visibility = View.GONE
            binding.incomeSection.visibility = View.GONE
            binding.transferSection.visibility = View.VISIBLE
        }

        // Populate Spinners
        val accounts = dataManager.getAccounts().map { it.name }
        val categories = listOf("Entertainment", "Food", "Fuel", "Other")

        val accountAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, accounts)
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.expenseAccount.adapter = accountAdapter
        binding.expenseCategory.adapter = categoryAdapter
        binding.incomeAccount.adapter = accountAdapter
        binding.incomeCategory.adapter = categoryAdapter
        binding.transferFromAccount.adapter = accountAdapter
        binding.transferToAccount.adapter = accountAdapter

        // Date Pickers
        binding.expenseDate.setOnClickListener { showDatePicker(binding.expenseDate) }
        binding.incomeDate.setOnClickListener { showDatePicker(binding.incomeDate) }
        binding.transferDate.setOnClickListener { showDatePicker(binding.transferDate) }

        // Submit buttons
        binding.submitExpense.setOnClickListener { addExpense() }
        binding.submitIncome.setOnClickListener { addIncome() }
        binding.submitTransfer.setOnClickListener { addTransfer() }
    }

    private fun showDatePicker(editText: EditText) {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                editText.setText(format.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun addExpense() {
        val amountText = binding.expenseAmount.text.toString().replace("$", "").trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val account = binding.expenseAccount.selectedItem.toString()
        val category = binding.expenseCategory.selectedItem.toString()
        val date = binding.expenseDate.text.toString()

        if (date.isEmpty()) {
            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        dataManager.addRecord(Record(date, category, -amount, account, "Expense"))
        Toast.makeText(context, "Expense added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun addIncome() {
        val amountText = binding.incomeAmount.text.toString().replace("$", "").trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val account = binding.incomeAccount.selectedItem.toString()
        val category = binding.incomeCategory.selectedItem.toString()
        val date = binding.incomeDate.text.toString()

        if (date.isEmpty()) {
            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        dataManager.addRecord(Record(date, category, amount, account, "Income"))
        Toast.makeText(context, "Income added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun addTransfer() {
        val amountText = binding.transferAmount.text.toString().replace("$", "").trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val fromAccount = binding.transferFromAccount.selectedItem.toString()
        val toAccount = binding.transferToAccount.selectedItem.toString()
        val date = binding.transferDate.text.toString()

        if (date.isEmpty()) {
            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (fromAccount == toAccount) {
            Toast.makeText(context, "From and To accounts must be different", Toast.LENGTH_SHORT).show()
            return
        }

        // Add a single transfer record with both from and to accounts
        dataManager.addRecord(Record(date, "Transfer", amount, fromAccount, "Transfer", toAccount))
        Toast.makeText(context, "Transfer added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}