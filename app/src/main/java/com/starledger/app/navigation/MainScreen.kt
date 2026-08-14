package com.starledger.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.starledger.app.R
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.StarPurple
import com.starledger.app.core.design.theme.SurfacePrimary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.feature.home.HomeScreen
import com.starledger.app.feature.ledger.LedgerScreen
import com.starledger.app.feature.planning.PlanningScreen
import com.starledger.app.feature.starmap.StarmapScreen

private data class TabItem(
    @androidx.annotation.StringRes val labelResId: Int,
    val icon: ImageVector,
    val index: Int,
)

private val leftTabs = listOf(
    TabItem(R.string.tab_current, Icons.Filled.Home, 0),
    TabItem(R.string.tab_ledger, Icons.Filled.List, 1),
)

private val rightTabs = listOf(
    TabItem(R.string.tab_planning, Icons.Filled.DateRange, 2),
    TabItem(R.string.tab_starmap, Icons.Filled.Star, 3),
)

@Composable
fun MainScreen(
    onAddTransaction: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onPlanDetail: (Long) -> Unit,
    onPlanEdit: (Long?) -> Unit,
    onStarDetail: (Long) -> Unit,
    onReview: (Long) -> Unit,
    onTemplateEdit: (Long) -> Unit,
    onOwnedItems: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground),
    ) {
        when (selectedTab) {
            0 -> HomeScreen(
                onOpenSettings = onOpenSettings,
                onOpenAccounts = onOpenAccounts,
                onEditTransaction = onEditTransaction,
                onPlanDetail = onPlanDetail,
                onPlanEdit = onPlanEdit,
                onStarDetail = onStarDetail,
                onReview = onReview,
                onGoPlanning = { selectedTab = 2 },
            )
            1 -> LedgerScreen(
                onEditTransaction = onEditTransaction,
                onOpenAccounts = onOpenAccounts,
                onOpenCategories = onOpenCategories,
            )
            2 -> PlanningScreen(
                onPlanDetail = onPlanDetail,
                onPlanEdit = onPlanEdit,
                onTemplateEdit = onTemplateEdit,
                onOwnedItems = onOwnedItems,
            )
            3 -> StarmapScreen(
                onStarDetail = onStarDetail,
            )
        }

        BottomBar(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
            onAdd = onAddTransaction,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BottomBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceBackground.copy(alpha = 0.95f))
            .padding(bottom = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftTabs.forEach { tab ->
                TabButton(
                    tab = tab,
                    selected = selectedTab == tab.index,
                    onSelect = { onSelect(tab.index) },
                    modifier = Modifier.weight(1f),
                )
            }

            // 中央快速记账按钮
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(AccentBlue, StarPurple))
                    )
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_transaction),
                    tint = Color(0xFF070A12),
                    modifier = Modifier.size(28.dp),
                )
            }

            rightTabs.forEach { tab ->
                TabButton(
                    tab = tab,
                    selected = selectedTab == tab.index,
                    onSelect = { onSelect(tab.index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: TabItem,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) AccentBlue else TextSecondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = stringResource(tab.labelResId), tint = color, modifier = Modifier.size(22.dp))
        Text(
            stringResource(tab.labelResId),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TextSecondary,
        )
    }
}
