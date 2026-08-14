package com.starledger.app.core.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** 金额工具：内部以“分”为单位存储（Long），显示时转为元。 */
object Money {

    private val symbols = DecimalFormatSymbols(Locale.CHINA).apply {
        groupingSeparator = ','
    }

    /** 123456 -> "1,234.56"；123400 -> "1,234"；0 -> "0" */
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        val yuan = abs / 100
        val fen = (abs % 100).toInt()
        val yuanStr = DecimalFormat("#,##0", symbols).format(yuan)
        return if (fen == 0) "$sign$yuanStr" else "$sign$yuanStr.${fen.toString().padStart(2, '0')}"
    }

    /** 带货币符号：¥1,234.56 */
    fun formatWithSymbol(cents: Long): String = "¥${format(cents)}"

    /** 解析用户输入的“元”字符串为分。支持 "12"、"12.5"、"12.34"、"12.345"(四舍五入到分)。 */
    fun parseYuan(text: String): Long? {
        val t = text.trim().replace(",", "")
        if (t.isEmpty()) return null
        val d = t.toDoubleOrNull() ?: return null
        if (d < 0) return null
        return Math.round(d * 100)
    }
}
