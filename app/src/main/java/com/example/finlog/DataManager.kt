package com.example.finlog

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class for an account
data class Account(val name: String, val balance: Double)

// Data class for a budget category
data class BudgetCategory(val name: String, val spent: Double, val total: Double)

// Data class for a financial record
data class Record(
    val date: String,
    val category: String,
    val amount: Double,
    val account: String,
    val type: String,
    val toAccount: String? = null
) : Serializable

class DataManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("FinLogPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // List of accounts with initial balances
    private val accounts = listOf(
        Account("Total", 26000.00),
        Account("Credit Card", 15000.00),
        Account("Debit Card", 8000.00),
        Account("Cash", 3000.00)
    )

    // Budget categories with initial spent and total values
    private val budgetCategories = listOf(
        BudgetCategory("Entertainment", 3430.00, 5000.00),
        BudgetCategory("Food", 430.00, 1000.00),
        BudgetCategory("Fuel", 150.00, 500.00)
    )

    // Keys for SharedPreferences
    companion object {
        private const val KEY_SELECTED_ACCOUNT = "selected_account"
        private const val KEY_RECORDS = "records"
    }

    init {
        // Add test records if none exist
        val records = getRecords()
        if (records.isEmpty()) {
            val testRecords = listOf(
                Record("20 Apr 2025", "Food", -6.21, "Cash", "Expense"),
                Record("19 Apr 2025", "Salary", 3400.90, "Debit Card", "Income"),
                Record("18 Apr 2025", "Transfer", 60.21, "Credit Card", "Transfer", "Cash")
            )
            saveRecords(testRecords)
        }
    }

    // Get all accounts
    fun getAccounts(): List<Account> = accounts

    // Get the currently selected account from SharedPreferences
    fun getSelectedAccount(): Account {
        val selectedAccountName = sharedPreferences.getString(KEY_SELECTED_ACCOUNT, accounts[0].name)
        return accounts.find { it.name == selectedAccountName } ?: accounts[0]
    }

    // Set the selected account and save to SharedPreferences
    fun setSelectedAccount(account: Account) {
        sharedPreferences.edit().putString(KEY_SELECTED_ACCOUNT, account.name).apply()
    }

    // Get budget categories
    fun getBudgetCategories(): List<BudgetCategory> = budgetCategories

    // Get total budget spent
    fun getTotalBudgetSpent(): Double = budgetCategories.sumOf { it.spent }

    // Get total budget allocated
    fun getTotalBudgetAllocated(): Double = budgetCategories.sumOf { it.total }

    // Get budget left
    fun getBudgetLeft(): Double = getTotalBudgetAllocated() - getTotalBudgetSpent()

    // Get all records from SharedPreferences
    fun getRecords(): List<Record> {
        val recordsJson = sharedPreferences.getString(KEY_RECORDS, null)
        return if (recordsJson != null) {
            val type = object : TypeToken<List<Record>>() {}.type
            gson.fromJson(recordsJson, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Add a record and save to SharedPreferences
    fun addRecord(record: Record) {
        val records = getRecords().toMutableList()
        records.add(record)
        saveRecords(records)
    }

    // Save records (for deletion or updates)
    fun saveRecords(records: List<Record>) {
        val recordsJson = gson.toJson(records)
        sharedPreferences.edit().putString(KEY_RECORDS, recordsJson).apply()
    }

    // Get last records (limit to 3 for display)
    fun getLastRecords(): List<Record> = getRecords().sortedByDescending { it.date }.take(3)

    // Get current date dynamically
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}