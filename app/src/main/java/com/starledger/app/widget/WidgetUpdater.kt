package com.starledger.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 数据变更后主动刷新小组件 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refresh() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(StarLedgerWidget::class.java)
            ids.forEach { id ->
                runCatching { StarLedgerWidget().update(context, id) }
            }
        }
    }
}
