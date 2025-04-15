package com.example.finlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.finlog.databinding.ActivityNavigationBinding

class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load HomeFragment on start
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
        }

        // Handle BottomNavigationView item clicks
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_records -> {
                    replaceFragment(RecordsFragment())
                    true
                }
                R.id.nav_cards -> {
                    replaceFragment(CardsFragment())
                    true
                }
                R.id.nav_menu -> {
                    replaceFragment(MenuFragment())
                    true
                }
                else -> false
            }
        }

        // Handle FAB click (Add Record)
        binding.fabAdd.setOnClickListener {
            // Placeholder: Switch to RecordsFragment when FAB is clicked
            replaceFragment(RecordsFragment())
            binding.bottomNavigationView.selectedItemId = R.id.nav_records
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}