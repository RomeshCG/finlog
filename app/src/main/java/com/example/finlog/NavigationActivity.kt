package com.example.finlog

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
}