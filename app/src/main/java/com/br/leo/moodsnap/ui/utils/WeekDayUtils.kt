package com.br.leo.moodsnap.ui.utils

import android.content.Context
import com.br.leo.moodsnap.R
import java.util.*

object WeekDayUtils {
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
} 