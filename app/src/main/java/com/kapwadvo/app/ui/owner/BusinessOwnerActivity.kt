package com.kapwadvo.app.ui.owner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityBusinessOwnerBinding

class BusinessOwnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessOwnerBinding

    private lateinit var listingsFragment: OwnerListingsFragment
    private lateinit var bookingsFragment: OwnerBookingsFragment
    private lateinit var profileFragment: com.kapwadvo.app.ui.main.profile.ProfileFragment
    private lateinit var activeFragment: Fragment


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

        if (savedInstanceState == null) {
            listingsFragment = OwnerListingsFragment()
            bookingsFragment = OwnerBookingsFragment()
            profileFragment = com.kapwadvo.app.ui.main.profile.ProfileFragment()
            activeFragment = listingsFragment
            initFragments()
        } else {
            listingsFragment = supportFragmentManager.findFragmentByTag("listings") as OwnerListingsFragment
            bookingsFragment = supportFragmentManager.findFragmentByTag("bookings") as OwnerBookingsFragment
            profileFragment = supportFragmentManager.findFragmentByTag("profile") as com.kapwadvo.app.ui.main.profile.ProfileFragment
            activeFragment = supportFragmentManager.fragments.firstOrNull { !it.isHidden && it.tag in listOf("listings", "bookings", "profile") } ?: listingsFragment
        }
        setupBottomNav()
        setupBackStackListener()
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.collect { isOnline ->
                    binding.tvOfflineBanner.visibility = if (isOnline) android.view.View.GONE else android.view.View.VISIBLE
                }
            }
        }
    }
    


    private fun setupBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val hasBackStack = supportFragmentManager.backStackEntryCount > 0
            if (hasBackStack) {
                binding.bottomNav.visibility = android.view.View.GONE
            } else {
                binding.bottomNav.visibility = if (activeFragment == profileFragment) android.view.View.GONE else android.view.View.VISIBLE
            }
        }
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
                
                if (target == profileFragment) {
                    binding.bottomNav.visibility = android.view.View.GONE
                } else {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                }
            }
            true
        }
        if (binding.bottomNav.selectedItemId == R.id.nav_owner_listings) {
            binding.bottomNav.selectedItemId = R.id.nav_owner_listings
        }
    }
}
