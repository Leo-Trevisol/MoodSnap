package com.br.leo.moodsnap.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.Utils
import kotlin.math.roundToInt

class RadarMoodAdapter(
    private val context: Context,
    private val moodData: List<MoodItem>,
    private val totalCount: Int
) : RecyclerView.Adapter<RadarMoodAdapter.MoodViewHolder>() {

    data class MoodItem(
        val moodType: Int,
        val moodName: String,
        val count: Int,
        val color: Int
    )

    class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moodCardView: CardView = itemView.findViewById(R.id.mood_card_view)
        val moodIcon: ImageView = itemView.findViewById(R.id.mood_icon)
        val moodName: TextView = itemView.findViewById(R.id.mood_name)
        val moodCount: TextView = itemView.findViewById(R.id.mood_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_radar_mood, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val moodItem = moodData[position]
        
        val percentage = (moodItem.count.toFloat() / totalCount * 100).roundToInt()
        
        holder.moodCardView.setCardBackgroundColor(moodItem.color)
        holder.moodIcon.setImageResource(Utils.getMoodIcon(moodItem.moodType))
        holder.moodName.text = moodItem.moodName
        holder.moodCount.text = context.getString(R.string.weekday_count_format, moodItem.count, percentage)
    }

    override fun getItemCount(): Int = moodData.size
}
