package com.starledger.app.feature.planning

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.starledger.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary

@Composable
fun PlanEditScreen(
    planId: Long?,
    onDone: () -> Unit,
    viewModel: PlanEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(planId) { viewModel.load(planId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(SpaceBackground)) {}
        return
    }

    ScreenScaffold(
        title = if (planId == null) stringResource(R.string.plan_edit_title) else stringResource(R.string.plan_edit_edit_title),
        onBack = onDone,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.setName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_what)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { viewModel.setAmount(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_estimated_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                Text(stringResource(R.string.plan_cooling), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 7, 14, 30).forEach { days ->
                        val selected = state.coolingDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                .clickable { viewModel.setCoolingDays(days) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                if (days == 0) stringResource(R.string.plan_cooling_off) else stringResource(R.string.plan_cooling_days, days),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) AccentBlue else TextSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.plan_cooling_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            item {
                OutlinedTextField(
                    value = state.reason,
                    onValueChange = { viewModel.setReason(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_why)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.alternative,
                    onValueChange = { viewModel.setAlternative(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_alternative)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.targetDateText,
                    onValueChange = { viewModel.setTargetDateText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_target_date)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { viewModel.setNote(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tx_note)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.save() },
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = SpaceBackground,
                        disabledContainerColor = SurfaceSecondary,
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    Text(stringResource(R.string.plan_create), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = SurfaceSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
)
