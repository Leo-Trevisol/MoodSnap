package com.br.leo.moodsnap.ui.edit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.repository.MoodRepository
import android.graphics.Bitmap
import android.view.View
import java.util.*
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.ClickUtils
import android.app.DatePickerDialog
import android.widget.DatePicker
import java.text.SimpleDateFormat

class EditDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var moodId: Int = 0
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setTheme(R.style.DialogRounded)
        binding = ActivityEditDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialmente, mostra o placeholder
        binding.placeholderContainer.visibility = View.VISIBLE
        binding.imageDay.visibility = View.GONE
        
        // Configurar a data inicial
        updateDateText()
        
        // Configurar os listeners para navegação de data
        setupDateNavigation()
    }
    
    private fun setupDateNavigation() {
        // Botão para voltar para o mês anterior
        ClickUtils.setDebounceClickListener(binding.btnPreviousMonth) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDateText()
        }
        
        // Botão para avançar para o próximo mês
        ClickUtils.setDebounceClickListener(binding.btnNextMonth) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            updateDateText()
        }
        
        // Clicar na data para abrir seletor de data
        ClickUtils.setDebounceClickListener(binding.dateText) {
            showDatePicker()
        }
        
        // Botão de voltar
        ClickUtils.setDebounceClickListener(binding.btnBack) {
            finish()
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun updateDateText() {
        // Obter o dia da semana
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayOfWeek = dayOfWeekFormat.format(calendar.time).lowercase()
        
        // Obter o dia do mês
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Obter o nome do mês
        val month = DateUtils.getMonthNameShort(this, calendar.get(Calendar.MONTH))
        
        // Formatar a data no formato "sexta-feira, 21 maio"
        binding.dateText.text = "$dayOfWeek, $day $month"
    }

    // Método para ser chamado quando uma imagem for selecionada
    private fun updateImageView(bitmap: Bitmap) {
        binding.imageDay.setImageBitmap(bitmap)
        binding.imageDay.visibility = View.VISIBLE
        binding.placeholderContainer.visibility = View.GONE
    }

    // Método para limpar a imagem
    private fun clearImage() {
        binding.imageDay.setImageBitmap(null)
        binding.imageDay.visibility = View.GONE
        binding.placeholderContainer.visibility = View.VISIBLE
    }
}