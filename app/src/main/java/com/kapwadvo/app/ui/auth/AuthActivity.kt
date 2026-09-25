package com.kapwadvo.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.databinding.ActivityAuthBinding
import com.kapwadvo.app.ui.admin.AdminActivity
import com.kapwadvo.app.ui.main.MainActivity
import com.kapwadvo.app.ui.owner.BusinessOwnerActivity

class AuthActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val insetsType = androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime()
            val systemBars = insets.getInsets(insetsType)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

    /**
     * Hides the tab bar and shows the email verification screen.
     * Called after a successful sign-up.
     */
    fun showVerifyEmail(email: String, password: String) {
        binding.tabLayout.visibility = android.view.View.GONE
        supportFragmentManager.beginTransaction()
            .replace(binding.authFragmentContainer.id, VerifyEmailFragment.newInstance(email, password))
            .commit()
    }
}
