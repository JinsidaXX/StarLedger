package com.starledger.app.feature.ledger

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.CategoryPresetColors
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.model.Category
import kotlinx.coroutines.launch

private val EMOJI_CHOICES = listOf(
    "🍜", "🍚", "🍔", "🍰", "🧋", "☕",
    "🚌", "🚇", "🚲", "🚕",
    "📚", "✏️", "🎓", "💻",
    "🏠", "🛏️", "🧴", "🧺", "💡",
    "📱", "💬",
    "🎉", "🎁", "🍻",
    "🎮", "🎬", "🎵", "🎨", "🏸", "⚽",
    "🛍️", "👗", "👟", "💄",
    "🏥", "💊", "🚑",
    "💰", "🧧", "💼", "🏦", "📈",
    "📦", "⭐", "❤️", "🐱", "✈️",
)

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Category?>(null) }
    var creatingExpense by remember { mutableStateOf(false) }
    var creatingIncome by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = "分类管理",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionHeader("支出分类") { creatingExpense = true }
            }
            items(state.expense, key = { it.id }) { category ->
                CategoryRow(category = category, onClick = { editing = category })
            }
            item {
                SectionHeader("收入分类", topPadding = 12) { creatingIncome = true }
            }
            items(state.income, key = { it.id }) { category ->
                CategoryRow(category = category, onClick = { editing = category })
            }
        }
    }

    if (creatingExpense || creatingIncome || editing != null) {
        CategoryEditDialog(
            category = editing,
            isExpense = creatingExpense || creatingIncome || editing?.isExpense == true,
            onDismiss = {
                creatingExpense = false
                creatingIncome = false
                editing = null
            },
            onSave = { category ->
                scope.launch {
                    viewModel.save(category)
                    creatingExpense = false
                    creatingIncome = false
                    editing = null
                }
            },
            onDelete = { category ->
                scope.launch {
                    if (!viewModel.delete(category)) {
                        errorMessage = "该分类已有账目，无法删除"
                    }
                    editing = null
                }
            },
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = SpaceBackground,
            title = { Text("提示", color = TextPrimary) },
            text = { Text(msg, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    topPadding: Int = 0,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "新增$title", tint = AccentBlue)
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(emoji = category.icon, color = category.color.toColor(), size = 36)
            Spacer(Modifier.width(12.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text("编辑", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun CategoryEditDialog(
    category: Category?,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category) { mutableStateOf(category?.icon ?: "📦") }
    var color by remember(category) { mutableStateOf((category?.color ?: 0xFF86A8FF).toColor()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text(if (category == null) "新增分类" else "编辑分类", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("分类名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                )
                // 图标选择
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EMOJI_CHOICES.chunked(8).forEach { rowEmojis ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowEmojis.forEach { e ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (icon == e) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                        .clickable { icon = e },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(e, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
                                }
                            }
                        }
                    }
                }
                // 颜色
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPresetColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == c) Text("✓", color = SpaceBackground)
                        }
                    }
                }
                if (category != null) {
                    TextButton(onClick = { onDelete(category) }) {
                        Text("删除分类", color = RiskRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (category ?: Category(name = name, isExpense = isExpense)).copy(
                            name = name.trim(),
                            icon = icon,
                            color = color.toArgbLong(),
                            isExpense = if (category != null) category.isExpense else isExpense,
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("保存", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = SurfaceSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
)
