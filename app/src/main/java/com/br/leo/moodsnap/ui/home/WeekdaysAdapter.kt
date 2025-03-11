package com.br.leo.moodsnap.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R

class WeekdaysAdapter : RecyclerView.Adapter<WeekdaysAdapter.WeekdayViewHolder>() {

    private val weekdays = listOf("D", "S", "T", "Q", "Q", "S", "S")

    class WeekdayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weekdayText: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekday, parent, false)
        return WeekdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekdayViewHolder, position: Int) {
        holder.weekdayText.text = weekdays[position]
    }

    override fun getItemCount() = weekdays.size
} 