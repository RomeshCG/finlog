package com.example.finlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finlog.databinding.ActivityNavigationBinding

class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_records -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, RecordsFragment())
                        .commit()
                    true
                }
                R.id.nav_cards -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CardsFragment())
                        .commit()
                    true
                }
                R.id.nav_menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MenuFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Handle FAB click to open AddRecordFragment
        binding.fabAdd.setOnClickListener {
            // Check if AddRecordFragment is already the current fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment !is AddRecordFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}