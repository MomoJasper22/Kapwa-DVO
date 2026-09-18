package com.kapwadvo.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.ui.admin.AdminActivity
import com.kapwadvo.app.ui.auth.AuthActivity
import com.kapwadvo.app.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.kapwadvo.app.R.layout.activity_splash.also { setContentView(it) }

        lifecycleScope.launch {
            delay(1200)
            try {
                if (AuthRepository.hasSession()) {
                    AuthRepository.loadSession()
                    navigateBasedOnRole()
                } else {
                    navigateToAuth()
                }
            } catch (e: Exception) {
                navigateToAuth()
            }
        }
    }

    private fun navigateBasedOnRole() {
        val intent = when {
            UserSession.isAdmin() -> Intent(this, AdminActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
