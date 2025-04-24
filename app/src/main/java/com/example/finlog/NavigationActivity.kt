package com.example.finlog

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.finlog.databinding.ActivityNavigationBinding

// Implement the listener interface defined in HomeFragment
class NavigationActivity : AppCompatActivity(), HomeFragment.HomeFragmentListener {

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var dataManager: DataManager
    private val TAG = "NavigationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize DataManager
        dataManager = DataManager(this)

        // Hide the action bar (removes the "FinLog" title)
        supportActionBar?.hide()

        createNotificationChannel()
        requestNotificationPermission()

        // Load HomeFragment on start
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
        }

        // Handle BottomNavigationView item clicks
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is HomeFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                    }
                    true
                }
                R.id.nav_records -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is RecordsFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, RecordsFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    true
                }
                R.id.nav_cards -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is CardsFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, CardsFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    true
                }
                R.id.nav_menu -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is MenuFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, MenuFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    true
                }
                else -> false
            }
        }

        // Handle FAB click to open AddRecordFragment
        binding.fabAdd.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment !is AddRecordFragment) {
                Log.d(TAG, "FAB clicked - navigating to AddRecordFragment")
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name) // Need to add this string resource
            val descriptionText = getString(R.string.channel_description) // Need to add this string resource
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        } else {
            Log.d(TAG, "Notification channel not required for this API level.")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted, request it
                Log.d(TAG, "Requesting notification permission.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "Notification permission already granted.")
                // Permission has already been granted
            }
        }
    }

    // Handle the result of the permission request (optional, can just let user grant later)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Notification permission granted by user.")
                } else {
                     Log.w(TAG, "Notification permission denied by user.")
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At this point, you could also prompt the user to go to settings.
                }
                return
            }
            // Handle other permission results if needed
        }
    }

    // --- Implementation of HomeFragmentListener ---
    override fun navigateToManageCategories() {
        Log.d(TAG, "Navigating to ManageCategoriesFragment from listener")
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is ManageCategoriesFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ManageCategoriesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun navigateToSetBudget() {
        Log.d(TAG, "Navigating to SetBudgetFragment from listener")
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is SetBudgetFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SetBudgetFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    // --- End Implementation ---

    companion object {
        const val CHANNEL_ID = "FinLogBudgetChannel"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }
}