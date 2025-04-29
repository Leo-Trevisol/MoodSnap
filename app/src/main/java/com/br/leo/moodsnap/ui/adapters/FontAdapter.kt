package com.br.leo.moodsnap.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.model.FontModel
import com.br.leo.moodsnap.ui.utils.ButtonUtils
import com.br.leo.moodsnap.ui.utils.Utils

class FontAdapter(
    private val context: Context,
    private var fonts: List<FontModel>,
    private val onFontSelected: (FontModel) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedPosition = -1
    private var lastSelectedButton: Button? = null
    private var currentSelectedButton: Button? = null

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fontButton: Button = itemView.findViewById(R.id.btn_font)

        fun bind(font: FontModel, position: Int) {
            // Configurar o texto do botão
            fontButton.text = font.name
            
            // Aplicar a fonte ao próprio botão
            fontButton.typeface = ResourcesCompat.getFont(context, font.resourceId)

            // Atualizar o estado visual do botão
            if (position == selectedPosition) {
                ButtonUtils.highlightButton(context, fontButton)
                currentSelectedButton = fontButton
            } else {
                Utils.updateBackGroundColor(context, fontButton, R.color.gray_dark)
            }

            // Configurar o clique
            fontButton.setOnClickListener {
                // Atualizar o estado visual dos botões manualmente
                if (lastSelectedButton != fontButton) {
                    // Desmarcar o botão anteriormente selecionado
                    lastSelectedButton?.let { button ->
                        Utils.updateBackGroundColor(context, button, R.color.gray_dark)
                    }
                    
                    // Marcar o novo botão selecionado
                    ButtonUtils.highlightButton(context, fontButton)
                    
                    // Atualizar as referências
                    lastSelectedButton = fontButton
                    selectedPosition = position
                    
                    // Notificar o callback
                    onFontSelected(font)
                }
            }
            
            // Manter referência ao botão selecionado
            if (position == selectedPosition) {
                lastSelectedButton = fontButton
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