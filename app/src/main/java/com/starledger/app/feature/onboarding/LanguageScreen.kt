package com.starledger.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfacePrimary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.starmap.StarCanvas
import com.starledger.app.core.starmap.StarVisual

@Composable
fun LanguageScreen(onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            StarCanvas(
                visual = StarVisual(
                    brightness = 0.8f,
                    rays = emptyList(),
                    hasTransactions = true,
                    allocationDone = true,
                ),
                modifier = Modifier.size(140.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "选择语言 · Choose Language",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(40.dp))

        LanguageOption("简体中文", "Simplified Chinese", Modifier) { onSelected("zh") }
        Spacer(Modifier.height(16.dp))
        LanguageOption("English", "English", Modifier) { onSelected("en") }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePrimary)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
