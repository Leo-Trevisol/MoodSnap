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
} 