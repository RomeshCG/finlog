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
    private var recordToEdit: Record? = null

    companion object {
        private const val ARG_RECORD = "record"

        fun newInstance(record: Record? = null): AddRecordFragment {
            val fragment = AddRecordFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORD, record)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordToEdit = arguments?.getSerializable(ARG_RECORD) as? Record
    }

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
        binding.transferCategory.adapter = categoryAdapter

        // Date Pickers
        binding.expenseDate.setOnClickListener { showDatePicker(binding.expenseDate) }
        binding.incomeDate.setOnClickListener { showDatePicker(binding.incomeDate) }
        binding.transferDate.setOnClickListener { showDatePicker(binding.transferDate) }

        // Submit buttons
        binding.submitExpense.setOnClickListener { addOrUpdateExpense() }
        binding.submitIncome.setOnClickListener { addOrUpdateIncome() }
        binding.submitTransfer.setOnClickListener { addOrUpdateTransfer() }

        // Pre-fill form if editing
        recordToEdit?.let { record ->
            when (record.type) {
                "Expense" -> {
                    binding.expenseSection.visibility = View.VISIBLE
                    binding.incomeSection.visibility = View.GONE
                    binding.transferSection.visibility = View.GONE
                    binding.expenseAmount.setText(record.amount.toString())
                    binding.expenseAccount.setSelection(accounts.indexOf(record.account))
                    binding.expenseCategory.setSelection(categories.indexOf(record.category))
                    binding.expenseDate.setText(record.date)
                }
                "Income" -> {
                    binding.expenseSection.visibility = View.GONE
                    binding.incomeSection.visibility = View.VISIBLE
                    binding.transferSection.visibility = View.GONE
                    binding.incomeAmount.setText(record.amount.toString())
                    binding.incomeAccount.setSelection(accounts.indexOf(record.account))
                    binding.incomeCategory.setSelection(categories.indexOf(record.category))
                    binding.incomeDate.setText(record.date)
                }
                "Transfer" -> {
                    binding.expenseSection.visibility = View.GONE
                    binding.incomeSection.visibility = View.GONE
                    binding.transferSection.visibility = View.VISIBLE
                    binding.transferAmount.setText(record.amount.toString())
                    binding.transferFromAccount.setSelection(accounts.indexOf(record.account))
                    binding.transferToAccount.setSelection(accounts.indexOf(record.toAccount))
                    binding.transferCategory.setSelection(categories.indexOf(record.category))
                    binding.transferDate.setText(record.date)
                }
            }
        }
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

    private fun addOrUpdateExpense() {
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

        val record = Record(date, category, -amount, account, "Expense")
        if (recordToEdit != null) {
            updateRecord(record)
        } else {
            dataManager.addRecord(record)
        }
        Toast.makeText(context, if (recordToEdit != null) "Expense updated" else "Expense added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun addOrUpdateIncome() {
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

        val record = Record(date, category, amount, account, "Income")
        if (recordToEdit != null) {
            updateRecord(record)
        } else {
            dataManager.addRecord(record)
        }
        Toast.makeText(context, if (recordToEdit != null) "Income updated" else "Income added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun addOrUpdateTransfer() {
        val amountText = binding.transferAmount.text.toString().replace("$", "").trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val fromAccount = binding.transferFromAccount.selectedItem.toString()
        val toAccount = binding.transferToAccount.selectedItem.toString()
        val category = binding.transferCategory.selectedItem.toString()
        val date = binding.transferDate.text.toString()

        if (date.isEmpty()) {
            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (fromAccount == toAccount) {
            Toast.makeText(context, "From and To accounts must be different", Toast.LENGTH_SHORT).show()
            return
        }

        val record = Record(date, category, amount, fromAccount, "Transfer", toAccount)
        if (recordToEdit != null) {
            updateRecord(record)
        } else {
            dataManager.addRecord(record)
        }
        Toast.makeText(context, if (recordToEdit != null) "Transfer updated" else "Transfer added", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun updateRecord(newRecord: Record) {
        val records = dataManager.getRecords().toMutableList()
        val index = records.indexOf(recordToEdit)
        if (index != -1) {
            records[index] = newRecord
            dataManager.saveRecords(records)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}