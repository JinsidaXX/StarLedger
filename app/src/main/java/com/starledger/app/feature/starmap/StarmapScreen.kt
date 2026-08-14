package com.starledger.app.feature.starmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.StarPurple
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.starmap.StarCanvas
import com.starledger.app.core.starmap.StarVisual

@Composable
fun StarmapScreen(
    onStarDetail: (Long) -> Unit,
    viewModel: StarmapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val starsByMonth = state.stars.associateBy { it.month }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding(),
    ) {
        // 年份切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.previousYear() }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一年", tint = TextSecondary)
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${state.year} 年度星座",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    "收 ${Money.formatWithSymbol(state.yearIncome)} · 支 ${Money.formatWithSymbol(state.yearExpense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            IconButton(onClick = { viewModel.nextYear() }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一年", tint = TextSecondary)
            }
        }

        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LegendItem("计划内", StarColorState.BLUE)
            LegendItem("轻微偏差", StarColorState.WARM)
            LegendItem("超支", StarColorState.RED)
            LegendItem("未记录", StarColorState.FOG)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed((1..12).toList()) { _, month ->
                val star = starsByMonth[month]
                MonthStarCell(
                    year = state.year,
                    month = month,
                    star = star,
                    onClick = { star?.let { onStarDetail(it.cycleId) } },
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, state: StarColorState) {
    val color = when (state) {
        StarColorState.BLUE -> com.starledger.app.core.design.theme.AccentBlue
        StarColorState.WARM -> com.starledger.app.core.design.theme.WarningYellow
        StarColorState.ORANGE -> Color(0xFFFF8C42)
        StarColorState.RED -> com.starledger.app.core.design.theme.RiskRed
        StarColorState.FOG -> com.starledger.app.core.design.theme.FogBlue
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape),
        )
        Spacer(Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun MonthStarCell(
    year: Int,
    month: Int,
    star: MonthlyStar?,
    onClick: () -> Unit,
) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = star != null, onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StarCanvas(
                visual = star?.let { StarVisual.from(it) } ?: StarVisual.unrecorded(),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${month}月",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
            )
            Text(
                when {
                    star == null -> "未记录"
                    star.colorState == StarColorState.FOG -> "未记录"
                    else -> "${star.colorState.label}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}
