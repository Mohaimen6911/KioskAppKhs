package com.example.firsttrykhs.utils


import android.content.SharedPreferences

fun saveUrl(sharedPreferences: SharedPreferences, url: String) {
    sharedPreferences.edit().putString("LAST_URL", url).apply()
}

fun loadSavedUrl(sharedPreferences: SharedPreferences): String {
    return sharedPreferences.getString("LAST_URL", "https://www.google.com") ?: "https://www.google.com"
}

fun loadUrlFromTextField(inputUrl: String, onUrlUpdated: (String) -> Unit) {
    val formattedUrl = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
        "https://$inputUrl"
    } else {
        inputUrl
    }
    onUrlUpdated(formattedUrl)
}
