package com.br.leo.moodsnap.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.FontUtils

class WeekdaysAdapter : RecyclerView.Adapter<WeekdaysAdapter.WeekdayViewHolder>() {

    private val weekdays = listOf(
        R.string.weekday_sunday,
        R.string.weekday_monday,
        R.string.weekday_tuesday,
        R.string.weekday_wednesday,
        R.string.weekday_thursday,
        R.string.weekday_friday,
        R.string.weekday_saturday
    )

    class WeekdayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weekdayText: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekday, parent, false)
        return WeekdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekdayViewHolder, position: Int) {
        holder.weekdayText.text = holder.itemView.context.getString(weekdays[position])
        
        // Apply current font to weekday text
        val sharedPreferences = holder.itemView.context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        FontUtils.applyFontToView(holder.itemView.context, holder.weekdayText, currentFont ?: "default")
    }

    override fun getItemCount() = weekdays.size
} 