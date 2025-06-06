package com.example.firsttrykhs

import AdminExitDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.mozilla.geckoview.GeckoSession


class MainActivity : ComponentActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName
    private lateinit var kioskPasswordManager: KioskPasswordManager
    private var tapCount = 0
    private val tapHandler = Handler(Looper.getMainLooper())

    private var isKioskModeEnabled by mutableStateOf(true)
    private var showPasswordChangeDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(
            "GViewCheck",
            "Using GeckoView version: " + GeckoSession::class.java.getPackage().implementationVersion
        )

        kioskPasswordManager = KioskPasswordManager(this)
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        handleTerminalCommand(intent)

        if (isKioskModeEnabled) enableKioskMode()

        val geckoRuntime = GeckoRuntimeManager.getOrCreate(applicationContext)

        setContent {
            var showAdminDialog by remember { mutableStateOf(false) }
            var showReLockDialog by remember { mutableStateOf(false) }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        registerTap {
                            if (isKioskModeEnabled) showAdminDialog = true
                            else showReLockDialog = true
                        }
                    },
            ) {
                WebBrowser(Modifier.padding(it), geckoRuntime = geckoRuntime)

                if (showAdminDialog) {
                    AdminExitDialog(
                        context = this,
                        onDismiss = { showAdminDialog = false },
                        onUnlock = {
                            exitKioskMode()
                            isKioskModeEnabled = false
                            showAdminDialog = false
                        }
                    )
                }

                if (showReLockDialog) {
                    ReLockDialog(
                        context = this,
                        onDismiss = { showReLockDialog = false },
                        onReLock = {
                            enableKioskMode()
                            isKioskModeEnabled = true
                            showReLockDialog = false
                        }
                    )
                }

                if (showPasswordChangeDialog) {
                    FourEyesPasswordChangeDialog(
                        context = this,
                        showDialog = showPasswordChangeDialog,
                        onDismiss = { showPasswordChangeDialog = false },
                        onPasswordChanged = {
                            showPasswordChangeDialog = false
                            Toast.makeText(this, "Passwort erfolgreich geändert", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleTerminalCommand(intent)
    }

    private fun handleTerminalCommand(intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_SEND) {
                val command = it.getStringExtra(Intent.EXTRA_TEXT)
                command?.let { cmd ->
                    when {
                        cmd.startsWith("set-primary ") -> {
                            val pass = cmd.substringAfter("set-primary ").trim()
                            if (pass.length >= 4) {
                                kioskPasswordManager.savePrimaryPassword(pass)
                                Toast.makeText(this, "Primäres Passwort aktualisiert", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }

                        cmd.startsWith("set-secondary ") -> {
                            val pass = cmd.substringAfter("set-secondary ").trim()
                            if (pass.length >= 4) {
                                kioskPasswordManager.saveSecondaryPassword(pass)
                                Toast.makeText(this, "Sekundäres Passwort aktualisiert", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun registerTap(onSuccess: () -> Unit) {
        tapCount++
        tapHandler.postDelayed({ tapCount = 0 }, 800)
        if (tapCount >= 5) {
            onSuccess()
            tapCount = 0
        }
    }

    private fun enableKioskMode() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(packageName))
            startLockTask()
            Log.d("MainActivity", "Kiosk Mode Enabled")
        } else {
            Log.e("MainActivity", "App is not the device owner")
        }
    }

    private fun exitKioskMode() {
        stopLockTask()
        Log.d("MainActivity", "Exited Kiosk Mode")
    }

    fun changeSecondaryPassword(newPassword: String) {
        if (newPassword.length < 4) {
            Toast.makeText(this, "Password too short (min 4 chars)", Toast.LENGTH_LONG).show()
            return
        }

        kioskPasswordManager.saveSecondaryPassword(newPassword)
        val hash = kioskPasswordManager.getSecondaryHash()
        Log.d("PasswordChange", "New hash: ${hash.take(10)}...")
    }
}
