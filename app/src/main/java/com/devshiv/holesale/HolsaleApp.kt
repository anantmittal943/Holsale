package com.devshiv.holesale

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.libraries.places.api.Places

class HolsaleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val apiKey = BuildConfig.MAPS_API_KEY
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
    }
}