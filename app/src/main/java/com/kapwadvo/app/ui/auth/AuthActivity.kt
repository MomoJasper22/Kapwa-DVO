package com.kapwadvo.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.databinding.ActivityAuthBinding
import com.kapwadvo.app.ui.admin.AdminActivity
import com.kapwadvo.app.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTabs()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Login"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Sign Up"))

        showFragment(LoginFragment())

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showFragment(LoginFragment())
                    1 -> showFragment(SignupFragment())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.authFragmentContainer.id, fragment)
            .commit()
    }

    fun navigateToMain() {
        val intent = when {
            UserSession.isAdmin() -> Intent(this, AdminActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    fun navigateToGuestMain() {
        UserSession.isGuest = true
        UserSession.role = "guest"
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
