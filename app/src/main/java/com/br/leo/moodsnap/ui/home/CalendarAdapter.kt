package com.br.leo.moodsnap.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.ui.utils.Utils
import java.util.Calendar

class CalendarAdapter(
    private var daysInMonth: Int,
    private var moodList: List<MoodModel> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1
    private var onDayClickListener: ((Int) -> Unit)? = null
    private val today = Calendar.getInstance()
    private val displayMonth = Calendar.getInstance()
    private var firstDayOfWeek = 0 // Domingo = 0, Segunda = 1, etc

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
        // Se a posição for menor que o primeiro dia da semana, é um espaço vazio
        if (position < firstDayOfWeek) {
            holder.dayNumber.text = ""
            holder.dayCard.visibility = View.INVISIBLE
            holder.dayCard.isClickable = false
            return
        }

        val dayOfMonth = position - firstDayOfWeek + 1
        if (dayOfMonth > daysInMonth) {
            holder.dayNumber.text = ""
            holder.dayCard.visibility = View.INVISIBLE
            holder.dayCard.isClickable = false
            return
        }

        holder.dayCard.visibility = View.VISIBLE
        holder.dayNumber.text = dayOfMonth.toString()

        // Verificar se é data futura
        val isFutureDate = isDateInFuture(dayOfMonth)
        
        // Configurar aparência para datas futuras
        if (isFutureDate) {
            holder.dayNumber.setTextColor(Color.LTGRAY)
            holder.dayCard.alpha = 0.5f
            holder.dayCard.isClickable = false
        } else {
            holder.dayNumber.setTextColor(Color.LTGRAY)
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
        val isSelected = (position - firstDayOfWeek) == selectedPosition && !isFutureDate
        holder.dayCard.isSelected = isSelected
        holder.dayCard.setCardBackgroundColor(
            if (isSelected)
                holder.itemView.context.getColor(R.color.primary_green)
            else
                holder.itemView.context.getColor(android.R.color.white)
        )

        holder.dayCard.setOnClickListener {
            if (!isFutureDate) {
                val previousSelected = selectedPosition
                selectedPosition = position - firstDayOfWeek
                notifyItemChanged(previousSelected + firstDayOfWeek)
                notifyItemChanged(position)
                onDayClickListener?.invoke(dayOfMonth)
            } else {
                Utils.run {
                    holder.dayCard.flashError {
                        showCustomToast(holder.itemView.context, "Não é possível registrar humor em datas futuras")
                    }
                }
            }
        }
    }

    private fun isDateInFuture(dayOfMonth: Int): Boolean {
        displayMonth.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return displayMonth.after(today)
    }

    override fun getItemCount() = daysInMonth + firstDayOfWeek

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
            notifyItemChanged(previousSelected + firstDayOfWeek)
            notifyItemChanged(selectedPosition + firstDayOfWeek)
        }
    }

    fun setDisplayMonth(year: Int, month: Int) {
        displayMonth.set(Calendar.YEAR, year)
        displayMonth.set(Calendar.MONTH, month)
        displayMonth.set(Calendar.DAY_OF_MONTH, 1) // Primeiro dia do mês
        firstDayOfWeek = displayMonth.get(Calendar.DAY_OF_WEEK) - 1 // Ajusta para 0-based (Domingo = 0)
    }

    fun setOnDayClickListener(listener: (Int) -> Unit) {
        onDayClickListener = listener
    }
} 