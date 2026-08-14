package com.starledger.app.feature.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.StarPurple
import com.starledger.app.core.design.theme.SurfacePrimary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.starmap.StarCanvas
import com.starledger.app.core.starmap.StarVisual

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            StarCanvas(
                visual = StarVisual(
                    brightness = 0.85f,
                    rays = emptyList(),
                    hasTransactions = true,
                    allocationDone = true,
                    reviewDone = true,
                ),
                modifier = Modifier.size(160.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "星图账本",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "让每一笔收支，都有自己的轨道",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(40.dp))

        listOf(
            "🔒  数据只保存在你的手机本地",
            "🤫  安静记账，不打扰、不评判",
            "🌙  无需登录，离线也能使用",
            "⭐  每月一颗恒星，一年一个星座",
        ).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(SurfacePrimary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                contentColor = SpaceBackground,
            ),
        ) {
            Text("开始使用", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "自由、开源、免费 · GPL-3.0",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
