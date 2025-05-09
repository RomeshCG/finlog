package com.example.finlog

import android.content.Context
import android.content.Intent // Needed for PendingIntent
import android.content.SharedPreferences
import android.content.pm.PackageManager // Needed for permission check
import android.util.Log
import androidx.core.app.ActivityCompat // Needed for permission check
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent // Needed for notification tap action
import android.Manifest // Needed for permission constant
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
        private const val KEY_NOTIFIED_TOTAL_PREFIX = "notified_total_"
        private const val KEY_NOTIFIED_CATEGORY_PREFIX = "notified_category_"
        private const val BUDGET_NOTIFICATION_ID = 1 // Unique ID for budget notifications
    }

    init {
        // We might still want to ensure balances are calculated on first launch if any data *did* exist
        // but with no defaults, this likely isn't strictly necessary unless restoring a backup.
        // Consider calling updateCardBalances() here if needed for edge cases or after restore.
        Log.d(TAG, "DataManager initialized - No default data added.")
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
        Log.d(TAG, "Providing default empty monthly budget.")
        return MonthlyBudget(
            total = 0.0, // Start with zero budget
            categories = emptyMap() // Start with no category budgets
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
    fun getBudgetCategories(): List<BudgetCategory> = emptyList()

    // Get total budget spent
    fun getTotalBudgetSpent(): Double = 0.0

    // Get total budget allocated
    fun getTotalBudgetAllocated(): Double = 0.0

    // Get budget left
    fun getBudgetLeft(): Double = 0.0

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

    // Add a record and save to SharedPreferences, then update balances and check budget
    fun addRecord(record: Record) {
        val records = getRecords().toMutableList()
        records.add(record)
        saveRecords(records) // This calls updateCardBalances internally
        Log.d(TAG, "Added record: $record")

        // Check budget only if it was an expense
        if (record.type == "Expense") {
            checkBudgetAndNotify(record.category, record.amount)
        }
    }

    // Save records (for deletion or updates), then update balances
    fun saveRecords(records: List<Record>) {
        val recordsJson = gson.toJson(records)
        sharedPreferences.edit().putString(KEY_RECORDS, recordsJson).apply()
        updateCardBalances() // Update card balances after saving records
        Log.d(TAG, "Saved ${records.size} records.")
        // Note: We might want budget checks after deletion/update too, but complexities arise.
        // For now, checks only happen on adding expenses via addRecord.
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
                // Return empty list if parsing fails or no data
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user categories JSON: $json", e)
                emptyList()
            }
        } else {
            emptyList() // Return empty list if key doesn't exist
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

    // --- Budget Notification Logic ---

    private fun checkBudgetAndNotify(expenseCategory: String, expenseAmount: Double) {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthlyBudget = getMonthlyBudget()
        val totalSpent = getTotalSpendingCurrentMonth() // Gets updated spending *after* the record was saved

        Log.d(TAG, "Checking budget after expense. Total Spent: $totalSpent, Budget Total: ${monthlyBudget.total}")

        // Check Total Budget
        val totalBudgetKey = "${KEY_NOTIFIED_TOTAL_PREFIX}${currentMonthYear}"
        if (totalSpent > monthlyBudget.total && !sharedPreferences.getBoolean(totalBudgetKey, false)) {
            Log.i(TAG, "Total budget exceeded! Sending notification.")
            sendBudgetNotification("Total Monthly Budget Exceeded",
                "You\'ve spent $${String.format("%.2f", totalSpent)} of your $${String.format("%.2f", monthlyBudget.total)} budget.")
            // Mark as notified for this month
            sharedPreferences.edit().putBoolean(totalBudgetKey, true).apply()
        }

        // Check Category Budget
        val categoryBudgetAllocated = monthlyBudget.categories.getOrDefault(expenseCategory, 0.0)
        if (categoryBudgetAllocated > 0) { // Only check if a budget is set for this category
            val categorySpendingMap = getCategorySpendingCurrentMonth()
            val categorySpent = categorySpendingMap.getOrDefault(expenseCategory, 0.0)
            val categoryBudgetKey = "${KEY_NOTIFIED_CATEGORY_PREFIX}${expenseCategory}_${currentMonthYear}"

            Log.d(TAG, "Checking category '$expenseCategory'. Spent: $categorySpent, Budget: $categoryBudgetAllocated")

            if (categorySpent > categoryBudgetAllocated && !sharedPreferences.getBoolean(categoryBudgetKey, false)) {
                Log.i(TAG, "Category budget '$expenseCategory' exceeded! Sending notification.")
                sendBudgetNotification("$expenseCategory Budget Exceeded",
                    "You\'ve spent $${String.format("%.2f", categorySpent)} of your $${String.format("%.2f", categoryBudgetAllocated)} budget for $expenseCategory.")
                // Mark as notified for this month
                sharedPreferences.edit().putBoolean(categoryBudgetKey, true).apply()
            }
        }
    }

    private fun sendBudgetNotification(title: String, message: String) {
        // --- Permission Check (Essential!) ---
        if (ActivityCompat.checkSelfPermission(
                appContext, // Use appContext here
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot send notification: POST_NOTIFICATIONS permission not granted.")
            // Optionally: You could inform the user indirectly or store the need to notify later.
            return
        }
        // --- End Permission Check ---

        // Create an explicit intent for an Activity in your app
        val intent = Intent(appContext, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(appContext, NavigationActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true) // Automatically removes the notification when the user taps it

        with(NotificationManagerCompat.from(appContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(BUDGET_NOTIFICATION_ID, builder.build()) // Use a consistent ID for budget alerts
        }
        Log.d(TAG, "Notification sent: Title='$title'")
    }

    // --- End Budget Notification Logic ---
}