package com.devshiv.holesale

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class HolsaleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}