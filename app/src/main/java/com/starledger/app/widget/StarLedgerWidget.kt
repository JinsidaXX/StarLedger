package com.starledger.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.starledger.app.MainActivity
import com.starledger.app.core.model.Money
import com.starledger.app.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 深空主题色（与 Compose 主题一致）
private val SpaceBackground = Color(0xFF070A12)
private val TextPrimary = Color(0xFFF4F7FF)
private val TextSecondary = Color(0xFFA7B0C3)
private val AccentBlue = Color(0xFF86A8FF)
private val PositiveGreen = Color(0xFF58D6A9)
private val SurplusGold = Color(0xFFF6D477)

/** 星图账本小组件：当前周期收入 / 支出 / 可用支出 / 结余 */
class StarLedgerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = loadSummary(context)
        provideContent {
            StarLedgerWidgetContent(summary)
        }
    }

    private suspend fun loadSummary(context: Context): WidgetSummary =
        withContext(Dispatchers.IO) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
            entryPoint.widgetDataProvider().currentSummary()
        }
}

@Composable
private fun StarLedgerWidgetContent(summary: WidgetSummary) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(SpaceBackground)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(MainActivity::class.java))
            .padding(16.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "星图账本",
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "本期",
                style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 12.sp),
            )
        }

        Spacer(modifier = GlanceModifier.height(14.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MetricItem("收入", Money.format(summary.income), PositiveGreen, GlanceModifier.defaultWeight())
            MetricItem("支出", Money.format(summary.expense), TextPrimary, GlanceModifier.defaultWeight())
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MetricItem("可用支出", Money.format(summary.available), AccentBlue, GlanceModifier.defaultWeight())
            MetricItem("结余", Money.format(summary.surplus), SurplusGold, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 11.sp),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** 小组件接收器 */
class StarLedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StarLedgerWidget()
}
