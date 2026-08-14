package com.starledger.app.feature.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.starledger.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.EmptyState
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.OwnedItem
import com.starledger.app.core.model.TimeUtil

@Composable
fun OwnedItemsScreen(
    onBack: () -> Unit,
    viewModel: OwnedItemsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.items_title), onBack = onBack) {
        if (items.isEmpty()) {
            EmptyState(
                emoji = "🎒",
                title = stringResource(R.string.items_empty),
                subtitle = stringResource(R.string.items_empty_hint),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.starledger.app.core.design.theme.SpaceBackground),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text(
                                    stringResource(R.string.items_bought_on, TimeUtil.formatDateFull(item.purchaseDate), Money.formatWithSymbol(dailyCost(item))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            MoneyText(
                                cents = item.purchasePrice,
                                withSymbol = true,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AccentBlue,
                            )
                            TextButton(onClick = { viewModel.delete(item) }) {
                                Text(stringResource(R.string.delete), color = RiskRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 日均消费（分/天）= 商品价值 ÷ 持有天数，四舍五入到分 */
private fun dailyCost(item: OwnedItem): Long {
    val days = java.time.temporal.ChronoUnit.DAYS.between(
        TimeUtil.toLocalDate(item.purchaseDate),
        java.time.LocalDate.now(),
    ) + 1
    return (item.purchasePrice + days / 2) / days
}
