package com.br.leo.moodsnap.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import kotlin.math.roundToInt

class WeekdayCountAdapter(
    private val context: Context,
    private val weekdayData: List<Pair<String, Int>>,
    private val totalCount: Int
) : RecyclerView.Adapter<WeekdayCountAdapter.WeekdayViewHolder>() {

    class WeekdayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val weekdayText: TextView = itemView.findViewById(R.id.weekday_text)
        val countText: TextView = itemView.findViewById(R.id.count_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekday_count, parent, false)
        return WeekdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekdayViewHolder, position: Int) {
        val (weekday, count) = weekdayData[position]
        
        holder.weekdayText.apply {
            text = weekday
            setTextColor(context.resources.getColor(R.color.secundary))
            textSize = context.resources.getDimension(R.dimen.text_recycler_dialog)
        }
        
        holder.countText.apply {
            val percentage = (count.toFloat() / totalCount * 100).roundToInt()
            text = context.getString(R.string.weekday_count_format, count, percentage)
            setTextColor(context.resources.getColor(R.color.secundary))
            textSize = context.resources.getDimension(R.dimen.text_recycler_dialog)
        }
    }

    override fun getItemCount(): Int = weekdayData.size
}
