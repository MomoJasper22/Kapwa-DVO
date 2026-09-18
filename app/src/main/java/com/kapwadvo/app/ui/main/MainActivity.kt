package com.kapwadvo.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityMainBinding
import com.kapwadvo.app.ui.main.explore.ExploreFragment
import com.kapwadvo.app.ui.main.map.MapFragment
import com.kapwadvo.app.ui.main.profile.ProfileFragment
import com.kapwadvo.app.ui.main.saved.SavedFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val exploreFragment = ExploreFragment()
    private val mapFragment = MapFragment()
    private val savedFragment = SavedFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = exploreFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
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
            add(R.id.fragmentContainer, exploreFragment, "explore")
            add(R.id.fragmentContainer, mapFragment, "map").hide(mapFragment)
            add(R.id.fragmentContainer, savedFragment, "saved").hide(savedFragment)
            add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target: Fragment = when (item.itemId) {
                R.id.nav_explore -> exploreFragment
                R.id.nav_map -> mapFragment
                R.id.nav_saved -> savedFragment
                R.id.nav_profile -> profileFragment
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
        binding.bottomNav.selectedItemId = R.id.nav_explore
    }
}
