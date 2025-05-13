package com.br.leo.moodsnap.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.edit.EditDayActivity
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.utils.Utils.showCustomToast
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

class DialogEmotions(
    private val viewModel: MainViewModel,
    private val existingMoodId: Long = 0,
    private val selectedDate: Calendar
) : BottomSheetDialogFragment() {

    private val EDIT_DAY_REQUEST = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.emotions_layout, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                
                // Definir a altura máxima para garantir que o diálogo fique acima do FAB
                val displayMetrics = requireContext().resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val marginFromBottom = (80 * displayMetrics.density).toInt() // 80dp em pixels
                it.layoutParams.height = screenHeight - marginFromBottom

                FontUtils.applyFontToView(requireContext(), it)
            }
        }
        
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply current font
        FontUtils.applyFontToView(requireContext(), view)

        initComponents(view)
    }

    private fun initComponents(view: View) {
        // Configurar a data selecionada
        val selectedDateText = view.findViewById<TextView>(R.id.selected_date)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
        val month = selectedDate.get(Calendar.MONTH)
        val year = selectedDate.get(Calendar.YEAR)
        
        // Obter o nome do mês traduzido
        val monthName = DateUtils.getMonthName(requireContext(), month)
        
        selectedDateText.text = getString(R.string.date_format, day, monthName, year)

        val today = Calendar.getInstance()
        val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                selectedDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

//        view.findViewById<MaterialTextView>(R.id.title_dialog).text =
//            if (isToday) getString(R.string.how_are_you_feeling)
//            else getString(R.string.how_were_you_feeling)

        // Configurar botão de editar
        val btnEdit = view.findViewById<ImageButton>(R.id.btn_edit)
       // Utils.updateBackGroundColor(requireContext(), btnEdit, R.color.btn_edit)
        btnEdit.visibility = View.VISIBLE
        setupEditButton()

        // Configurar botão de deletar
        val btnDelete = view.findViewById<ImageButton>(R.id.btn_delete)
        //Utils.updateBackGroundColor(requireContext(), btnDelete, R.color.primary_red)
        if (existingMoodId > 0) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog()
            }
            
            // Configurar botão de compartilhar
            val btnShare = view.findViewById<ImageButton>(R.id.btn_share)
            btnShare.visibility = View.VISIBLE
            btnShare.setOnClickListener {
                shareMood()
            }
        }

        // Calcular tamanho baseado na largura da tela
        val screenWidth = resources.displayMetrics.widthPixels
        val containerSize = (screenWidth * 0.15).toInt() // 13% da largura da tela
        val iconSize = (containerSize * 1).toInt() // 99% do tamanho do container

        val verySadImage = view.findViewById<ImageView>(R.id.emotion_very_sad)
        val sadImage = view.findViewById<ImageView>(R.id.emotion_sad)
        val neutralImage = view.findViewById<ImageView>(R.id.emotion_neutral)
        val happyImage = view.findViewById<ImageView>(R.id.emotion_happy)
        val veryHappyImage = view.findViewById<ImageView>(R.id.emotion_very_happy)

        val layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            setMargins(Utils.dpToPx(requireContext(), 6), Utils.dpToPx(requireContext(), 6),
                Utils.dpToPx(requireContext(), 6), Utils.dpToPx(requireContext(), 6))
        }

        verySadImage.layoutParams = layoutParams
        sadImage.layoutParams = layoutParams
        neutralImage.layoutParams = layoutParams
        happyImage.layoutParams = layoutParams
        veryHappyImage.layoutParams = layoutParams

        view.findViewById<ImageView>(R.id.emotion_very_happy)?.setOnClickListener {
            saveMood(R.drawable.very_happy_icon)
        }

        view.findViewById<ImageView>(R.id.emotion_happy)?.setOnClickListener {
            saveMood(R.drawable.happy_icon)
        }

        view.findViewById<ImageView>(R.id.emotion_neutral)?.setOnClickListener {
            saveMood(R.drawable.neutral_icon)
        }

        view.findViewById<ImageView>(R.id.emotion_sad)?.setOnClickListener {
            saveMood(R.drawable.sad_icon)
        }

        view.findViewById<ImageView>(R.id.emotion_very_sad)?.setOnClickListener {
            saveMood(R.drawable.very_sad_icon)
        }
    }

    private fun setupEditButton() {
        val btnEdit = requireView().findViewById<ImageButton>(R.id.btn_edit)
        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditDayActivity::class.java)
            intent.putExtra("mood_id", existingMoodId)
            intent.putExtra("selected_date", selectedDate.timeInMillis)
            startActivityForResult(intent, EDIT_DAY_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_DAY_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val newDateMillis = data.getLongExtra("current_date", -1L)
            val newMonth = data.getIntExtra("current_month", -1)
            val newYear = data.getIntExtra("current_year", -1)
            
            if (newDateMillis != -1L && newMonth != -1 && newYear != -1) {
                selectedDate.timeInMillis = newDateMillis
                selectedDate.set(Calendar.MONTH, newMonth)
                selectedDate.set(Calendar.YEAR, newYear)
                
                // Reabrir o dialog com a nova data
                dismiss()
                
                // Atualizar o dia selecionado no calendário e o mês/ano
                viewModel.setSelectedDay(selectedDate.get(Calendar.DAY_OF_MONTH))
                viewModel.setSelectedMonth(newMonth)
                viewModel.setSelectedYear(newYear)

                // Buscar o humor para a nova data
                val mood = viewModel.getMoodByDate(selectedDate.time)
                val moodId = mood?.id?.toLong() ?: 0L
                
                val dialogEmotions = DialogEmotions(viewModel, moodId, selectedDate)
                dialogEmotions.show(parentFragmentManager, dialogEmotions.tag)
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            // Ignorar exceção se o fragmento não puder ser adicionado
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    private fun showDeleteConfirmationDialog() {
        val selectedDateText = requireView().findViewById<TextView>(R.id.selected_date)
        CustomAlertDialog.create(requireContext())
            .setTitle(getString(R.string.attention))
            .setMessage(getString(R.string.confirm_delete_mood, selectedDateText.text))
            .setPositiveListener {
                viewModel.deleteMood(existingMoodId)
               // showCustomToast(requireContext(), getString(R.string.mood_deleted_success))
                dismissAllowingStateLoss()
            }
            .setNegativeListener(null)
            .show()
    }

    private fun saveMood(emotionResId: Int) {
        if (isAdded) {
            viewModel.setSelectedEmotion(emotionResId)
            showCustomToast(requireContext(), getString(R.string.mood_registered_success))
            dismissAllowingStateLoss()
        }
    }

    private fun shareMood() {
        if (!isAdded) return
        
        // Get the mood data
        val mood = viewModel.getMoodById(existingMoodId)
        if (mood == null) {
            Utils.showCustomToast(requireContext(), getString(R.string.error_generic))
            return
        }
        
        // Create a bitmap to share
        val shareBitmap = createShareImage(mood.moodType)
        
        // Save bitmap to cache directory
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        val shareImageFile = File(cachePath, "shared_mood.png")
        
        try {
            val outputStream = FileOutputStream(shareImageFile)
            shareBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            // Get URI for the file
            val shareImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                shareImageFile
            )
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareImageUri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_mood_title))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_mood_title))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start the share activity
            startActivity(Intent.createChooser(shareIntent, getString(R.string.btn_share)))
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.showCustomToast(requireContext(), getString(R.string.error_generic))
        }
    }
    
    private fun createShareImage(moodType: Int): Bitmap {
        val width = 1200
        val height = 630
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor("#E0E0E0".toColorInt())

        val moodColor = when (moodType) {
            0 -> resources.getColor(R.color.very_happy_color)
            1 ->  resources.getColor(R.color.happy_color)
            2 ->  resources.getColor(R.color.neutral_color)
            3 ->  resources.getColor(R.color.sad_color)
            4 ->  resources.getColor(R.color.very_sad_color)
            else ->  resources.getColor(R.color.neutral_color)
        }

        val centerX = width / 2f
        val centerY = height / 2f
        
        val circlePaint = Paint().apply {
            color = moodColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val circleRadius = height / 6.5f
        val circleCenterX = centerX
        val circleCenterY = centerY - 150
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, circlePaint)
        
        val iconSize = (circleRadius * 1.8).toInt()
        val iconLeft = (centerX - iconSize / 2).toInt()
        val iconTop = (circleCenterY - iconSize / 2).toInt()
        
        val moodIconDrawable = ContextCompat.getDrawable(requireContext(), Utils.getMoodIcon(moodType))
        moodIconDrawable?.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        moodIconDrawable?.draw(canvas)
        
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate.time
        val dayOfWeek = DateUtils.getDayOfWeekName(requireContext(), calendar.get(Calendar.DAY_OF_WEEK) - 1)
        val month = DateUtils.getMonthName(requireContext(), calendar.get(Calendar.MONTH))
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val formattedDate = "$dayOfWeek, $month $day"
        
        val dateBackgroundPaint = Paint().apply {
            color = "#F5F5F5".toColorInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val dateTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 45f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val dateTextWidth = dateTextPaint.measureText(formattedDate)
        val dateRectLeft = centerX - dateTextWidth / 2 - 30
        val dateRectTop = circleCenterY + circleRadius + 30
        val dateRectRight = centerX + dateTextWidth / 2 + 30
        val dateRectBottom = dateRectTop + 70
        
        val dateRectF = RectF(dateRectLeft, dateRectTop, dateRectRight, dateRectBottom)
        canvas.drawRoundRect(dateRectF, 20f, 20f, dateBackgroundPaint)
        
        canvas.drawText(formattedDate, centerX, dateRectTop + 48, dateTextPaint)
        
        val appNamePaint = Paint().apply {
            color = resources.getColor(R.color.primary_green)
            textSize = 55f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        try {
            val appIconSize = 80
            val appIconLeft = (centerX - 120).toInt()
            val appIconTop = height - 120
            
            val appIcon = requireContext().packageManager.getApplicationIcon(requireContext().packageName)
            appIcon.setBounds(appIconLeft, appIconTop, appIconLeft + appIconSize, appIconTop + appIconSize)
            appIcon.draw(canvas)
            
            canvas.drawText(getString(R.string.app_name), centerX + 60 , height - 60f, appNamePaint)
        } catch (e: Exception) {
            canvas.drawText(getString(R.string.app_name), centerX, height - 60f, appNamePaint)
            e.printStackTrace()
        }
        
        return bitmap
    }
}
