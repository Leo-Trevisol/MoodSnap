package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.br.leo.moodsnap.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object Utils {

    fun getMainColor() : Int{
        return R.color.primary_green
    }


    fun showCustomToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        layout.findViewById<TextView>(R.id.toast_text).text = message

        Toast(context).apply {
            setDuration(duration)
            view = layout
            show()
        }
    }

    fun CardView.flashError(onComplete: () -> Unit) {
        val originalColor = cardBackgroundColor
        val context = this.context
        
        CoroutineScope(Dispatchers.Main).launch {
            // Piscar vermelho
            setCardBackgroundColor(context.getColor(R.color.primary_red))
            delay(200) // Aguarda 100ms
            
            // Volta para a cor original
            setCardBackgroundColor(originalColor)
            delay(50) // Pequeno delay antes de chamar o callback
            
            onComplete()
        }
    }

    fun updateBackGroundColor(context: Context, button: Button, color: Int = getMainColor()) {

        button.setTextColor(Color.BLACK)
        button.backgroundTintList = ColorStateList.valueOf(context.resources.getColor(color))
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

    fun getMoodName(context: Context, moodType: Int): String {
        return when (moodType) {
            0 -> context.getString(R.string.mood_very_happy)
            1 -> context.getString(R.string.mood_happy)
            2 -> context.getString(R.string.mood_neutral)
            3 -> context.getString(R.string.mood_sad)
            4 -> context.getString(R.string.mood_very_sad)
            else -> "Desconhecido"
        }
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

    fun getMoodDrawable(moodType: Int): Int {
        return when (moodType) {
            0 -> R.drawable.muito_feliz
            1 -> R.drawable.feliz
            2 -> R.drawable.neutro
            3 -> R.drawable.triste
            4 -> R.drawable.muito_triste
            else -> R.drawable.neutro
        }
    }
}