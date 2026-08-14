package com.starledger.app

import android.app.Application
import com.starledger.app.di.applyAppLanguage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StarLedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyAppLanguage(this)
    }
}
