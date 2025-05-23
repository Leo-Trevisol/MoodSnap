package com.br.leo.moodsnap.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.model.FontModel
import com.br.leo.moodsnap.ui.utils.ClickUtils

class FontAdapter(
    private val context: Context,
    private var fonts: List<FontModel>,
    private val onFontSelected: (FontModel) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedPosition = -1

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textFont: TextView = itemView.findViewById(R.id.text_font)
        private val rootLayout: View = itemView

        fun bind(font: FontModel, position: Int) {
            // Configurar o texto da fonte
            textFont.text = font.name

            // Aplicar a fonte atual ao texto
            textFont.typeface = ResourcesCompat.getFont(context, font.resourceId)

            if (position == selectedPosition) {
                rootLayout.setBackgroundResource(R.drawable.background_rounded_left)
                rootLayout.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.primary_green)
                )
                textFont.setTextColor(ContextCompat.getColor(context, R.color.primary))

                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_check)
                drawable?.setTint(ContextCompat.getColor(context, R.color.secundary))
                textFont.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            } else {
                rootLayout.setBackgroundResource(android.R.color.transparent)
                textFont.setTextColor(ContextCompat.getColor(context, R.color.secundary))

                textFont.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            // Configurar o clique
            ClickUtils.setDebounceClickListener(rootLayout){
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onFontSelected(font)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_font_button, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position], position)
    }

    override fun getItemCount(): Int = fonts.size

    fun updateFonts(newFonts: List<FontModel>) {
        fonts = newFonts
        notifyDataSetChanged()
    }

    fun setSelectedFont(fontId: String) {
        val position = fonts.indexOfFirst { it.id == fontId }
        if (position != -1) {
            selectedPosition = position
            notifyDataSetChanged()
        }
    }
}