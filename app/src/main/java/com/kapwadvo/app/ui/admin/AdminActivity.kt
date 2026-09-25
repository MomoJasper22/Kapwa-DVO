package com.kapwadvo.app.ui.admin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityAdminBinding
import com.kapwadvo.app.ui.main.profile.ProfileFragment

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    private val listingsFragment = AdminListingsFragment()
    private val reviewFragment = AdminReviewFragment()
    private val appsFragment = AdminAppsFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = listingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Only apply top padding to the root layout to protect from status bar
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initFragments()
        setupBottomNav()
    }

    private fun initFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, listingsFragment, "listings")
            add(R.id.fragmentContainer, reviewFragment, "review").hide(reviewFragment)
            add(R.id.fragmentContainer, appsFragment, "apps").hide(appsFragment)
            add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target: Fragment = when (item.itemId) {
                R.id.nav_admin_listings -> listingsFragment
                R.id.nav_admin_review -> reviewFragment
                R.id.nav_admin_apps -> appsFragment
                R.id.nav_admin_profile -> profileFragment
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
        binding.bottomNav.selectedItemId = R.id.nav_admin_listings
    }
}
