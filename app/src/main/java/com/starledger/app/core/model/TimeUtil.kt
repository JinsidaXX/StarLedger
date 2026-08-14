package com.starledger.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 时间工具 */
object TimeUtil {

    val zone: ZoneId = ZoneId.systemDefault()

    private fun isZh(): Boolean = Locale.getDefault().language == "zh"

    private fun monthFmt() = if (isZh()) DateTimeFormatter.ofPattern("yyyy年M月")
    else DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    private fun dateFmt() = if (isZh()) DateTimeFormatter.ofPattern("M月d日")
    else DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

    private fun dateFullFmt() = if (isZh()) DateTimeFormatter.ofPattern("yyyy年M月d日")
    else DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    private fun weekFmt() = if (isZh()) DateTimeFormatter.ofPattern("EEEE")
    else DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

    private val shortFmt = DateTimeFormatter.ofPattern("MM.dd")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun monthStart(epochMillis: Long): Long =
        toEpochMillis(YearMonth.from(toLocalDate(epochMillis)).atDay(1))

    fun monthEnd(epochMillis: Long): Long {
        val ym = YearMonth.from(toLocalDate(epochMillis))
        return toEpochMillis(ym.atEndOfMonth().plusDays(1)) - 1
    }

    fun monthStart(year: Int, month: Int): Long =
        toEpochMillis(YearMonth.of(year, month).atDay(1))

    fun monthEnd(year: Int, month: Int): Long {
        val ym = YearMonth.of(year, month)
        return toEpochMillis(ym.atEndOfMonth().plusDays(1)) - 1
    }

    fun monthName(year: Int, month: Int): String =
        if (isZh()) "${year}年${month}月"
        else "${YearMonth.of(year, month).month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year"

    fun monthLabel(month: Int): String =
        if (isZh()) "${month}月"
        else java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    fun formatMonth(epochMillis: Long): String = monthFmt().format(toLocalDate(epochMillis))

    fun formatDate(epochMillis: Long): String = dateFmt().format(toLocalDate(epochMillis))

    fun formatDateFull(epochMillis: Long): String = dateFullFmt().format(toLocalDate(epochMillis))

    fun formatShort(epochMillis: Long): String = shortFmt.format(toLocalDate(epochMillis))

    fun formatRange(start: Long, end: Long): String =
        "${shortFmt.format(toLocalDate(start))} - ${shortFmt.format(toLocalDate(end))}"

    fun dayOfWeek(epochMillis: Long): String = weekFmt().format(toLocalDate(epochMillis))

    fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(timeFmt)

    fun todayMillis(): Long = toEpochMillis(LocalDate.now())

    fun yearMonthOf(epochMillis: Long): YearMonth = YearMonth.from(toLocalDate(epochMillis))

    /** 距离今天的天数（负数表示已过） */
    fun daysFromToday(epochMillis: Long): Long {
        val today = LocalDate.now()
        val target = toLocalDate(epochMillis)
        return java.time.temporal.ChronoUnit.DAYS.between(today, target)
    }

    /** 周期内已过天数 / 总天数 */
    fun elapsedRatio(start: Long, end: Long): Float {
        val now = System.currentTimeMillis()
        if (now <= start) return 0f
        if (now >= end) return 1f
        val total = end - start
        return ((now - start).toDouble() / total).toFloat()
    }
}
