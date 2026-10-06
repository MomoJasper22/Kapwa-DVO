package com.kapwadvo.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
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

    private val tabHistory = java.util.Stack<Int>()
    private var isProgrammaticSelection = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val bottom = if (ime.bottom > 0) ime.bottom else 0
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }

        initFragments()
        setupBottomNav()
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.collect { isOnline ->
                    binding.tvOfflineBanner.visibility = if (isOnline) android.view.View.GONE else android.view.View.VISIBLE
                    
                    if (isOnline) {
                        val oneTimeSync = androidx.work.OneTimeWorkRequestBuilder<com.kapwadvo.app.workers.NetworkSyncWorker>().build()
                        androidx.work.WorkManager.getInstance(this@MainActivity).enqueueUniqueWork(
                            "KapwaNetworkReconnectSync",
                            androidx.work.ExistingWorkPolicy.KEEP,
                            oneTimeSync
                        )
                    }
                }
            }
        }
        
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                binding.bottomNav.visibility = android.view.View.GONE
            } else {
                binding.bottomNav.visibility = if (activeFragment == profileFragment) android.view.View.GONE else android.view.View.VISIBLE
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else if (tabHistory.isNotEmpty()) {
                    val prevTab = tabHistory.pop()
                    isProgrammaticSelection = true
                    binding.bottomNav.selectedItemId = prevTab
                    isProgrammaticSelection = false
                } else {
                    // Minimize the app instead of closing it completely
                    moveTaskToBack(true)
                }
            }
        })
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
            if (binding.bottomNav.selectedItemId != item.itemId && !isProgrammaticSelection) {
                tabHistory.push(binding.bottomNav.selectedItemId)
            }

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
                
                // Hide bottom nav when on profile
                if (target == profileFragment) {
                    binding.bottomNav.visibility = android.view.View.GONE
                } else {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                }
            }
            true
        }
        binding.bottomNav.selectedItemId = R.id.nav_explore
    }



}
