package com.starledger.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 数据变更后主动刷新所有小组件 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refresh() {
        val manager = GlanceAppWidgetManager(context)
        listOf<GlanceAppWidget>(StarLedgerWidget(), StarLedgerSmallWidget()).forEach { widget ->
            runCatching {
                manager.getGlanceIds(widget.javaClass).forEach { id ->
                    runCatching { widget.update(context, id) }
                }
            }
        }
    }
}
