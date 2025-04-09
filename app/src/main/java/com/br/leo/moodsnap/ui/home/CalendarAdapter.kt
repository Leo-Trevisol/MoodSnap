package com.br.leo.moodsnap.ui.home

import android.content.Context
import android.graphics.Color
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.Utils
import java.util.Calendar

class CalendarAdapter(
    private var daysInMonth: Int,
    private val context: Context,
    private var moodList: List<MoodModel> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1
    private var onDayClickListener: ((Int) -> Unit)? = null
    private val today = Calendar.getInstance()
    private val displayMonth = Calendar.getInstance()
    private var firstDayOfWeek = 0 // Domingo = 0, Segunda = 1, etc

    // Cores para cada tipo de humor
    private val moodColors = mapOf(
        0 to R.color.very_happy_color,
        1 to R.color.happy_color,
        2 to R.color.neutral_color,
        3 to R.color.sad_color,
        4 to R.color.very_sad_color
    )

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

        FontUtils.applyFontToView(context, holder.dayNumber)

        // Resetar o background do TextView para garantir que não mantenha estados anteriores
        holder.dayNumber.setBackgroundResource(0)
        holder.dayNumber.setTextColor(context.getColor(R.color.day_text_color))

        // Verificar se é data futura
        val isFutureDate = isDateInFuture(dayOfMonth)

        // Verificar se é o dia atual
        val isToday = isToday(dayOfMonth)

        // Encontrar o humor para este dia
        val mood = moodList.find { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            calendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth
        }

        // Configurar seleção
        val isSelected = (position - firstDayOfWeek) == selectedPosition && !isFutureDate

        // Configurar aparência para datas futuras e dia atual
        when {
            isFutureDate -> {
                holder.dayNumber.setTextColor(context.getColor(R.color.day_text_color))
                holder.dayCard.alpha = 0.5f
                holder.dayCard.isClickable = false
                holder.dayCard.setCardBackgroundColor(context.getColor(R.color.future_day_background_color))
                holder.moodIndicator.visibility = View.GONE
            }
            isToday -> {
                holder.dayCard.alpha = 1.0f
                holder.dayCard.isClickable = true
                holder.dayNumber.setTextColor(context.getColor(R.color.secundary))

                if (mood != null) {
                    holder.dayCard.setCardBackgroundColor(
                        if (isSelected) context.getColor(R.color.primary_green)
                        else context.getColor(moodColors[mood.moodType] ?: R.color.white)
                    )
                } else {
                    holder.dayCard.setCardBackgroundColor(
                        if (isSelected) context.getColor(R.color.primary_green)
                        else context.getColor(R.color.past_day_background_color)
                    )
                }
                holder.moodIndicator.visibility = if (mood != null) View.VISIBLE else View.GONE
                if (mood != null) holder.moodIndicator.setImageResource(getMoodDrawable(mood.moodType))
            }
            else -> {
                holder.dayNumber.setTextColor(context.getColor(R.color.day_text_color))
                holder.dayCard.alpha = 1.0f
                holder.dayCard.isClickable = true

                if (mood != null) {
                    holder.dayCard.setCardBackgroundColor(
                        if (isSelected) context.getColor(R.color.primary_green)
                        else context.getColor(moodColors[mood.moodType] ?: R.color.white)
                    )
                    holder.moodIndicator.visibility = View.VISIBLE
                    holder.moodIndicator.setImageResource(getMoodDrawable(mood.moodType))
                    holder.dayNumber.setTextColor(if (isSelected) Color.WHITE else context.getColor(R.color.day_text_color))
                } else {
                    holder.dayCard.setCardBackgroundColor(
                        if (isSelected) context.getColor(R.color.primary_green)
                        else context.getColor(R.color.past_day_background_color)
                    )
                    holder.moodIndicator.visibility = View.GONE
                    holder.dayNumber.setTextColor(if (isSelected) Color.WHITE else context.getColor(R.color.day_text_color))
                }
            }
        }

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
                        showCustomToast(context, context.getString(R.string.future_date_not_allowed))
                    }
                }
            }
        }
    }

    private fun isDateInFuture(dayOfMonth: Int): Boolean {
        displayMonth.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return displayMonth.after(today)
    }

    private fun isToday(dayOfMonth: Int): Boolean {
        return today.get(Calendar.YEAR) == displayMonth.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == displayMonth.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == dayOfMonth
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
        if (!isDateInFuture(dayOfMonth) && dayOfMonth > 0) {
            val previousSelected = selectedPosition
            selectedPosition = dayOfMonth - 1
            notifyItemChanged(previousSelected + firstDayOfWeek)
            notifyItemChanged(selectedPosition + firstDayOfWeek)
        }
    }

    fun setDisplayMonth(year: Int, month: Int) {
        displayMonth.set(Calendar.YEAR, year)
        displayMonth.set(Calendar.MONTH, month)
        displayMonth.set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfWeek = displayMonth.get(Calendar.DAY_OF_WEEK) - 1
        notifyDataSetChanged()
    }

    fun setOnDayClickListener(listener: (Int) -> Unit) {
        onDayClickListener = listener
    }
} 