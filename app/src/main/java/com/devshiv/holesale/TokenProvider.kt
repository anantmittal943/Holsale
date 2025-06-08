package com.devshiv.holesale

import com.google.android.libraries.places.api.auth.PlacesAppCheckTokenProvider
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck

class TokenProvider : PlacesAppCheckTokenProvider {
    override fun fetchAppCheckToken(): ListenableFuture<String> {
        val future = SettableFuture.create<String>()
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .addOnSuccessListener { appCheckToken: AppCheckToken ->
                future.set(appCheckToken.token)
            }
            .addOnFailureListener { ex: Exception ->
                future.setException(ex)
            }

        return future
    }
}