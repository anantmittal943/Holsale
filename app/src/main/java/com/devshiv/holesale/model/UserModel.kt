package com.devshiv.holesale.model

import android.net.Uri

data class UserModel(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: Uri?,
    val isEmailVerified: Boolean
)
