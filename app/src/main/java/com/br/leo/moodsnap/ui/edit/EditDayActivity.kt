package com.br.leo.moodsnap.ui.edit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.R
import com.bumptech.glide.Glide
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditDayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var moodId: Int = 0
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var calendar: Calendar
    private lateinit var selectedDate: Date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MoodRepository(this)
        moodId = intent.getIntExtra("mood_id", 0)
        
        // Inicializar o calendar com a data recebida ou data atual
        calendar = Calendar.getInstance()
        val selectedDateMillis = intent.getLongExtra("selected_date", -1L)
        if (selectedDateMillis != -1L) {
            calendar.timeInMillis = selectedDateMillis
        }
        selectedDate = calendar.time

        // Carregar dados do humor se existir
        loadExistingData()
        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        // Configurar a data atual
        updateDateText()

        // Configurar botão de voltar
        binding.btnBack.setOnClickListener {
            returnResult()
            finish()
        }

        // Configurar navegação entre dias
        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDateText()
            loadExistingData()
        }

        binding.btnNextMonth.setOnClickListener {
            // Não permitir navegar para dias futuros
            val nextDay = calendar.clone() as Calendar
            nextDay.add(Calendar.DAY_OF_MONTH, 1)
            if (!isDateInFuture(nextDay)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                updateDateText()
                loadExistingData()
            }
        }

        // Configurar clique na data
        binding.txtCurrentDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
        binding.txtCurrentDate.text = dateFormat.format(calendar.time)
    }

    private fun showDatePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.month_picker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.year_picker)
        
        // Configurar o picker de meses
        val months = (0..11).map { month ->
            val tempCalendar = Calendar.getInstance()
            tempCalendar.set(Calendar.MONTH, month)
            tempCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
        }.toTypedArray()

        monthPicker.apply {
            minValue = 0
            maxValue = 11
            displayedValues = months
            value = calendar.get(Calendar.MONTH)
        }

        // Configurar o picker de anos
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.apply {
            minValue = currentYear - 10
            maxValue = currentYear
            value = calendar.get(Calendar.YEAR)
        }

        MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            .setTitle("Selecione a Data")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("OK") { _, _ ->
                calendar.set(Calendar.YEAR, yearPicker.value)
                calendar.set(Calendar.MONTH, monthPicker.value)
                updateDateText()
                
                // Carregar dados do novo dia selecionado
                loadExistingData()
            }
            .show()
    }

    private fun loadExistingData() {
        // Buscar humor para a data atual do calendário
        val mood = repository.getMoodByDate(calendar.time)
        
        // Limpar campos existentes
        binding.editDescription.setText("")
        binding.imageDay.setImageResource(R.drawable.edit_text_border)
        selectedImageUri = null
        
        // Se encontrar humor, carregar seus dados
        if (mood != null) {
            binding.editDescription.setText(mood.description)
            moodId = mood.id
            
            mood.imagePath?.let { path ->
                val imageFile = File(path)
                if (imageFile.exists()) {
                    Glide.with(this)
                        .load(imageFile)
                        .into(binding.imageDay)
                }
            }
        } else {
            // Se não encontrar humor, resetar o moodId
            moodId = 0
        }
    }

    private fun setupListeners() {
        binding.imageDay.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnSave.setOnClickListener {
            val description = binding.editDescription.text.toString()
            
            // Buscar o humor para a data atual
            val existingMood = if (moodId > 0) {
                repository.get(moodId)
            } else {
                null
            }
            
            if (existingMood != null) {
                // Atualizar humor existente
                existingMood.description = description
                selectedImageUri?.let { uri ->
                    val imagePath = saveImageToInternalStorage(uri)
                    existingMood.imagePath = imagePath
                }
                repository.update(existingMood)
            } else {
                // Criar novo humor
                val mood = MoodModel().apply {
                    this.date = calendar.time
                    this.description = description
                    selectedImageUri?.let { uri ->
                        this.imagePath = saveImageToInternalStorage(uri)
                    }
                }
                repository.save(mood)
            }
            
            Toast.makeText(this, "Descrição salva com sucesso!", Toast.LENGTH_SHORT).show()
            returnResult()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.imageDay)
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "mood_image_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        
        return file.absolutePath
    }

    private fun isDateInFuture(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.after(today)
    }

    private fun returnResult() {
        val resultIntent = Intent()
        resultIntent.putExtra("current_date", calendar.timeInMillis)
        resultIntent.putExtra("current_month", calendar.get(Calendar.MONTH))
        resultIntent.putExtra("current_year", calendar.get(Calendar.YEAR))
        setResult(Activity.RESULT_OK, resultIntent)
    }

    override fun onBackPressed() {
        returnResult()
        super.onBackPressed()
    }
} 