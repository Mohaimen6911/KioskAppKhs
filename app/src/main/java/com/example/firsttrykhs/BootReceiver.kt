package com.example.firsttrykhs

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted, restarting app...")

            // Start MainActivity after reboot
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context?.startActivity(launchIntent)

            // Enable Kiosk Mode immediately after reboot
            val devicePolicyManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponentName = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                // Set LockTask packages immediately
                devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(context.packageName))
                Log.d("BootReceiver", "LockTask Set after Reboot")
            }
        }
    }
}
