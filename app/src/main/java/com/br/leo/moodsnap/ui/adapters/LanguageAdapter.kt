package com.br.leo.moodsnap.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.model.LanguageModel
import com.br.leo.moodsnap.ui.utils.ButtonUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.Utils

class LanguageAdapter(
    private val context: Context,
    private var languages: List<LanguageModel>,
    private val onLanguageSelected: (LanguageModel) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = -1
    private val currentFont = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        .getString("current_font", "default")

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val languageButton: Button = itemView.findViewById(R.id.btn_language)

        fun bind(language: LanguageModel, position: Int) {
            // Configurar o texto do botão
            languageButton.text = context.getString(language.name)
            
            // Aplicar a fonte atual ao botão
            languageButton.typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))

            // Atualizar o estado visual do botão
            if (position == selectedPosition) {
                ButtonUtils.highlightButton(context, languageButton)
            } else {
                Utils.updateBackGroundColor(context, languageButton, R.color.gray_dark)
            }

            // Configurar o clique
            languageButton.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onLanguageSelected(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_language_button, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position)
    }

    override fun getItemCount(): Int = languages.size

    fun updateLanguages(newLanguages: List<LanguageModel>) {
        languages = newLanguages
        notifyDataSetChanged()
    }

    fun setSelectedLanguage(languageId: String) {
        val position = languages.indexOfFirst { it.id == languageId }
        if (position != -1) {
            selectedPosition = position
            notifyDataSetChanged()
        }
    }
} 