package com.example.firsttrykhs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordManager(
    private val context: Context,
    private var correctPassword: String? = null
) {
    private val pref: SharedPreferences by lazy { getEncryptedSharedPreferences() }
    private val TEMPORARY_PASSWORD = "Temporary@123" // Updated to meet KHS policy

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "admin_password_pref",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Logging methods
    private fun logLoginAttempt(username: String, isSuccessful: Boolean) {
        if (isSuccessful) {
            Log.d("PasswordManager", "Login attempt successful for user: $username")
        } else {
            Log.w("PasswordManager", "Failed login attempt for user: $username")
        }
    }

    private fun logPasswordChange() {
        Log.d("PasswordManager", "Password changed successfully.")
    }

    private fun logFailedPasswordVerification() {
        Log.w("PasswordManager", "Failed password verification attempt.")
    }

    private fun logSuccessfulPasswordVerification() {
        Log.d("PasswordManager", "Password verification successful.")
    }

    private fun logEncryptionOperation() {
        Log.d("PasswordManager", "Encryption operation performed successfully.")
    }

    private fun logDecryptionOperation() {
        Log.d("PasswordManager", "Decryption operation performed successfully.")
    }

    // KHS Password Policy Validation
    private fun isPasswordValid(password: String): Boolean {
        if (password.length < 9 || password.length > 40) return false

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
        }

        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    private fun initializePassword() {
        val storedPassword = getPasswordHash()
        if (storedPassword.isEmpty()) {
            Log.d("PasswordManager", "Password not set, initializing with temporary password.")
            savePassword(TEMPORARY_PASSWORD)
        } else {
            correctPassword = storedPassword
        }
    }

    fun savePassword(newPassword: String) {
        if (!isPasswordValid(newPassword)) {
            throw IllegalArgumentException("Password does not meet required criteria")
        }

        val bcryptHashString = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
        pref.edit().putString("ADMIN_PASSWORD", bcryptHashString).apply()
        correctPassword = bcryptHashString
        Log.d("PasswordManager", "Admin password saved (hashed): [HASHED]")
    }

    fun getPasswordHash(): String {
        return pref.getString("ADMIN_PASSWORD", "") ?: ""
    }

    fun verifyPassword(inputPassword: String): Boolean {
        val storedPassword = getPasswordHash()
        val result = BCrypt.verifyer().verify(inputPassword.toCharArray(), storedPassword)
        if (result.verified) {
            logSuccessfulPasswordVerification()
        } else {
            logFailedPasswordVerification()
        }
        return result.verified
    }

    // Logging encryption and decryption
    fun encryptPassword(password: String): String {
        logEncryptionOperation()
        // Perform encryption logic here
        return password // Placeholder for actual encryption
    }

    fun decryptPassword(encryptedPassword: String): String {
        logDecryptionOperation()
        // Perform decryption logic here
        return encryptedPassword // Placeholder for actual decryption
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChangePasswordDialog(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        onPasswordChanged: (String) -> Unit
    ) {
        var adminPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showPasswordRules by remember { mutableStateOf(false) }

        val errorBorderColor = Color.Red
        val normalBorderColor = Color.Gray

        if (showDialog) {
            AlertDialog(
                modifier = Modifier.padding(16.dp),
                onDismissRequest = onDismiss,
                title = { Text("Passwort ändern", fontSize = 18.sp) },
                text = {
                    Column {
                        if (showPasswordRules) {
                            Text(
                                text = "KHS Passwortrichtlinie:\n" +
                                        "• 9-40 Zeichen\n" +
                                        "• Großbuchstabe (A-Z)\n" +
                                        "• Kleinbuchstabe (a-z)\n" +
                                        "• Ziffer (0-9)\n" +
                                        "• Sonderzeichen (!@#\$ etc.)",
                                fontSize = 14.sp,
                                color = Color.Blue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        TextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Aktuelles Passwort") },
                            singleLine = true,
                            isError = errorMessage != null && adminPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && adminPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort") },
                            singleLine = true,
                            isError = errorMessage != null && newPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && newPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            ),
                            supportingText = {
                                if (newPassword.isNotEmpty() && !isPasswordValid(newPassword)) {
                                    Text(
                                        text = "Das Passwort erfüllt nicht die Kriterien",
                                        color = Color.Red
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort bestätigen") },
                            singleLine = true,
                            isError = errorMessage != null && confirmNewPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && confirmNewPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            )
                        )

                        TextButton(
                            onClick = { showPasswordRules = !showPasswordRules },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = if (showPasswordRules) "Passwortregeln ausblenden" else "Passwortregeln anzeigen",
                                fontSize = 12.sp
                            )
                        }

                        errorMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = Color.Red, fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when {
                                adminPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty() -> {
                                    errorMessage = "Bitte alle Felder ausfüllen."
                                }
                                !verifyPassword(adminPassword) -> {
                                    errorMessage = "Aktuelles Passwort ist falsch."
                                    adminPassword = ""
                                }
                                !isPasswordValid(newPassword) -> {
                                    errorMessage = "Neues Passwort erfüllt nicht die KHS-Richtlinien."
                                    newPassword = ""
                                    confirmNewPassword = ""
                                }
                                newPassword != confirmNewPassword -> {
                                    errorMessage = "Neues Passwort und Bestätigung stimmen nicht überein."
                                    newPassword = ""
                                    confirmNewPassword = ""
                                }
                                else -> {
                                    try {
                                        savePassword(newPassword)
                                        onPasswordChanged(newPassword)
                                        logPasswordChange() // Log password change
                                        onDismiss()
                                        adminPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                        errorMessage = ""
                                    } catch (e: IllegalArgumentException) {
                                        errorMessage = e.message
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Bestätigen", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Abbrechen", style = MaterialTheme.typography.bodyMedium)
                    }
                    adminPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    errorMessage = ""

                },
                containerColor = Color.LightGray
            )
        }
    }

    init {
        initializePassword()
    }
}
