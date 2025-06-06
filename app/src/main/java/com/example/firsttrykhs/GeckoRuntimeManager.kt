package com.example.firsttrykhs

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime

object GeckoRuntimeManager {
    private var runtime: GeckoRuntime? = null

    fun getOrCreate(context: Context): GeckoRuntime {
        if (runtime == null) {
            runtime = GeckoRuntime.create(context.applicationContext)
        }
        return runtime!!
    }
}
