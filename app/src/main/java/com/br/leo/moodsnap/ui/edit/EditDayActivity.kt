package com.br.leo.moodsnap.ui.edit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.br.leo.moodsnap.R
import com.bumptech.glide.Glide
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import com.br.leo.moodsnap.ui.dialog.CustomAlertDialog
import com.br.leo.moodsnap.ui.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton

class EditDayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var moodId: Int = 0
    private var selectedMoodType: Int? = null
    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var calendar: Calendar
    private lateinit var selectedDate: Date

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera necessária para esta função", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permissão de galeria necessária para esta função", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoFile?.let { file ->
                selectedImageUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    file
                )
                loadImage(selectedImageUri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImage(selectedImageUri)
            }
        }
    }

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
        setupMoodSelection()
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
        binding.dateText.setOnClickListener {
            showDatePicker()
        }
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
        binding.dateText.text = dateFormat.format(calendar.time)
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

    private fun setupMoodSelection() {
        // Configurar cliques nos humores
        binding.emotionVeryHappy.setOnClickListener {
            selectedMoodType = 0
            updateMoodSelection()
        }

        binding.emotionHappy.setOnClickListener {
            selectedMoodType = 1
            updateMoodSelection()
        }

        binding.emotionNeutral.setOnClickListener {
            selectedMoodType = 2
            updateMoodSelection()
        }

        binding.emotionSad.setOnClickListener {
            selectedMoodType = 3
            updateMoodSelection()
        }

        binding.emotionVerySad.setOnClickListener {
            selectedMoodType = 4
            updateMoodSelection()
        }
    }

    private fun updateMoodSelection() {
        // Resetar opacidade de todos os humores
        binding.emotionVeryHappy.alpha = 0.5f
        binding.emotionHappy.alpha = 0.5f
        binding.emotionNeutral.alpha = 0.5f
        binding.emotionSad.alpha = 0.5f
        binding.emotionVerySad.alpha = 0.5f

        // Destacar o humor selecionado
        when (selectedMoodType) {
            0 -> binding.emotionVeryHappy.alpha = 1f
            1 -> binding.emotionHappy.alpha = 1f
            2 -> binding.emotionNeutral.alpha = 1f
            3 -> binding.emotionSad.alpha = 1f
            4 -> binding.emotionVerySad.alpha = 1f
        }
    }

    private fun loadExistingData() {
        // Buscar humor para a data atual do calendário
        val mood = repository.getMoodByDate(calendar.time)
        
        // Limpar campos existentes
        binding.editDescription.setText("")
        binding.imageDay.setImageResource(R.drawable.edit_text_border)
        selectedImageUri = null
        selectedMoodType = null
        
        // Se encontrar humor, carregar seus dados
        if (mood != null) {
            binding.editDescription.setText(mood.description)
            selectedMoodType = mood.moodType
            updateMoodSelection()
            
            mood.imagePath?.let { path ->
                val imageFile = File(path)
                if (imageFile.exists()) {
                    Glide.with(this)
                        .load(imageFile)
                        .into(binding.imageDay)
                }
            }
            moodId = mood.id
        } else {
            // Se não encontrar humor, resetar o moodId e a seleção de humor
            moodId = 0
            updateMoodSelection()
        }
    }

    private fun setupListeners() {
        binding.imageDay.setOnClickListener {
            showImageSourceDialog()
        }

        Utils.updateBackGroundColor(applicationContext, binding.btnSave, null)

        binding.btnSave.setOnClickListener {
            if (moodId == 0 && selectedMoodType == null) {
                Toast.makeText(this, "Por favor, selecione um humor para o dia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                selectedMoodType?.let { existingMood.moodType = it }
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
                    this.moodType = selectedMoodType ?: 2 // Neutro como padrão
                    selectedImageUri?.let { uri ->
                        this.imagePath = saveImageToInternalStorage(uri)
                    }
                }
                repository.save(mood)
            }
            
            Toast.makeText(this, "Humor salvo com sucesso!", Toast.LENGTH_SHORT).show()
            returnResult()
            finish()
        }
    }

    private fun showImageSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Verificar se existe imagem para mostrar botão de deletar
        val btnDeleteImage = dialogView.findViewById<MaterialButton>(R.id.btn_delete_image)
        val hasExistingImage = if (moodId > 0) {
            val mood = repository.get(moodId)
            !mood.imagePath.isNullOrEmpty()
        } else false

        btnDeleteImage.visibility = if (hasExistingImage) View.VISIBLE else View.GONE

        dialogView.findViewById<MaterialButton>(R.id.btn_camera)
            .setOnClickListener {
                dialog.dismiss()
                checkCameraPermission()
            }

        dialogView.findViewById<MaterialButton>(R.id.btn_gallery)
            .setOnClickListener {
                dialog.dismiss()
                checkGalleryPermission()
            }

        btnDeleteImage.setOnClickListener {
            dialog.dismiss()
            CustomAlertDialog.create(this)
                .setTitle("Atenção")
                .setMessage("Deseja realmente deletar a imagem?")
                .setPositiveListener {
                    // Deletar a imagem
                    if (moodId > 0) {
                        val mood = repository.get(moodId)
                        mood.imagePath?.let { path ->
                            // Deletar o arquivo
                            File(path).delete()
                            // Limpar o path no modelo
                            mood.imagePath = null
                            repository.update(mood)
                            // Resetar a ImageView
                            binding.imageDay.setImageResource(R.drawable.edit_text_border)
                            selectedImageUri = null
                            Toast.makeText(this, "Imagem deletada com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeListener(null)
                .show()
        }

        dialog.show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog(
                    "Permissão da Câmera",
                    "O acesso à câmera é necessário para tirar fotos.",
                    Manifest.permission.CAMERA
                )
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog(
                    "Permissão da Galeria",
                    "O acesso à galeria é necessário para selecionar imagens.",
                    permission
                )
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showPermissionRationaleDialog(title: String, message: String, permission: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Permitir") { _, _ ->
                when (permission) {
                    Manifest.permission.CAMERA -> cameraPermissionLauncher.launch(permission)
                    else -> galleryPermissionLauncher.launch(permission)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = createImageFile()
        photoFile?.let { file ->
            val photoURI = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureLauncher.launch(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun loadImage(uri: Uri?) {
        uri?.let {
            Glide.with(this)
                .load(it)
                .into(binding.imageDay)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, "mood_image_${System.currentTimeMillis()}.jpg")
        
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        
        inputStream?.close()
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