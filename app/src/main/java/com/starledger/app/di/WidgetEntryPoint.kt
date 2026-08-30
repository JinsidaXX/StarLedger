package com.starledger.app.di

import com.starledger.app.widget.WidgetDataProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 供 AppWidget（Glance）等非 Hilt 组件从容器取依赖 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetDataProvider(): WidgetDataProvider
}
