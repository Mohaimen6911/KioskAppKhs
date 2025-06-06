// KioskPasswordManager.kt
package com.example.firsttrykhs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import at.favre.lib.crypto.bcrypt.BCrypt
import java.io.File

class KioskPasswordManager(private val context: Context) {
    private val pref: SharedPreferences by lazy { getEncryptedSharedPreferences() }

    companion object {
        private const val PREF_NAME = "kiosk_password_pref"
        private const val PRIMARY_KEY = "primary_password"
        private const val SECONDARY_KEY = "secondary_password"
        private const val DEFAULT_PRIMARY = "KhsDefault@123"
        private const val DEFAULT_SECONDARY = "Secondary@456"
        private const val MIN_PASSWORD_LENGTH = 9
        private const val MAX_PASSWORD_LENGTH = 40
    }

    private val filesDir: File
        get() = context.filesDir

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun isPasswordCompliant(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
            return false
        }

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSpecial = false

        for (char in password) {
            when {
                char.isUpperCase() -> hasUpper = true
                char.isLowerCase() -> hasLower = true
                char.isDigit() -> hasDigit = true
                !char.isLetterOrDigit() -> hasSpecial = true
            }

            if (hasUpper && hasLower && hasDigit && hasSpecial) {
                return true
            }
        }

        return false
    }

    init {
        initializePasswords()
    }

    private fun initializePasswords() {
        if (getPrimaryHash().isEmpty()) savePrimaryPassword(DEFAULT_PRIMARY)
        if (getSecondaryHash().isEmpty()) saveSecondaryPassword(DEFAULT_SECONDARY)
    }

    // Primary Password Functions
    fun savePrimaryPassword(password: String) {
        if (!isPasswordCompliant(password)) {
            throw SecurityException("Password doesn't meet security requirements")
        }
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        pref.edit().putString(PRIMARY_KEY, hash).apply()
        Log.d("PasswordManager", "Primary password updated")
    }

    fun verifyPrimaryPassword(input: String): Boolean {
        return verify(input, getPrimaryHash())
    }

    fun getPrimaryHash(): String {
        return pref.getString(PRIMARY_KEY, "") ?: ""
    }

    // Secondary Password Functions
    fun saveSecondaryPassword(password: String) {
        if (!isPasswordCompliant(password)) {
            throw SecurityException("Password doesn't meet security requirements")
        }
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        pref.edit().putString(SECONDARY_KEY, hash).apply()
        Log.d("PasswordManager", "Secondary password updated")
    }

    fun verifySecondaryPassword(input: String): Boolean {
        return verify(input, getSecondaryHash())
    }

    fun getSecondaryHash(): String {
        return pref.getString(SECONDARY_KEY, "") ?: ""
    }

    // Common Verification
    private fun verify(input: String, storedHash: String): Boolean {
        return when {
            storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") ->
                BCrypt.verifyer().verify(input.toCharArray(), storedHash).verified
            else -> input == storedHash
        }
    }

    fun debugDumpPasswords() {
        Log.d("PasswordStorage", "=== Current Password Hashes ===")
        Log.d("PasswordStorage", "Primary: ${getPrimaryHash()}")
        Log.d("PasswordStorage", "Secondary: ${getSecondaryHash()}")

        val prefsFile = File(filesDir.parent, "shared_prefs/kiosk_password_pref.xml")
        if (prefsFile.exists()) {
            Log.d("PasswordStorage", "File content:\n${prefsFile.readText()}")
        } else {
            Log.e("PasswordStorage", "Prefs file doesn't exist!")
        }
    }

    // Terminal Commands
    fun handleCommand(command: String): Boolean {
        return when {
            command.startsWith("set-primary ") -> {
                val pass = command.substringAfter("set-primary ").trim()
                if (isPasswordCompliant(pass)) {
                    savePrimaryPassword(pass)
                    true
                } else false
            }
            command.startsWith("set-secondary ") -> {
                val pass = command.substringAfter("set-secondary ").trim()
                if (isPasswordCompliant(pass)) {
                    saveSecondaryPassword(pass)
                    true
                } else false
            }
            else -> false
        }
    }
}