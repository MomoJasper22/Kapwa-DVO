package com.kapwadvo.app.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    private val listingsFragment = AdminListingsFragment()
    private val reviewFragment = AdminReviewFragment()
    private val appsFragment = AdminAppsFragment()
    private var activeFragment: Fragment = listingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initFragments()
        setupBottomNav()
    }

    private fun initFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, listingsFragment, "listings")
            add(R.id.fragmentContainer, reviewFragment, "review").hide(reviewFragment)
            add(R.id.fragmentContainer, appsFragment, "apps").hide(appsFragment)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target: Fragment = when (item.itemId) {
                R.id.nav_admin_listings -> listingsFragment
                R.id.nav_admin_review -> reviewFragment
                R.id.nav_admin_apps -> appsFragment
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
