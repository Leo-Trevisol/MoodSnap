package com.br.leo.moodsnap.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.model.MoodModel
import java.util.Calendar
import java.util.Date

class CalendarAdapter(
    private var daysInMonth: Int,
    private var moodList: List<MoodModel> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1
    private var onDayClickListener: ((Int) -> Unit)? = null

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayCard: CardView = view.findViewById(R.id.day_card)
        val dayNumber: TextView = view.findViewById(R.id.day_number)
        val moodIndicator: ImageView = view.findViewById(R.id.mood_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val dayOfMonth = position + 1
        holder.dayNumber.text = dayOfMonth.toString()

        // Encontrar o humor para este dia
        val mood = moodList.find { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            calendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth
        }

        if (mood != null) {
            holder.moodIndicator.visibility = View.VISIBLE
            holder.moodIndicator.setImageResource(getMoodDrawable(mood.moodType))
        } else {
            holder.moodIndicator.visibility = View.GONE
        }

        // Gerenciar seleção
        holder.dayCard.isSelected = position == selectedPosition
        
        holder.dayCard.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == position) -1 else position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onDayClickListener?.invoke(dayOfMonth)
        }
    }

    override fun getItemCount() = daysInMonth

    private fun getMoodDrawable(moodType: Int): Int {
        return when (moodType) {
            0 -> R.drawable.muito_feliz
            1 -> R.drawable.feliz
            2 -> R.drawable.neutro
            3 -> R.drawable.triste
            4 -> R.drawable.muito_triste
            else -> R.drawable.neutro
        }
    }

    fun updateData(newDaysInMonth: Int, newMoodList: List<MoodModel>) {
        daysInMonth = newDaysInMonth
        moodList = newMoodList
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun setOnDayClickListener(listener: (Int) -> Unit) {
        onDayClickListener = listener
    }
} 