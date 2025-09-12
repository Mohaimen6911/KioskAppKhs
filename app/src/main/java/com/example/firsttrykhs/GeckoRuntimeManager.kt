package com.example.firsttrykhs

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoRuntimeManager {
    private var runtime: GeckoRuntime? = null

    fun getOrCreate(context: Context): GeckoRuntime {
        if (runtime == null) {
            val settings = GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .arguments(arrayOf(
                    "--pref", "network.trr.mode=0",
                    "--pref", "network.captive-portal-service.enabled=false",
                    "--pref", "network.connectivity-service.enabled=false"
                ))
                .build()
            runtime = GeckoRuntime.create(context.applicationContext, settings)
        }
        return runtime!!
    }
}
