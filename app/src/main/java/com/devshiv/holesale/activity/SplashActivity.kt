package com.devshiv.holesale.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devshiv.holesale.R
import com.devshiv.holesale.Utils
import com.devshiv.holesale.Utils.Companion.transparentStatusAndNavigation
import com.devshiv.holesale.databinding.ActivitySplashBinding

class SplashActivity : ComponentActivity() {

    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.transparentStatusAndNavigation()

        setContentView(binding.root)

        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}