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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.TextView
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.FontUtils.updateFontDialogPicker
import com.br.leo.moodsnap.ui.utils.Utils.findViewsByType
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Spinner
import androidx.core.widget.ImageViewCompat
import com.br.leo.moodsnap.ui.dialog.ImageSourceBottomSheet
import com.br.leo.moodsnap.ui.utils.ClickUtils

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
    private var imageSourceDialog: AlertDialog? = null
    
    // Flags para controlar se as permissões já foram solicitadas
    private var cameraPermissionRequested = false
    private var galleryPermissionRequested = false

    // Determinar a permissão de armazenamento correta com base na versão do Android
    private val storagePermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionRequested = true
        if (isGranted) {
            openCamera()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // Usuário negou permanentemente
                showSettingsPermissionDialog(
                    getString(R.string.camera_permission_denied_permanently)
                )
            } else {
                showCustomToast(this, getString(R.string.camera_permission_required))
            }
        }
    }

    private val requestGalleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        galleryPermissionRequested = true
        if (isGranted) {
            openGallery()
        } else {
            if (!shouldShowRequestPermissionRationale(storagePermission)) {
                // Usuário negou permanentemente
                showSettingsPermissionDialog(
                    getString(R.string.gallery_permission_denied_permanently)
                )
            } else {
                showCustomToast(this, getString(R.string.gallery_permission_required))
            }
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

        // Apply font
        FontUtils.applyFontToActivity(this)

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
        ClickUtils.setDebounceClickListener(binding.btnBack){
            if (hasChanges) {
                showDiscardChangesDialog { returnResult(); finish() }
            } else {
                returnResult()
                finish()
            }
        }

        // Configurar navegação entre dias
        binding.btnPreviousMonth.setOnClickListener({

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
        })

        binding.btnNextMonth.setOnClickListener({
            // Não permitir navegar para dias futuros
            val nextDay = calendar.clone() as Calendar
            nextDay.add(Calendar.DAY_OF_MONTH, 1)
            if (!DateUtils.isDateInFuture(nextDay)) {
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
        })

        // Configurar clique na data
        ClickUtils.setDebounceClickListener(binding.dateText){
            showCalendarPicker()
        }
    }

    private fun updateDateText() {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = DateUtils.getMonthNameShort(this, calendar.get(Calendar.MONTH))
        binding.dateText.text = "$day - $month"
    }

    private fun showCalendarPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        FontUtils.applyFontToView(this, dialogView)

        // Referências aos elementos do layout
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val monthYearSpinner = dialogView.findViewById<Spinner>(R.id.month_year_spinner)
        val prevMonthButton = dialogView.findViewById<ImageButton>(R.id.prev_month_button)
        val nextMonthButton = dialogView.findViewById<ImageButton>(R.id.next_month_button)
        val calendarGrid = dialogView.findViewById<GridLayout>(R.id.calendar_grid)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)

        Utils.updateBackGroundColor(this, btnCancel, backgroundColor = R.color.gray_dark)
        Utils.updateBackGroundColor(this, btnOk)

        val primaryGreen = ContextCompat.getColor(this, R.color.primary_green)

        ImageViewCompat.setImageTintList(prevMonthButton, ColorStateList.valueOf(primaryGreen))
        ImageViewCompat.setImageTintList(nextMonthButton, ColorStateList.valueOf(primaryGreen))

        // Calendário para controlar a data exibida no diálogo
        val dialogCalendar = calendar.clone() as Calendar
        
        // Calendário para controlar a data atual (para limitar a seleção de datas futuras)
        val currentCalendar = Calendar.getInstance()
        
        // Dia selecionado inicialmente (dia atual do calendário)
        var selectedDay = dialogCalendar.get(Calendar.DAY_OF_MONTH)
        
        // Preparar lista de meses/anos para o spinner
        val monthYearList = ArrayList<String>()
        val monthNames = arrayOf(
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
        
        // Criar lista de meses/anos para os últimos 10 anos
        val currentYear = currentCalendar.get(Calendar.YEAR)
        for (year in currentYear - 10..currentYear) {
            for (month in 0..11) {
                // Não incluir meses futuros
                if (year == currentYear && month > currentCalendar.get(Calendar.MONTH)) {
                    continue
                }
                monthYearList.add(getString(R.string.month_year_format, monthNames[month], year))
            }
        }
        
        // Configurar o adaptador do spinner
        val adapter = ArrayAdapter(this, R.layout.spinner_item, monthYearList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        monthYearSpinner.adapter = adapter
        
        // Definir a posição inicial do spinner para o mês/ano atual
        val currentMonthYear = getString(
            R.string.month_year_format,
            monthNames[dialogCalendar.get(Calendar.MONTH)],
            dialogCalendar.get(Calendar.YEAR)
        )
        val spinnerPosition = monthYearList.indexOf(currentMonthYear)
        if (spinnerPosition != -1) {
            monthYearSpinner.setSelection(spinnerPosition)
        }
        
        // Função para atualizar o grid do calendário
        fun updateCalendarGrid() {
            // Limpar o grid atual
            calendarGrid.removeAllViews()
            
            // Configurar o calendário para o primeiro dia do mês
            val tempCalendar = dialogCalendar.clone() as Calendar
            tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
            
            // Obter o dia da semana do primeiro dia do mês (0 = Domingo, 1 = Segunda, etc.)
            val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1
            
            // Obter o número de dias no mês atual
            val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            // Criar células vazias para os dias antes do primeiro dia do mês
            for (i in 0 until firstDayOfWeek) {
                val emptyCell = TextView(this)
                val params = GridLayout.LayoutParams()
                params.width = 0
                //params.height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                params.columnSpec = GridLayout.spec(i, 1f)
                params.setMargins(0, 0, 0, 0)
                emptyCell.layoutParams = params
                calendarGrid.addView(emptyCell)
            }

            // Criar células para cada dia do mês
            for (day in 1..daysInMonth) {
                val dayCell = TextView(this)
                dayCell.text = day.toString()
                dayCell.gravity = Gravity.CENTER
                dayCell.textSize = 16f

                // Calcular a posição da célula no grid
                val position = firstDayOfWeek + day - 1
                val row = position / 7
                val col = position % 7

                val params = GridLayout.LayoutParams()
                params.width = 0
                params.height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                params.rowSpec = GridLayout.spec(row)
                params.columnSpec = GridLayout.spec(col, 1f)
                params.setMargins(2, 0, 2, 0)
                dayCell.layoutParams = params

                // Verificar se este dia é o dia selecionado
                if (day == selectedDay) {
                    dayCell.setBackgroundResource(R.drawable.calendar_selected_day_background)
                    dayCell.setTextColor(resources.getColor(android.R.color.white))
                } else {
                    // Verificar se este dia está no futuro
                    val dayCalendar = dialogCalendar.clone() as Calendar
                    dayCalendar.set(Calendar.DAY_OF_MONTH, day)

                    if (DateUtils.isDateInFuture(dayCalendar)) {
                        // Dia futuro - desabilitar
                        dayCell.setTextColor(resources.getColor(R.color.gray_dark))
                      //  dayCell.alpha = 0.5f
                    } else {
                        // Dia normal
                       //
                        // dayCell.setBackgroundResource(R.drawable.calendar_day_background)
                        dayCell.setTextColor(resources.getColor(R.color.secundary))

                        // Configurar clique para selecionar o dia
                        ClickUtils.setDebounceClickListener(dayCell){
                            // Atualizar a seleção
                            selectedDay = day
                            updateCalendarGrid()
                        }
                    }
                }
                
                calendarGrid.addView(dayCell)
            }
        }
        
        // Atualizar o grid do calendário inicialmente
        updateCalendarGrid()
        
        // Configurar listener do spinner
        monthYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = monthYearList[position]
                
                // Extrair o mês e o ano da string selecionada
                // O formato pode variar dependendo do idioma (por exemplo, "dezembro de 2025" em português)
                val monthName = monthNames.firstOrNull { monthName -> selectedItem.contains(monthName) }
                val yearStr = selectedItem.replace(monthName ?: "", "").trim()
                                         .replace("de", "").trim() // Remove "de" para português/espanhol
                val year = yearStr.toInt()
                
                // Encontrar o índice do mês selecionado
                val monthIndex = monthNames.indexOf(monthName)
                
                // Atualizar o calendário do diálogo
                dialogCalendar.set(Calendar.YEAR, year)
                dialogCalendar.set(Calendar.MONTH, monthIndex)
                
                // Ajustar o dia selecionado se necessário (para evitar dias inválidos)
                val maxDay = dialogCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (selectedDay > maxDay) {
                    selectedDay = maxDay
                }
                
                // Atualizar o grid do calendário
                updateCalendarGrid()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Não fazer nada
            }
        }
        
        // Configurar botões de navegação
        ClickUtils.setDebounceClickListener(prevMonthButton){
            val currentPosition = monthYearSpinner.selectedItemPosition
            if (currentPosition > 0) {
                monthYearSpinner.setSelection(currentPosition - 1)
            }
        }

        ClickUtils.setDebounceClickListener(nextMonthButton){
            val currentPosition = monthYearSpinner.selectedItemPosition
            if (currentPosition < monthYearList.size - 1) {
                monthYearSpinner.setSelection(currentPosition + 1)
            }
        }
        
        // Criar o diálogo
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Configurar botões de ação
        ClickUtils.setDebounceClickListener(btnCancel){
            dialog.dismiss()
        }

        ClickUtils.setDebounceClickListener(btnOk){
            // Atualizar o calendário principal com a data selecionada
            calendar.set(Calendar.YEAR, dialogCalendar.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, dialogCalendar.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
            
            // Atualizar a interface
            updateDateText()
            loadExistingData()
            
            dialog.dismiss()
        }
        
        // Exibir o diálogo
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun setupMoodSelection() {

        // Calcular tamanho baseado na largura da tela
        val screenWidth = resources.displayMetrics.widthPixels
        val containerSize = (screenWidth * 0.13).toInt() // 13% da largura da tela
        val iconSize = (containerSize * 1).toInt() // 99% do tamanho do container

        val verySadImage = findViewById<ImageView>(R.id.emotion_very_sad)
        val sadImage = findViewById<ImageView>(R.id.emotion_sad)
        val neutralImage = findViewById<ImageView>(R.id.emotion_neutral)
        val happyImage = findViewById<ImageView>(R.id.emotion_happy)
        val veryHappyImage = findViewById<ImageView>(R.id.emotion_very_happy)

        val layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            setMargins(Utils.dpToPx(applicationContext, 8), Utils.dpToPx(applicationContext, 8),
                Utils.dpToPx(applicationContext, 8), Utils.dpToPx(applicationContext, 8))
        }

        verySadImage.layoutParams = layoutParams
        sadImage.layoutParams = layoutParams
        neutralImage.layoutParams = layoutParams
        happyImage.layoutParams = layoutParams
        veryHappyImage.layoutParams = layoutParams

        // Configurar cliques nos humores
            ClickUtils.setDebounceClickListener(binding.emotionVeryHappy){
            selectedMoodType = 0
            updateMoodSelection()
            checkForChanges()
        }

        ClickUtils.setDebounceClickListener(binding.emotionHappy){
            selectedMoodType = 1
            updateMoodSelection()
            checkForChanges()
        }

        ClickUtils.setDebounceClickListener(binding.emotionNeutral){
            selectedMoodType = 2
            updateMoodSelection()
            checkForChanges()
        }

        ClickUtils.setDebounceClickListener(binding.emotionSad){
            selectedMoodType = 3
            updateMoodSelection()
            checkForChanges()
        }

        ClickUtils.setDebounceClickListener(binding.emotionVerySad){
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
        binding.imageDay.setImageDrawable(null)
        binding.imageDay.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.imageDay.visibility = View.GONE
        binding.placeholderContainer.visibility = View.VISIBLE
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
                    binding.imageDay.visibility = View.VISIBLE
                    binding.placeholderContainer.visibility = View.GONE
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
        ClickUtils.setDebounceClickListener(binding.imageDay){
            showImageSourceDialog()
        }

        ClickUtils.setDebounceClickListener(binding.cardImage){
            showImageSourceDialog()
        }

        ClickUtils.setDebounceClickListener(binding.placeholderContainer){
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
        // Verificar se existe imagem para mostrar botão de deletar
        val hasExistingImage = if (moodId > 0) {
            val mood = repository.get(moodId)
            !mood.imagePath.isNullOrEmpty()
        } else false

        // Criar e configurar o BottomSheet
        val bottomSheet = ImageSourceBottomSheet.newInstance(hasExistingImage)
        
        // Configurar o listener para as ações do BottomSheet
        bottomSheet.setImageSourceListener(object : ImageSourceBottomSheet.ImageSourceListener {
            override fun onCameraSelected() {
                checkCameraPermission()
            }

            override fun onGallerySelected() {
                checkGalleryPermission()
            }

            override fun onDeleteSelected() {
                CustomAlertDialog.create(this@EditDayActivity)
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
                                
                                // Atualizar a UI
                                binding.imageDay.setImageDrawable(null)
                                binding.imageDay.visibility = View.GONE
                                binding.placeholderContainer.visibility = View.VISIBLE
                                selectedImageUri = null
                                
                                showCustomToast(this@EditDayActivity, getString(R.string.image_deleted))
                                checkForChanges()
                            }
                        }
                    }
                    .setNegativeListener {  }
                    .setCancelable(false)
                    .show()
            }
        })
        
        // Mostrar o BottomSheet
        bottomSheet.show(supportFragmentManager, "ImageSourceBottomSheet")
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            cameraPermissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Usuário negou permanentemente (após já ter solicitado uma vez)
                showSettingsPermissionDialog(getString(R.string.camera_permission_denied_permanently))
            }
            else -> {
                // Primeira solicitação ou negação sem "não perguntar novamente"
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGalleryPermission() {
        // Log the current state for debugging
        Log.d("PermissionDebug", "Gallery permission check: " +
                "granted=${ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED}, " +
                "requested=$galleryPermissionRequested, " +
                "shouldShow=${shouldShowRequestPermissionRationale(storagePermission)}")

        // Always request permission the first time, regardless of shouldShowRequestPermissionRationale
        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else if (!galleryPermissionRequested) {
            // First time requesting - always show the system dialog
            Log.d("PermissionDebug", "First time requesting gallery permission")
            requestGalleryPermissionLauncher.launch(storagePermission)
        } else if (!shouldShowRequestPermissionRationale(storagePermission)) {
            // User denied with "Don't ask again"
            Log.d("PermissionDebug", "User denied gallery permission permanently")
            showSettingsPermissionDialog(getString(R.string.gallery_permission_denied_permanently))
        } else {
            // User denied without "Don't ask again"
            Log.d("PermissionDebug", "User denied gallery permission, can ask again")
            requestGalleryPermissionLauncher.launch(storagePermission)
        }
    }

    private fun showSettingsPermissionDialog(message: String) {

        CustomAlertDialog .create(this)
            .setMessage(message)
            .setPositiveListener {
                // Abrir configurações do aplicativo
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setDescricaoBtnPositive(getString(R.string.btn_go_config))
            .setDescricaoBtnNegative(getString(R.string.btn_cancel))
            .setNegativeListener {
                // Quando o usuário clicar em cancelar, reabrir o diálogo de fonte de imagem
                showImageSourceDialog()
            }
            .setSmallFont()
            .setCancelable(false)
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
            binding.imageDay.visibility = View.VISIBLE
            binding.placeholderContainer.visibility = View.GONE
            checkForChanges()
        } ?: run {
            binding.imageDay.setImageDrawable(null)
            binding.imageDay.visibility = View.GONE
            binding.placeholderContainer.visibility = View.VISIBLE
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
            .setMessage(getString(R.string.unsaved_changes_message))
            .setPositiveListener {
                onConfirm()
            }
            .setCancelable(false)
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