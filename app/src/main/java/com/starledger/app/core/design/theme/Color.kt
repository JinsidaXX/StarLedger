package com.starledger.app.core.design.theme

import androidx.compose.ui.graphics.Color

// 深空背景
val SpaceBackground = Color(0xFF070A12)

// 主卡片
val SurfacePrimary = Color(0xFF0E1421)

// 次级卡片
val SurfaceSecondary = Color(0xFF151D2E)

// 主文字
val TextPrimary = Color(0xFFF4F7FF)

// 次级文字
val TextSecondary = Color(0xFFA7B0C3)

// 主强调色
val AccentBlue = Color(0xFF86A8FF)

// 星图紫色
val StarPurple = Color(0xFFA78BFA)

// 正向绿色
val PositiveGreen = Color(0xFF58D6A9)

// 提醒黄色
val WarningYellow = Color(0xFFF3B95F)

// 风险红色
val RiskRed = Color(0xFFFF6B7A)

// 结余金色
val SurplusGold = Color(0xFFF6D477)

// 星雾灰蓝
val FogBlue = Color(0xFF5A6B8C)

// 分隔线
val DividerDark = Color(0xFF232D42)

// 分类预设颜色
val CategoryPresetColors = listOf(
    Color(0xFF86A8FF), // 蓝
    Color(0xFF58D6A9), // 绿
    Color(0xFFF3B95F), // 黄
    Color(0xFFFF6B7A), // 红
    Color(0xFFA78BFA), // 紫
    Color(0xFF5BC8E8), // 青
    Color(0xFFF6D477), // 金
    Color(0xFFF08CB4), // 粉
    Color(0xFF8BC98A), // 草绿
    Color(0xFFD9A066), // 棕
)

/** Color → ARGB Long（0xAARRGGBB）。不要用 Color.value，它包含颜色空间打包。 */
fun Color.toArgbLong(): Long {
    val a = (alpha * 255 + 0.5f).toInt()
    val r = (red * 255 + 0.5f).toInt()
    val g = (green * 255 + 0.5f).toInt()
    val b = (blue * 255 + 0.5f).toInt()
    return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/** ARGB Long → Color */
fun Long.toColor(): Color = Color(
    red = ((this shr 16) and 0xFF) / 255f,
    green = ((this shr 8) and 0xFF) / 255f,
    blue = (this and 0xFF) / 255f,
    alpha = ((this shr 24) and 0xFF) / 255f,
)
