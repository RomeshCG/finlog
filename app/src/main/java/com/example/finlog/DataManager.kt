package com.example.finlog

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
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

// Data class for a card
data class Card(
    val type: String,
    val number: String,
    val holderName: String,
    val cvv: String,
    val balance: Double
) : Serializable

// Data class for backup
data class BackupData(
    val records: List<Record>,
    val cards: List<Card>
)

class DataManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("FinLogPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val appContext = context

    // List of accounts with initial balances (excluding card balances for now)
    private val baseAccounts = listOf(
        Account("Total", 0.00), // Will be updated dynamically
        Account("Credit Card", 0.00),
        Account("Debit Card", 0.00),
        Account("Cash", 3000.00)
    )

    // Budget categories with initial spent and total values
    private val budgetCategories = listOf(
        BudgetCategory("Entertainment", 3430.00, 5000.00),
        BudgetCategory("Food", 430.00, 1000.00),
        BudgetCategory("Fuel", 150.00, 500.00)
    )

    // Initial cards
    private val initialCards = listOf(
        Card("Credit Card", "1234567890121234", "JOHN DOE", "123", 15000.00),
        Card("Debit Card", "9876543210981234", "JOHN DOE", "456", 8000.00)
    )

    // Keys for SharedPreferences
    companion object {
        private const val KEY_SELECTED_ACCOUNT = "selected_account"
        private const val KEY_RECORDS = "records"
        private const val KEY_CARDS = "cards"
        private const val BACKUP_FILE_NAME = "finlog_backup.json"
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

        // Add initial cards if none exist
        val cards = getCards()
        if (cards.isEmpty()) {
            saveCards(initialCards)
        }
    }

    // Get all accounts with updated balances
    fun getAccounts(): List<Account> {
        val cards = getCards()
        val creditCardBalance = cards.filter { it.type == "Credit Card" }.sumOf { it.balance }
        val debitCardBalance = cards.filter { it.type == "Debit Card" }.sumOf { it.balance }
        val cashBalance = baseAccounts.find { it.name == "Cash" }?.balance ?: 0.00
        val totalBalance = creditCardBalance + debitCardBalance + cashBalance

        return listOf(
            Account("Total", totalBalance),
            Account("Credit Card", creditCardBalance),
            Account("Debit Card", debitCardBalance),
            Account("Cash", cashBalance)
        )
    }

    // Get the currently selected account from SharedPreferences
    fun getSelectedAccount(): Account {
        val selectedAccountName = sharedPreferences.getString(KEY_SELECTED_ACCOUNT, "Total")
        return getAccounts().find { it.name == selectedAccountName } ?: getAccounts()[0]
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

    // Get all cards from SharedPreferences
    fun getCards(): List<Card> {
        val cardsJson = sharedPreferences.getString(KEY_CARDS, null)
        return if (cardsJson != null) {
            val type = object : TypeToken<List<Card>>() {}.type
            gson.fromJson(cardsJson, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Add a card and save to SharedPreferences
    fun addCard(card: Card) {
        val cards = getCards().toMutableList()
        cards.add(card)
        saveCards(cards)
    }

    // Save cards
    fun saveCards(cards: List<Card>) {
        val cardsJson = gson.toJson(cards)
        sharedPreferences.edit().putString(KEY_CARDS, cardsJson).apply()
    }

    // Create a backup of all data
    fun createBackup() {
        val backupData = BackupData(
            records = getRecords(),
            cards = getCards()
        )
        val backupJson = gson.toJson(backupData)
        val backupFile = File(appContext.filesDir, BACKUP_FILE_NAME)
        backupFile.writeText(backupJson)
    }

    // Restore data from a backup
    fun restoreBackup() {
        val backupFile = File(appContext.filesDir, BACKUP_FILE_NAME)
        if (!backupFile.exists()) {
            throw Exception("No backup file found")
        }
        val backupJson = backupFile.readText()
        val type = object : TypeToken<BackupData>() {}.type
        val backupData: BackupData = gson.fromJson(backupJson, type)
        saveRecords(backupData.records)
        saveCards(backupData.cards)
    }
}