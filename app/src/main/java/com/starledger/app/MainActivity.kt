package com.starledger.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.starledger.app.core.design.theme.StarLedgerTheme
import com.starledger.app.navigation.RootNavigation
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("language", Context.MODE_PRIVATE)
            .getString("lang", "system") ?: "system"
        val config = Configuration(newBase.resources.configuration)
        val targetLocale = when (language) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
        Locale.setDefault(targetLocale)
        config.setLocale(targetLocale)
        val wrapped = newBase.createConfigurationContext(config)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StarLedgerTheme {
                RootNavigation()
            }
        }
    }
}
