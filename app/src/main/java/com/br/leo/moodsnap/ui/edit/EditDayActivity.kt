package com.br.leo.moodsnap.ui.edit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
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
import com.br.leo.moodsnap.ui.utils.Utils.showCustomToast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import android.content.Context
import com.br.leo.moodsnap.ui.utils.FontManager

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
    private var hasChanges = false
    private var originalMood: MoodModel? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showCustomToast(this, getString(R.string.camera_permission_required))
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            showCustomToast(this, getString(R.string.gallery_permission_required))
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
        overridePendingTransition(R.anim.dialog_enter, R.anim.dialog_exit)

        // Apply current font
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        FontManager.applyFontToActivity(this, currentFont ?: "default")

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
            if (hasChanges) {
                showDiscardChangesDialog { returnResult(); finish() }
            } else {
                returnResult()
                finish()
            }
        }

        // Configurar navegação entre dias
        binding.btnPreviousMonth.setOnClickListener {
            if (hasChanges) {
                showDiscardChangesDialog {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    updateDateText()
                    loadExistingData()
                    hasChanges = false
                }
            } else {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                updateDateText()
                loadExistingData()
            }
        }

        binding.btnNextMonth.setOnClickListener {
            // Não permitir navegar para dias futuros
            val nextDay = calendar.clone() as Calendar
            nextDay.add(Calendar.DAY_OF_MONTH, 1)
            if (!Utils.isDateInFuture(nextDay)) {
                if (hasChanges) {
                    showDiscardChangesDialog {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        updateDateText()
                        loadExistingData()
                        hasChanges = false
                    }
                } else {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    updateDateText()
                    loadExistingData()
                }
            }
        }

        // Configurar clique na data
        binding.dateText.setOnClickListener {
            showDatePicker()
        }
    }

    private fun updateDateText() {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = Utils.getMonthName(this, calendar.get(Calendar.MONTH))
        binding.dateText.text = "$day - $month"
    }

    private fun showDatePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.month_picker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.year_picker)
        
        // Apply current font to dialog view
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        FontManager.applyFontToView(this, dialogView, currentFont ?: "default")
        
        // Configurar o picker de meses
        val months = arrayOf(
            getString(R.string.month_january),
            getString(R.string.month_february),
            getString(R.string.month_march),
            getString(R.string.month_april),
            getString(R.string.month_may),
            getString(R.string.month_june),
            getString(R.string.month_july),
            getString(R.string.month_august),
            getString(R.string.month_september),
            getString(R.string.month_october),
            getString(R.string.month_november),
            getString(R.string.month_december)
        )

        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        monthPicker.apply {
            minValue = 0
            maxValue = 11
            displayedValues = months
            value = calendar.get(Calendar.MONTH)
        }

        // Configurar o picker de anos
        yearPicker.apply {
            minValue = currentYear - 10
            maxValue = currentYear
            value = calendar.get(Calendar.YEAR)
        }

        // Adicionar listener para controlar a seleção de meses futuros
        yearPicker.setOnValueChangedListener { _, _, newVal ->
            if (newVal == currentYear) {
                monthPicker.maxValue = currentMonth
                if (monthPicker.value > currentMonth) {
                    monthPicker.value = currentMonth
                }
            } else {
                monthPicker.maxValue = 11
            }
        }

        // Verificar se a data selecionada é futura
        val selectedYear = yearPicker.value
        val selectedMonth = monthPicker.value
        if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            yearPicker.value = currentYear
            monthPicker.value = currentMonth
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.hint_date))
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                val selectedYear = yearPicker.value
                val selectedMonth = monthPicker.value

                if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
                    showCustomToast(this, getString(R.string.error_invalid_date))
                    return@setPositiveButton
                }

                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                updateDateText()
                loadExistingData()
            }
            .create()

        dialog.window?.setWindowAnimations(R.style.DialogAnimation)
        dialog.show()
    }

    private fun setupMoodSelection() {
        // Configurar cliques nos humores
        binding.emotionVeryHappy.setOnClickListener {
            selectedMoodType = 0
            updateMoodSelection()
            checkForChanges()
        }

        binding.emotionHappy.setOnClickListener {
            selectedMoodType = 1
            updateMoodSelection()
            checkForChanges()
        }

        binding.emotionNeutral.setOnClickListener {
            selectedMoodType = 2
            updateMoodSelection()
            checkForChanges()
        }

        binding.emotionSad.setOnClickListener {
            selectedMoodType = 3
            updateMoodSelection()
            checkForChanges()
        }

        binding.emotionVerySad.setOnClickListener {
            selectedMoodType = 4
            updateMoodSelection()
            checkForChanges()
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
        binding.imageDay.setImageResource(R.drawable.addimage_white)
        binding.imageDay.scaleType = ImageView.ScaleType.CENTER
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
                    binding.imageDay.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
            moodId = mood.id
            originalMood = mood.copy()
        } else {
            // Se não encontrar humor, resetar o moodId e a seleção de humor
            moodId = 0
            updateMoodSelection()
            originalMood = null
        }
        hasChanges = false
        updateMoodQuestionText()
    }

    private fun setupListeners() {
        binding.imageDay.setOnClickListener {
            showImageSourceDialog()
        }

        Utils.updateBackGroundColor(applicationContext, binding.btnSave)

        // Adicionar listener para detectar mudanças na descrição
        binding.editDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                checkForChanges()
            }
        })

        binding.btnSave.setOnClickListener {
            if (moodId == 0 && selectedMoodType == null) {
                showCustomToast(this, getString(R.string.select_mood))
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
            
            hasChanges = false
            showCustomToast(this, getString(R.string.mood_saved))
            returnResult()
            finish()
        }
    }

    private fun showImageSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Aplica a animação de entrada e saída
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Aplica a fonte atual
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        FontManager.applyFontToView(this, dialogView, currentFont ?: "default")

        // Verificar se existe imagem para mostrar botão de deletar
        val btnDeleteImage = dialogView.findViewById<MaterialButton>(R.id.btn_delete_image)
        Utils.updateBackGroundColor(applicationContext, btnDeleteImage, R.color.primary_red)
        val hasExistingImage = if (moodId > 0) {
            val mood = repository.get(moodId)
            !mood.imagePath.isNullOrEmpty()
        } else false

        btnDeleteImage.visibility = if (hasExistingImage) View.VISIBLE else View.GONE

        val btnCamera: Button = dialogView.findViewById<MaterialButton>(R.id.btn_camera)
        Utils.updateBackGroundColor(applicationContext, btnCamera)
        btnCamera.setOnClickListener {
            dialog.dismiss()
            checkCameraPermission()
        }

        val btnGallery: Button = dialogView.findViewById<MaterialButton>(R.id.btn_gallery)
        Utils.updateBackGroundColor(applicationContext, btnGallery)
        btnGallery.setOnClickListener {
            dialog.dismiss()
            checkGalleryPermission()
        }

        btnDeleteImage.setOnClickListener {
            dialog.dismiss()
            CustomAlertDialog.create(this)
                .setTitle(getString(R.string.attention_dialog))
                .setMessage(getString(R.string.confirm_delete_image))
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
                            binding.imageDay.setImageResource(R.drawable.addimage_white)
                            binding.imageDay.scaleType = ImageView.ScaleType.CENTER
                            selectedImageUri = null
                            checkForChanges()
                            showCustomToast(this, getString(R.string.image_deleted))
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
            binding.imageDay.scaleType = ImageView.ScaleType.CENTER_CROP
            checkForChanges()
        } ?: run {
            binding.imageDay.setImageResource(R.drawable.addimage_white)
            binding.imageDay.scaleType = ImageView.ScaleType.CENTER
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

    private fun returnResult() {
        val resultIntent = Intent()
        resultIntent.putExtra("current_date", calendar.timeInMillis)
        resultIntent.putExtra("current_month", calendar.get(Calendar.MONTH))
        resultIntent.putExtra("current_year", calendar.get(Calendar.YEAR))
        setResult(Activity.RESULT_OK, resultIntent)
    }

    private fun showDiscardChangesDialog(onConfirm: () -> Unit) {
        CustomAlertDialog.create(this)
            .setTitle(getString(R.string.unsaved_changes_title))
            .setMessage(getString(R.string.unsaved_changes_message))
            .setPositiveListener {
                onConfirm()
            }
            .setNegativeListener(null)
            .show()
    }

    private fun checkForChanges() {
        val currentDescription = binding.editDescription.text.toString()
        val currentImageUri = selectedImageUri
        
        hasChanges = when {
            originalMood == null -> {
                // Se não havia humor salvo, verifica se adicionou algo
                currentDescription.isNotEmpty() || currentImageUri != null || selectedMoodType != null
            }
            else -> {
                // Se havia humor salvo, verifica se algo mudou
                currentDescription != originalMood?.description ||
                currentImageUri != null ||
                selectedMoodType != originalMood?.moodType
            }
        }
    }

    private fun updateMoodQuestionText() {
        val today = Calendar.getInstance()
        val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                     calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                     calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
        
        binding.tvMoodQuestion.text = if (isToday) {
            getString(R.string.how_are_you_feeling_today)
        } else {
            getString(R.string.how_were_you_feeling_that_day)
        }
    }

    override fun onBackPressed() {
        if (hasChanges) {
            showDiscardChangesDialog { 
                returnResult()
                super.onBackPressed()
            }
        } else {
            returnResult()
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overridePendingTransition(R.anim.dialog_exit, R.anim.dialog_enter)
    }
}