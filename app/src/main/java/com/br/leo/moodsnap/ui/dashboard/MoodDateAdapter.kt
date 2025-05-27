package com.br.leo.moodsnap.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.DateUtils
import java.util.*

class MoodDateAdapter(
    private val context: Context,
    private val dates: List<Date>,
    private val moodColor: Int
) : RecyclerView.Adapter<MoodDateAdapter.DateViewHolder>() {

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateCardView: CardView = itemView.findViewById(R.id.date_card_view)
        val dateText: TextView = itemView.findViewById(R.id.date_text)
        val weekdayText: TextView = itemView.findViewById(R.id.weekday_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        
        // Configurar a cor do card
        holder.dateCardView.setCardBackgroundColor(moodColor)
        
        // Formatar a data
        val calendar = Calendar.getInstance()
        calendar.time = date
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val monthName = DateUtils.getMonthName(context, month)
        
        holder.dateText.text = "$day $monthName $year"
        
        // Obter o nome do dia da semana
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val weekdays = listOf(
            context.getString(R.string.weekday_full_sunday),
            context.getString(R.string.weekday_full_monday),
            context.getString(R.string.weekday_full_tuesday),
            context.getString(R.string.weekday_full_wednesday),
            context.getString(R.string.weekday_full_thursday),
            context.getString(R.string.weekday_full_friday),
            context.getString(R.string.weekday_full_saturday)
        )
        holder.weekdayText.text = weekdays[dayOfWeek]
    }

    override fun getItemCount(): Int = dates.size
}
