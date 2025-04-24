package com.example.finlog

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
    val title: String,
    val toAccount: String? = null
) : Serializable

// Data class for a card
data class Card(
    val type: String,
    val number: String,
    val holderName: String,
    val cvv: String,
    var balance: Double
) : Serializable

// Data class for backup
data class BackupData(
    val timestamp: String,
    val records: List<Record>,
    val cards: List<Card>
)

// Data class for monthly budget
data class MonthlyBudget(
    val total: Double,
    val categories: Map<String, Double> // Map<CategoryName, BudgetAmount>
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
        Account("Cash", 0.00) // Initial cash balance will be calculated from records
    )

    // Budget categories with initial spent and total values
    private val budgetCategories = listOf(
        BudgetCategory("Entertainment", 3430.00, 5000.00),
        BudgetCategory("Food", 430.00, 1000.00),
        BudgetCategory("Fuel", 150.00, 500.00)
    )

    // Initial cards (Balances will be updated from records)
    private val initialCards = listOf(
        Card("Credit Card", "1234567890121234", "JOHN DOE", "123", 0.0),
        Card("Debit Card", "9876543210981234", "JOHN DOE", "456", 0.0)
    )

    // Keys for SharedPreferences and backup file prefix
    companion object {
        private const val KEY_SELECTED_ACCOUNT = "selected_account"
        private const val KEY_RECORDS = "records"
        private const val KEY_CARDS = "cards"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_USER_CATEGORIES = "user_categories" // Key for user categories
        private const val BACKUP_FILE_PREFIX = "finlog_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
        private const val TAG = "DataManager"
        // Default Categories if none are saved
        private val DEFAULT_CATEGORIES = listOf("Entertainment", "Food", "Fuel", "Salary", "Transfer", "Other")
    }

    init {
        // Add test records if none exist
        val records = getRecords()
        if (records.isEmpty()) {
            val testRecords = listOf(
                Record("20 Apr 2025", "Food", -6.21, "Cash", "Expense", "Lunch at Cafe"),
                Record("19 Apr 2025", "Salary", 3400.90, "Debit Card", "Income", "Monthly Salary"),
                Record("18 Apr 2025", "Transfer", -60.21, "Credit Card", "Transfer", "Cash Withdrawal", "Cash"), // Negative from Credit Card
                Record("18 Apr 2025", "Transfer", 60.21, "Cash", "Transfer", "Cash Withdrawal", "Credit Card")     // Positive to Cash
            )
            saveRecords(testRecords) // This will also trigger updateCardBalances
        } else {
            updateCardBalances() // Ensure balances are correct on startup
        }

        // Add initial cards if none exist (balances will be calculated)
        val cards = getCards()
        if (cards.isEmpty()) {
            saveCards(initialCards)
            updateCardBalances() // Calculate initial balances
        }

        // Ensure default categories exist if none are saved
        if (getUserCategories().isEmpty()) {
            saveUserCategories(DEFAULT_CATEGORIES)
        }
        // Ensure default budget includes default categories
        val currentBudget = getMonthlyBudget()
        val currentCategories = getUserCategories()
        val budgetNeedsUpdate = currentCategories.any { !currentBudget.categories.containsKey(it) }
        if (budgetNeedsUpdate) {
            val updatedCategories = currentBudget.categories.toMutableMap()
            currentCategories.forEach { category ->
                if (!updatedCategories.containsKey(category)) {
                    updatedCategories[category] = 0.0 // Add new categories with 0 budget initially
                }
            }
            saveMonthlyBudget(currentBudget.copy(categories = updatedCategories))
            Log.d(TAG, "Updated monthly budget to include new default categories.")
        }
    }

    // Get monthly budget from SharedPreferences or return default
    fun getMonthlyBudget(): MonthlyBudget {
        val budgetJson = sharedPreferences.getString(KEY_MONTHLY_BUDGET, null)
        return if (budgetJson != null) {
            try {
                gson.fromJson(budgetJson, MonthlyBudget::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing monthly budget JSON: $budgetJson", e)
                getDefaultMonthlyBudget() // Return default if parsing fails
            }
        } else {
            getDefaultMonthlyBudget() // Return default if not set
        }
    }

    // Provides a default monthly budget
    private fun getDefaultMonthlyBudget(): MonthlyBudget {
        val categories = getUserCategories()
        val defaultCategoryBudget = categories.associateWith { 0.0 } // Default all to 0
        return MonthlyBudget(
            total = 1000.00, // Default total budget
            categories = defaultCategoryBudget
        )
    }

    // Save monthly budget to SharedPreferences
    fun saveMonthlyBudget(budget: MonthlyBudget) {
        val budgetJson = gson.toJson(budget)
        sharedPreferences.edit().putString(KEY_MONTHLY_BUDGET, budgetJson).apply()
        Log.d(TAG, "Saved monthly budget: $budgetJson")
    }

    // Get category-wise spending for the current month (only expenses)
    fun getCategorySpendingCurrentMonth(): Map<String, Double> {
        val currentMonthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        Log.d(TAG, "Calculating spending for month: $currentMonthYear")

        val records = getRecords()
            .filter { record ->
                try {
                    val recordDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(record.date)
                    val recordMonthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(recordDate)
                    recordMonthYear == currentMonthYear && record.type == "Expense" // Only count expenses towards budget
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing record date: ${record.date}", e)
                    false // Exclude records with invalid dates
                }
            }

        val spending = records.groupBy { it.category }
            .mapValues { (_, categoryRecords) ->
                // Summing absolute values of negative amounts (expenses)
                categoryRecords.sumOf { if (it.amount < 0) -it.amount else 0.0 }
            }
        Log.d(TAG, "Category spending for $currentMonthYear: $spending")
        return spending
    }

    // Get total spending (expenses only) for the current month
    fun getTotalSpendingCurrentMonth(): Double {
        val categorySpending = getCategorySpendingCurrentMonth()
        val totalSpending = categorySpending.values.sum()
        Log.d(TAG, "Total spending for current month: $totalSpending")
        return totalSpending
    }

    // Get budget categories with updated spent values for the current month
    fun getBudgetCategoriesCurrentMonth(): List<BudgetCategory> {
        val monthlyBudget = getMonthlyBudget() // Loads saved budget map
        val categorySpending = getCategorySpendingCurrentMonth()
        val userCategories = getUserCategories() // Get the definitive list of categories

        // Ensure budget map contains all user categories
        val budgetMap = monthlyBudget.categories.toMutableMap()
        userCategories.forEach { cat ->
            if (!budgetMap.containsKey(cat)) {
                budgetMap[cat] = 0.0 // Add missing categories with 0 budget
            }
        }

        val categories = userCategories.map { name ->
            val total = budgetMap[name] ?: 0.0 // Get budget amount from potentially updated map
            val spent = categorySpending.getOrDefault(name, 0.0)
            BudgetCategory(
                name = name,
                spent = spent,
                total = total
            )
        }

        // No need to handle spending categories not in budget map anymore, as we ensure all user categories are in budgetMap

        Log.d(TAG, "Budget categories for current month (based on user categories): $categories")
        return categories.sortedBy { it.name } // Sort alphabetically
    }

    // Get total budget spent for the current month (sum of expenses)
    fun getTotalBudgetSpentCurrentMonth(): Double = getTotalSpendingCurrentMonth()

    // Get total budget allocated for the current month
    fun getTotalBudgetAllocatedCurrentMonth(): Double = getMonthlyBudget().total

    // Get budget left for the current month
    fun getBudgetLeftCurrentMonth(): Double {
         val left = getTotalBudgetAllocatedCurrentMonth() - getTotalBudgetSpentCurrentMonth()
         Log.d(TAG, "Budget left for current month: $left")
         return left
    }

    // Update card balances based on records
    private fun updateCardBalances() {
        val records = getRecords()
        val cards = getCards().toMutableList()
        
        // Create a map to hold calculated balances for each card type
        val calculatedBalances = mutableMapOf<String, Double>()

        // Initialize balances from initial card data if available (though records should override)
        initialCards.forEach { calculatedBalances[it.type] = it.balance }

        // Aggregate amounts from records for each card account
        records.forEach { record ->
            // Update balance for the primary account involved in the record
             if (calculatedBalances.containsKey(record.account)) {
                 calculatedBalances[record.account] = calculatedBalances.getOrDefault(record.account, 0.0) + record.amount
             }
             // Note: Transfers are handled by having two records, one positive and one negative.
        }

        // Update card objects with calculated balances
        val updatedCards = cards.map { card ->
            card.copy(balance = calculatedBalances.getOrDefault(card.type, card.balance)) // Use calculated balance or keep existing if not found
        }

        Log.d(TAG, "Updating card balances: $updatedCards")
        saveCards(updatedCards)
    }

    // Get all accounts with updated balances
    fun getAccounts(): List<Account> {
        val cards = getCards()
        val creditCardBalance = cards.filter { it.type == "Credit Card" }.sumOf { it.balance }
        val debitCardBalance = cards.filter { it.type == "Debit Card" }.sumOf { it.balance }
        val cashBalance = calculateCashBalance()
        val totalBalance = creditCardBalance + debitCardBalance + cashBalance

        return listOf(
            Account("Total", totalBalance),
            Account("Credit Card", creditCardBalance),
            Account("Debit Card", debitCardBalance),
            Account("Cash", cashBalance)
        )
    }

    // Calculate cash balance based on records
    private fun calculateCashBalance(): Double {
        val records = getRecords()
        val cashBalance = records
            .filter { it.account == "Cash" }
            .sumOf { it.amount } // Sums positive (income/transfer in) and negative (expense/transfer out)
        Log.d(TAG, "Calculated Cash Balance from records: $cashBalance")
        return cashBalance
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
        updateCardBalances() // Update card balances after adding record
    }

    // Save records (for deletion or updates)
    fun saveRecords(records: List<Record>) {
        val recordsJson = gson.toJson(records)
        sharedPreferences.edit().putString(KEY_RECORDS, recordsJson).apply()
        updateCardBalances() // Update card balances after saving records
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

    // Delete a card by card number and save to SharedPreferences
    fun deleteCard(cardNumber: String) {
        val cards = getCards().toMutableList()
        val cardRemoved = cards.removeAll { it.number == cardNumber }
        if (cardRemoved) {
            saveCards(cards)
            Log.d(TAG, "Card deleted: $cardNumber")
        } else {
            Log.e(TAG, "Card not found: $cardNumber")
        }
    }

    // Save cards
    fun saveCards(cards: List<Card>) {
        val cardsJson = gson.toJson(cards)
        sharedPreferences.edit().putString(KEY_CARDS, cardsJson).apply()
    }

    // Create a backup of all data with a timestamped filename
    fun createBackup(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupData = BackupData(
            timestamp = timestamp,
            records = getRecords(),
            cards = getCards()
        )
        val backupJson = gson.toJson(backupData)
        val backupFileName = "${BACKUP_FILE_PREFIX}${timestamp}${BACKUP_FILE_EXTENSION}"
        val backupFile = File(appContext.filesDir, backupFileName)
        backupFile.writeText(backupJson)
        Log.d(TAG, "Created backup: $backupFileName at ${backupFile.absolutePath}")
        return backupFileName
    }

    // Restore data from a specific backup file
    fun restoreBackup(backupFileName: String) {
        val backupFile = File(appContext.filesDir, backupFileName)
        Log.d(TAG, "Attempting to restore from: ${backupFile.absolutePath}")
        if (!backupFile.exists()) {
            Log.e(TAG, "Backup file does not exist: $backupFileName")
            throw Exception("Backup file not found: $backupFileName")
        }
        val backupJson = backupFile.readText()
        Log.d(TAG, "Backup file contents: $backupJson")
        val type = object : TypeToken<BackupData>() {}.type
        val backupData: BackupData = gson.fromJson(backupJson, type)
        saveRecords(backupData.records)
        saveCards(backupData.cards)
        Log.d(TAG, "Restored backup: $backupFileName")
    }

    // Get a list of all backup files
    fun getBackupFiles(): List<String> {
        val filesDir = appContext.filesDir
        val backupFiles = filesDir.listFiles { _, name ->
            name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION)
        }
        val fileList = backupFiles?.map { it.name }?.sortedByDescending { it } ?: emptyList()
        Log.d(TAG, "Found backup files: $fileList")
        return fileList
    }

    // Get the timestamp of a specific backup and format it
    fun getBackupTimestamp(backupFileName: String): String {
        val backupFile = File(appContext.filesDir, backupFileName)
        if (!backupFile.exists()) {
            Log.e(TAG, "Cannot get timestamp, backup file not found: $backupFileName")
            return "Unknown"
        }
        val backupJson = backupFile.readText()
        val type = object : TypeToken<BackupData>() {}.type
        val backupData: BackupData = gson.fromJson(backupJson, type)
        // Parse the timestamp (yyyyMMdd_HHmmss) and format it as "dd MMM yyyy, HH:mm:ss"
        return try {
            val inputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(backupData.timestamp)
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse timestamp ${backupData.timestamp}: ${e.message}")
            "Unknown"
        }
    }

    // Delete a specific backup file
    fun deleteBackup(backupFileName: String): Boolean {
        val backupFile = File(appContext.filesDir, backupFileName)
        return if (backupFile.exists()) {
            val deleted = backupFile.delete()
            Log.d(TAG, "Deleted backup $backupFileName: $deleted")
            deleted
        } else {
            Log.e(TAG, "Cannot delete, backup file not found: $backupFileName")
            false
        }
    }

    // Get total balance
    fun getTotalBalance(): Double {
        return getAccounts().find { it.name == "Total" }?.balance ?: 0.0
    }

    // Get account balance by name
    fun getAccountBalance(accountName: String): Double {
        return getAccounts().find { it.name == accountName }?.balance ?: 0.0
    }

    // --- User Category Management ---
    fun getUserCategories(): List<String> {
        val json = sharedPreferences.getString(KEY_USER_CATEGORIES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(json, type) ?: DEFAULT_CATEGORIES
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user categories JSON: $json", e)
                DEFAULT_CATEGORIES
            }
        } else {
            DEFAULT_CATEGORIES
        }
    }

    fun saveUserCategories(categories: List<String>) {
        val distinctSortedCategories = categories.distinct().sorted()
        val json = gson.toJson(distinctSortedCategories)
        sharedPreferences.edit().putString(KEY_USER_CATEGORIES, json).apply()
        Log.d(TAG, "Saved user categories: $distinctSortedCategories")

        // Also update the monthly budget to include any new categories with 0 budget
        val currentBudget = getMonthlyBudget()
        val budgetCategories = currentBudget.categories.toMutableMap()
        var budgetChanged = false
        distinctSortedCategories.forEach {
            if (!budgetCategories.containsKey(it)) {
                budgetCategories[it] = 0.0 // Default new categories to 0 budget
                budgetChanged = true
            }
        }
        // Remove categories from budget if they are no longer in the user list
        val categoriesToRemove = budgetCategories.keys.filter { !distinctSortedCategories.contains(it) }
        if (categoriesToRemove.isNotEmpty()) {
            categoriesToRemove.forEach { budgetCategories.remove(it) }
            budgetChanged = true
        }

        if (budgetChanged) {
            saveMonthlyBudget(currentBudget.copy(categories = budgetCategories))
            Log.d(TAG, "Updated monthly budget categories after saving user categories.")
        }
    }

    fun addCategory(categoryName: String): Boolean {
        val trimmedName = categoryName.trim()
        if (trimmedName.isEmpty()) {
            Log.w(TAG, "Attempted to add empty category name.")
            return false
        }
        val currentCategories = getUserCategories().toMutableList()
        if (!currentCategories.any { it.equals(trimmedName, ignoreCase = true) }) {
            currentCategories.add(trimmedName)
            saveUserCategories(currentCategories)
            return true
        } else {
            Log.w(TAG, "Category '$trimmedName' already exists.")
            return false // Indicate category already exists
        }
    }

    // Note: Removing categories currently doesn't remove associated records.
    // Consider adding logic later if records should be re-categorized or handled differently.
    fun removeCategory(categoryName: String): Boolean {
        val currentCategories = getUserCategories().toMutableList()
        val removed = currentCategories.removeIf { it.equals(categoryName, ignoreCase = true) }
        if (removed) {
            saveUserCategories(currentCategories)
        } else {
            Log.w(TAG, "Category '$categoryName' not found for removal.")
        }
        return removed
    }
}