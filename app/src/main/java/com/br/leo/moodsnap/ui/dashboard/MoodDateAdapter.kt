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
import java.text.SimpleDateFormat
import java.util.*

class MoodDateAdapter(
    private val context: Context,
    private val dates: List<Date>,
    private val moodColor: Int
) : RecyclerView.Adapter<MoodDateAdapter.DateViewHolder>() {

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.date_text)
        val timeText: TextView = itemView.findViewById(R.id.time_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        
        // Formatar a data
        val calendar = Calendar.getInstance()
        calendar.time = date

        holder.dateText.text = DateUtils.formatDateTime(context, date, false)
        
        // Formatar e exibir a hora no formato AM/PM
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.timeText.text = timeFormat.format(date)
    }

    override fun getItemCount(): Int = dates.size
}
