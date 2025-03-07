package com.br.leo.moodsnap.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
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

class CalendarAdapter(
    private var daysInMonth: Int,
    private var moodList: List<MoodModel> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1
    private var onDayClickListener: ((Int) -> Unit)? = null
    private val today = Calendar.getInstance()
    private val displayMonth = Calendar.getInstance()

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

    override fun onBindViewHolder(holder: CalendarViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val dayOfMonth = position + 1
        holder.dayNumber.text = dayOfMonth.toString()

        // Verificar se é data futura
        val isFutureDate = isDateInFuture(dayOfMonth)
        
        // Configurar aparência para datas futuras
        if (isFutureDate) {
            holder.dayNumber.setTextColor(Color.LTGRAY)
            holder.dayCard.alpha = 0.5f
            holder.dayCard.isClickable = false
        } else {
            holder.dayNumber.setTextColor(Color.BLACK)
            holder.dayCard.alpha = 1.0f
            holder.dayCard.isClickable = true
        }

        // Encontrar o humor para este dia
        val mood = moodList.find { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            calendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth
        }

        // Configurar o indicador de humor
        if (mood != null && !isFutureDate) {
            holder.moodIndicator.visibility = View.VISIBLE
            holder.moodIndicator.setImageResource(getMoodDrawable(mood.moodType))
        } else {
            holder.moodIndicator.visibility = View.GONE
        }

        // Configurar seleção
        val isSelected = dayOfMonth - 1 == selectedPosition && !isFutureDate
        holder.dayCard.isSelected = isSelected
        holder.dayCard.setCardBackgroundColor(
            if (isSelected)
                holder.itemView.context.getColor(R.color.primary_red)
            else
                holder.itemView.context.getColor(android.R.color.white)
        )

        holder.dayCard.setOnClickListener {
            if (!isFutureDate) {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(position)
                onDayClickListener?.invoke(dayOfMonth)
            }
        }
    }

    private fun isDateInFuture(dayOfMonth: Int): Boolean {
        displayMonth.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return displayMonth.after(today)
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

    fun setSelectedDay(dayOfMonth: Int) {
        if (!isDateInFuture(dayOfMonth)) {
            val previousSelected = selectedPosition
            selectedPosition = dayOfMonth - 1
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }

    fun setDisplayMonth(year: Int, month: Int) {
        displayMonth.set(Calendar.YEAR, year)
        displayMonth.set(Calendar.MONTH, month)
    }

    fun setOnDayClickListener(listener: (Int) -> Unit) {
        onDayClickListener = listener
    }
} 