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
import com.br.leo.moodsnap.ui.model.LanguageModel
import com.br.leo.moodsnap.ui.utils.FontUtils

class LanguageAdapter(
    private val context: Context,
    private var languages: List<LanguageModel>,
    private val onLanguageSelected: (LanguageModel) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = -1
    private var selectedLanguageId: String? = null

    // Cache para melhor performance
    private val typefaceCache = mutableMapOf<String, androidx.core.content.res.ResourcesCompat.FontCallback?>()
    private val currentFont = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        .getString("current_font", "default")

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textLanguage: TextView = itemView.findViewById(R.id.text_language)
        private val rootLayout: View = itemView
        private var currentLanguage: LanguageModel? = null

        init {
            // Pré-carregar fonte para melhor performance
            val typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
            textLanguage.typeface = typeface

            // Usar setOnClickListener direto SEM debounce para resposta instantânea
            rootLayout.setOnClickListener {
                currentLanguage?.let { language ->
                    onItemClick(language)
                }
            }
        }

        fun bind(language: LanguageModel) {
            currentLanguage = language

            // Configurar o texto do idioma
            textLanguage.text = context.getString(language.name)

            // Atualizar o estado visual do item
            val isSelected = language.id == selectedLanguageId ||
                    adapterPosition == selectedPosition

            updateVisualState(isSelected)
        }

        private fun updateVisualState(isSelected: Boolean) {
            if (isSelected) {
                rootLayout.setBackgroundResource(R.drawable.background_rounded_left)
                rootLayout.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.primary_green)
                )
                textLanguage.setTextColor(ContextCompat.getColor(context, R.color.primary))

                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_check)
                drawable?.setTint(ContextCompat.getColor(context, R.color.secundary))
                textLanguage.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            } else {
                rootLayout.setBackgroundResource(android.R.color.transparent)
                textLanguage.setTextColor(ContextCompat.getColor(context, R.color.secundary))
                textLanguage.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
        }

        private fun onItemClick(language: LanguageModel) {
            // Encontrar a posição atual do item
            val newPosition = languages.indexOfFirst { it.id == language.id }
            if (newPosition == -1) return

            // Se já está selecionado, não fazer nada
            if (selectedLanguageId == language.id) return

            // Salvar posições anteriores
            val oldSelectedPosition = selectedPosition
            val oldSelectedLanguageId = selectedLanguageId

            // Atualizar seleção
            selectedPosition = newPosition
            selectedLanguageId = language.id

            // Atualizar visualização de forma otimizada
            if (oldSelectedPosition != -1 && oldSelectedPosition != newPosition) {
                // Encontrar o ViewHolder do item anteriormente selecionado
                val oldViewHolder = findViewHolderForPosition(oldSelectedPosition)
                oldViewHolder?.updateVisualState(false)
            }

            // Atualizar visualização do novo item selecionado
            updateVisualState(true)

            // Notificar callback
            onLanguageSelected(language)
        }

        private fun findViewHolderForPosition(position: Int): LanguageViewHolder? {
            // Tenta encontrar o ViewHolder na posição especificada
            return try {
                val holder = itemView.parent?.let { parent ->
                    (parent as? RecyclerView)?.findViewHolderForAdapterPosition(position)
                } as? LanguageViewHolder
                holder
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_language_button, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
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
            selectedLanguageId = languageId
            // Em vez de notifyDataSetChanged(), atualize apenas o item necessário
            notifyItemChanged(position)
        }
    }

    fun getSelectedLanguage(): String? = selectedLanguageId

    // Método para limpar seleção
    fun clearSelection() {
        val oldPosition = selectedPosition
        selectedPosition = -1
        selectedLanguageId = null
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
    }
}