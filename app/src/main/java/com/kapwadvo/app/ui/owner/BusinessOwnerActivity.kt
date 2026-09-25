package com.kapwadvo.app.ui.owner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityBusinessOwnerBinding

class BusinessOwnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessOwnerBinding

    private val listingsFragment = OwnerListingsFragment()
    private val bookingsFragment = OwnerBookingsFragment()
    private val profileFragment = com.kapwadvo.app.ui.main.profile.ProfileFragment()
    private var activeFragment: Fragment = listingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBusinessOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initFragments()
        setupBottomNav()
    }

    private fun initFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, listingsFragment, "listings")
            add(R.id.fragmentContainer, bookingsFragment, "bookings").hide(bookingsFragment)
            add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target: Fragment = when (item.itemId) {
                R.id.nav_owner_listings -> listingsFragment
                R.id.nav_owner_bookings -> bookingsFragment
                R.id.nav_owner_profile -> profileFragment
                else -> return@setOnItemSelectedListener false
            }
            if (target != activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .commit()
                activeFragment = target
            }
            true
        }
        binding.bottomNav.selectedItemId = R.id.nav_owner_listings
    }
}
