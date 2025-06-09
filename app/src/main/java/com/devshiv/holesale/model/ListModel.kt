package com.devshiv.holesale.model

import android.util.Log
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

data class ListModel(
    val id: String? = "",
    val imageUrl: String? = "",
    val phoneNumber: String? = "",
    val address: String? = "",
    val createdAt: String,
    val updatedAt: String
) : Serializable {
    constructor() : this("", "", "", "", "", "")
    companion object {
        val TAG = "xyz"
        fun getCurrentTime(): String {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(System.currentTimeMillis())
        }

        fun formatDate(inputDate: String): String {
            try {
                val inputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                Log.d(TAG, "formatDate: " + e.message)
            }
            return ""
        }

        fun formatTime(inputDate: String): String {
            try {
                val inputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                Log.d(TAG, "formatDate: " + e.message)
            }
            return ""
        }
    }
}
