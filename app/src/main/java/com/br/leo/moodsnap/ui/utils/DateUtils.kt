package com.br.leo.moodsnap.ui.utils

import android.content.Context
import com.br.leo.moodsnap.R
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun getMonthName(context: Context, month: Int): String {
        return when (month) {
            Calendar.JANUARY -> context.getString(R.string.month_january)
            Calendar.FEBRUARY -> context.getString(R.string.month_february)
            Calendar.MARCH -> context.getString(R.string.month_march)
            Calendar.APRIL -> context.getString(R.string.month_april)
            Calendar.MAY -> context.getString(R.string.month_may)
            Calendar.JUNE -> context.getString(R.string.month_june)
            Calendar.JULY -> context.getString(R.string.month_july)
            Calendar.AUGUST -> context.getString(R.string.month_august)
            Calendar.SEPTEMBER -> context.getString(R.string.month_september)
            Calendar.OCTOBER -> context.getString(R.string.month_october)
            Calendar.NOVEMBER -> context.getString(R.string.month_november)
            Calendar.DECEMBER -> context.getString(R.string.month_december)
            else -> ""
        }
    }

    fun formatDateToString(date: Date, pattern: String = "dd/MM/yyyy"): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }

    fun formatMonthYear(context: Context, calendar: Calendar): String {
        val month = getMonthName(context, calendar.get(Calendar.MONTH))
        val year = calendar.get(Calendar.YEAR).toString()
        return "$month $year"
    }

    fun getStartAndEndOfDay(date: Date): Pair<Date, Date> {
        val calendar = Calendar.getInstance().apply { time = date }
        
        val startOfDay = calendar.clone() as Calendar
        startOfDay.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endOfDay = calendar.clone() as Calendar
        endOfDay.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return Pair(startOfDay.time, endOfDay.time)
    }

    fun isDateInFuture(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.after(today)
    }

    fun isDateInFuture(dayOfMonth: Int, displayMonth: Calendar): Boolean {
        val today = Calendar.getInstance()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.YEAR, displayMonth.get(Calendar.YEAR))
        tempCalendar.set(Calendar.MONTH, displayMonth.get(Calendar.MONTH))
        tempCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        return tempCalendar.after(today)
    }

    fun getDayOfWeekName(context: Context, dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> context.getString(R.string.weekday_full_sunday)
            Calendar.MONDAY -> context.getString(R.string.weekday_full_monday)
            Calendar.TUESDAY -> context.getString(R.string.weekday_full_tuesday)
            Calendar.WEDNESDAY -> context.getString(R.string.weekday_full_wednesday)
            Calendar.THURSDAY -> context.getString(R.string.weekday_full_thursday)
            Calendar.FRIDAY -> context.getString(R.string.weekday_full_friday)
            Calendar.SATURDAY -> context.getString(R.string.weekday_full_saturday)
            else -> "Desconhecido"
        }
    }

    fun isToday(dayOfMonth: Int, displayMonth: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == displayMonth.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == displayMonth.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == dayOfMonth
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return isSameDay(cal1, cal2)
    }

    fun getDayOfWeekShortName(context: Context, dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> context.getString(R.string.weekday_sunday)
            Calendar.MONDAY -> context.getString(R.string.weekday_monday)
            Calendar.TUESDAY -> context.getString(R.string.weekday_tuesday)
            Calendar.WEDNESDAY -> context.getString(R.string.weekday_wednesday)
            Calendar.THURSDAY -> context.getString(R.string.weekday_thursday)
            Calendar.FRIDAY -> context.getString(R.string.weekday_friday)
            Calendar.SATURDAY -> context.getString(R.string.weekday_saturday)
            else -> ""
        }
    }

    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun formatDateTime(context: Context, date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = getMonthName(context, calendar.get(Calendar.MONTH))
        val year = calendar.get(Calendar.YEAR)
        val dayOfWeek = getDayOfWeekName(context, calendar.get(Calendar.DAY_OF_WEEK))
        
        return context.getString(
            R.string.date_format,
            dayOfMonth,
            month,
            year
        ) + " - " + dayOfWeek
    }
}