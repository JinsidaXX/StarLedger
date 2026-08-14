package com.starledger.app.di

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.EntryPointAccessors

/** 根据已保存的语言偏好应用 App 语言（同步，无阻塞） */
fun applyAppLanguage(context: Context) {
    val store = EntryPointAccessors
        .fromApplication(context, SettingsEntryPoint::class.java)
        .settingsStore()
    val lang = store.getLanguageSync()
    val localeList = when (lang) {
        "zh" -> LocaleListCompat.forLanguageTags("zh-rCN")
        "en" -> LocaleListCompat.forLanguageTags("en")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(localeList)
}
