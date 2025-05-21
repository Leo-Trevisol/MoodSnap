package com.br.leo.moodsnap.ui.home

import android.Manifest
import android.content.ContentValues.TAG
import android.graphics.Color
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.FragmentHomeBinding
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.ui.adapters.FontAdapter
import com.br.leo.moodsnap.ui.adapters.LanguageAdapter
import com.br.leo.moodsnap.ui.adapters.WeekdaysAdapter
import com.br.leo.moodsnap.ui.dialog.CustomAlertDialog
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.dialog.OnboardingDialog
import com.br.leo.moodsnap.ui.dialog.TutorialBottomSheet
import com.br.leo.moodsnap.ui.model.FontModel
import com.br.leo.moodsnap.ui.model.LanguageModel
import com.br.leo.moodsnap.ui.notifications.NotificationHelper
import com.br.leo.moodsnap.ui.utils.ButtonUtils
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.FontUtils.updateFontDialogPicker
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.viewmodel.HomeViewModel
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private var selectedDay: Int = -1
    private lateinit var gestureDetector: GestureDetector

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showNotificationBottomSheet()
        } else {
            showNotificationPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply the saved theme preference
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedTheme = sharedPreferences.getInt("current_theme", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
        
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        mainViewModel.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Apply current font
        activity?.let { activity ->
            FontUtils.applyFontToActivity(activity)
        }

        setupGestureDetector()
        setupDatePickers()
        setupWeekdaysGrid()
        setupCalendarView()
        observeViewModel()
        updateDateTexts()
        setupFabListener()
        observeMainViewModel()
        setupSettingsMenu()
        // Carregar humores do mês atual
        homeViewModel.loadMoodsForMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        setupMonthYearSpinner()
        updateFabIcon()

        // Configurar o detector de gestos na view principal
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Reaplica a fonte atual quando o fragmento é retomado
        activity?.let { activity ->
            FontUtils.applyFontToActivity(activity)
        }
        
        // Força atualização do calendário para reaplicar a fonte
        calendarAdapter.notifyDataSetChanged()
    }

    /**
     * Normaliza um Calendar para comparação de datas, removendo horas, minutos, segundos e milissegundos
     */
    private fun normalizeCalendarDate(calendar: Calendar): Calendar {
        val normalized = calendar.clone() as Calendar
        normalized.set(Calendar.HOUR_OF_DAY, 0)
        normalized.set(Calendar.MINUTE, 0)
        normalized.set(Calendar.SECOND, 0)
        normalized.set(Calendar.MILLISECOND, 0)
        return normalized
    }

    private fun isDateInFuture(dayOfMonth: Int): Boolean {
        // Criar e normalizar a data atual
        val normalizedToday = normalizeCalendarDate(Calendar.getInstance())
        
        // Criar e normalizar a data a ser verificada
        val dateToCheck = Calendar.getInstance()
        dateToCheck.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), dayOfMonth)
        val normalizedDateToCheck = normalizeCalendarDate(dateToCheck)
        
        // Comparar as datas normalizadas
        return normalizedDateToCheck.after(normalizedToday)
    }

    private fun setupFabListener() {
        activity?.findViewById<View>(R.id.fab)?.setOnClickListener {
            if (selectedDay == -1) {
                // Se não houver dia selecionado, selecionar o dia atual
                val today = Calendar.getInstance()
                if (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                    // Se já estiver no mês atual, apenas seleciona o dia
                    selectedDay = today.get(Calendar.DAY_OF_MONTH)
                    calendarAdapter.setSelectedDay(selectedDay)
                    calendarAdapter.notifyDataSetChanged()
                } else {
                    // Se não estiver no mês atual, navega para o mês atual e seleciona o dia
                    calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                    calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                    selectedDay = today.get(Calendar.DAY_OF_MONTH)
                    updateCalendarForDate(calendar, keepSelectedDay = true)
                    calendarAdapter.setSelectedDay(selectedDay)
                    calendarAdapter.notifyDataSetChanged()
                }
            }

            // Verificar se já existe um humor para este dia
            val existingMood = homeViewModel.moodsForMonth.value?.find { mood ->
                val moodCalendar = Calendar.getInstance().apply { time = mood.date }
                moodCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                        moodCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                        moodCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }

            // Criar um Calendar com a data selecionada
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }

            // Abre o diálogo de emoções
            val moodId = existingMood?.id?.toLong() ?: 0L
            val dialogEmotions = DialogEmotions(mainViewModel, moodId, selectedCalendar)
            dialogEmotions.show(childFragmentManager, dialogEmotions.tag)
        }
    }

    private fun observeMainViewModel() {
        mainViewModel.selectedEmotion.observe(viewLifecycleOwner) { emotionResId ->
            if (selectedDay != -1) {
                // Salvar o mês e ano atuais antes de modificar o calendário
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                // Criar data para o dia selecionado
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                val selectedDate = calendar.time

                // Criar novo MoodModel
                val moodType = when (emotionResId) {
                    R.drawable.very_happy_icon -> 0
                    R.drawable.happy_icon -> 1
                    R.drawable.neutral_icon -> 2
                    R.drawable.sad_icon -> 3
                    R.drawable.very_sad_icon -> 4
                    else -> 2 // neutro como padrão
                }

                // Verificar se já existe um humor para esta data
                val existingMood = homeViewModel.moodsForMonth.value?.find { mood ->
                    val moodCalendar = Calendar.getInstance().apply { time = mood.date }
                    moodCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                            moodCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                            moodCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                }

                val mood = existingMood?.apply {
                    this.moodType = moodType
                } ?: MoodModel().apply {
                    date = selectedDate
                    this.moodType = moodType
                }

                // Salvar o humor
                homeViewModel.saveMood(mood)

                // Restaurar o mês e ano originais no calendário
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, currentMonth)
                
                // Resetar a seleção do dia após salvar o humor
                selectedDay = -1
                calendarAdapter.setSelectedDay(-1)
                calendarAdapter.notifyDataSetChanged()

                // Atualizar o ícone do FAB apenas se o humor foi registrado para o dia atual (hoje)
                val today = Calendar.getInstance()
                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    selectedDay == today.get(Calendar.DAY_OF_MONTH)) {
                    updateFabIcon()
                }
            }
        }

        // Observar quando um humor for deletado
        mainViewModel.moodDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                // Recarregar os humores do mês atual
                homeViewModel.loadMoodsForMonth(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH)
                )
                // Resetar a seleção do dia após deletar o humor
                selectedDay = -1
                calendarAdapter.setSelectedDay(-1)
                calendarAdapter.notifyDataSetChanged()
                updateFabIcon()
            }
        }

        // Observar mudança do mês e ano
        mainViewModel.selectedMonth.observe(viewLifecycleOwner) { month ->
            if (month != null && month >= 0) {
                calendar.set(Calendar.MONTH, month)
                updateCalendarForDate(calendar, keepSelectedDay = true)
            }
        }

        mainViewModel.selectedYear.observe(viewLifecycleOwner) { year ->
            if (year != null && year > 0) {
                calendar.set(Calendar.YEAR, year)
                updateCalendarForDate(calendar, keepSelectedDay = true)
            }
        }

        // Observar mudança do dia selecionado
        mainViewModel.selectedDay.observe(viewLifecycleOwner) { day ->
            if (day != null && day > 0) {
                selectedDay = day
                calendarAdapter.setSelectedDay(day)
                calendarAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupDatePickers() {
        binding.dateText.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.month_picker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.year_picker)

        // Apply current font to date picker dialog
        FontUtils.applyFontToView(requireContext(), dialogView)

        // Função para aplicar a fonte ao NumberPicker
        fun applyFontToNumberPicker(picker: NumberPicker) {
            try {
                val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId("default"))
                val pickerFields = NumberPicker::class.java.declaredFields
                
                // Aplicar fonte ao texto de entrada e à roda de seleção
                for (field in pickerFields) {
                    if (field.name == "mInputText" || field.name == "mSelectorWheelPaint") {
                        field.isAccessible = true
                        when (field.name) {
                            "mInputText" -> {
                                val inputText = field.get(picker) as EditText
                                inputText.typeface = typeface
                            }
                            "mSelectorWheelPaint" -> {
                                val paint = field.get(picker) as Paint
                                paint.typeface = typeface
                            }
                        }
                    }
                }

                // Aplicar fonte aos valores exibidos
                val count = picker.childCount
                for (i in 0 until count) {
                    val child = picker.getChildAt(i)
                    if (child is EditText) {
                        child.typeface = typeface
                    }
                }

                // Forçar atualização do NumberPicker
                picker.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
            
            // Apply font initially
            applyFontToNumberPicker(this)
            
            // Apply font when value changes and when scrolling
            setOnValueChangedListener { _, _, _ ->
                applyFontToNumberPicker(this)
            }
            setOnScrollListener { _, _ ->
                applyFontToNumberPicker(this)
            }
        }

        // Configurar o picker de anos
        yearPicker.apply {
            minValue = currentYear - 10
            maxValue = currentYear
            value = calendar.get(Calendar.YEAR)
            
            // Apply font initially
            applyFontToNumberPicker(this)
            
            // Apply font when value changes and when scrolling
            setOnValueChangedListener { _, _, newVal ->
                applyFontToNumberPicker(this)
                
                // Controle de meses futuros
                if (newVal == currentYear) {
                    monthPicker.maxValue = currentMonth
                    if (monthPicker.value > currentMonth) {
                        monthPicker.value = currentMonth
                    }
                } else {
                    monthPicker.maxValue = 11
                }
            }
            setOnScrollListener { _, _ ->
                applyFontToNumberPicker(this)
            }
        }

        // Verificar se a data selecionada é futura
        val selectedYear = yearPicker.value
        val selectedMonth = monthPicker.value
        if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            yearPicker.value = currentYear
            monthPicker.value = currentMonth
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(getString(R.string.hint_date))
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                val selectedYear = yearPicker.value
                val selectedMonth = monthPicker.value

                // Verificar se a data selecionada é futura
                if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
                    Utils.showCustomToast(requireContext(), getString(R.string.error_invalid_date))
                    return@setPositiveButton
                }

                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                updateDateTexts()
                updateCalendarForDate(calendar)
                homeViewModel.loadMoodsForMonth(selectedYear, selectedMonth)
            }
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Apply font to dialog title and buttons when dialog is shown
        dialog.setOnShowListener {

            updateFontDialogPicker(requireContext(), dialog, dialogView)

            // Apply font to the dialog view itself to catch any remaining text elements
            FontUtils.applyFontToView(requireContext(), dialog.window?.decorView ?: return@setOnShowListener)
        }

        dialog.show()
    }

    // Extension function to find all views of a specific type in a view hierarchy
    private fun <T : View> View.findViewsByType(type: Class<T>): List<T> {
        val result = mutableListOf<T>()
        if (type.isInstance(this)) {
            result.add(type.cast(this))
        }
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                result.addAll(getChildAt(i).findViewsByType(type))
            }
        }
        return result
    }

    private fun updateDateTexts() {
        binding.dateText.text = DateUtils.formatMonthYear(requireContext(), calendar)
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth(), requireContext())
        binding.calendarGrid.apply {
            adapter = calendarAdapter
            itemAnimator = null // Desabilitar animações do RecyclerView
            setHasFixedSize(true) // Otimizar performance
        }

        // Configurar o mês inicial
        updateCalendarForDate(calendar)

        calendarAdapter.setOnDayClickListener { dayOfMonth ->
            if (isDateInFuture(dayOfMonth)) {
                Utils.showCustomToast(requireContext(), requireContext().getString(R.string.future_date_not_allowed))
                return@setOnDayClickListener
            }
            
            // Atualizar seleção sem redesenhar todo o grid
            val oldSelectedDay = selectedDay
            selectedDay = dayOfMonth
            if (oldSelectedDay != -1) {
                calendarAdapter.notifyItemChanged(oldSelectedDay + calendarAdapter.getFirstDayOfWeek())
            }
            calendarAdapter.notifyItemChanged(selectedDay + calendarAdapter.getFirstDayOfWeek())

            // Verificar se já existe um humor para este dia
            val existingMood = homeViewModel.moodsForMonth.value?.find { mood ->
                val moodCalendar = Calendar.getInstance().apply { time = mood.date }
                moodCalendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth &&
                        moodCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                        moodCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }

            // Criar um Calendar com a data selecionada
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }

            // Mostrar o BottomSheet de emoções
            val moodId = existingMood?.id?.toLong() ?: 0L
            val dialogEmotions = DialogEmotions(mainViewModel, moodId, selectedCalendar)
            dialogEmotions.show(childFragmentManager, dialogEmotions.tag)
        }
    }

    private fun updateCalendarForDate(calendar: Calendar, keepSelectedDay: Boolean = false) {
        calendarAdapter.setDisplayMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )

        // Reaplica a fonte atual
        FontUtils.applyFontToView(requireContext(), binding.root)
        calendarAdapter.notifyDataSetChanged()

        // Atualizar o texto do mês
        val month = when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> getString(R.string.month_january)
            Calendar.FEBRUARY -> getString(R.string.month_february)
            Calendar.MARCH -> getString(R.string.month_march)
            Calendar.APRIL -> getString(R.string.month_april)
            Calendar.MAY -> getString(R.string.month_may)
            Calendar.JUNE -> getString(R.string.month_june)
            Calendar.JULY -> getString(R.string.month_july)
            Calendar.AUGUST -> getString(R.string.month_august)
            Calendar.SEPTEMBER -> getString(R.string.month_september)
            Calendar.OCTOBER -> getString(R.string.month_october)
            Calendar.NOVEMBER -> getString(R.string.month_november)
            Calendar.DECEMBER -> getString(R.string.month_december)
            else -> ""
        }
        val year = calendar.get(Calendar.YEAR)
        binding.dateText.text = "$month $year"

        // Carregar humores do mês
        homeViewModel.loadMoodsForMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )

        // Verificar se o dia selecionado é válido para o novo mês
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (keepSelectedDay && selectedDay > daysInMonth) {
            // Ajustar o dia selecionado para o último dia do mês atual
            selectedDay = daysInMonth
        }

        // Manter o dia selecionado se necessário
        if (!keepSelectedDay) {
            selectedDay = -1
            calendarAdapter.setSelectedDay(-1)
        } else if (selectedDay > 0) {
            calendarAdapter.setSelectedDay(selectedDay)
            calendarAdapter.notifyDataSetChanged()
        }
    }

    private fun isCurrentOrPastMonth(calendar: Calendar): Boolean {
        val currentDate = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) < currentDate.get(Calendar.YEAR) ||
                (calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) <= currentDate.get(Calendar.MONTH))
    }

    private fun getDaysInMonth(): Int {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun observeViewModel() {
        homeViewModel.moodsForMonth.observe(viewLifecycleOwner) { moods ->
            calendarAdapter.updateData(getDaysInMonth(), moods)
            updateFabIcon()
        }
    }

    private fun setupWeekdaysGrid() {
        binding.weekdaysGrid.adapter = WeekdaysAdapter()
    }

    private fun setupMonthYearSpinner() {
        val monthYearOptions = getMonthYearOptions()

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_month_year,
            monthYearOptions
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(Color.WHITE)
                (view as TextView).apply {
                    gravity = Gravity.CENTER
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    setPadding(16, 16, 16, 16)
                }
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    gravity = Gravity.CENTER
                    textSize = 16f
                    setTextColor(Color.BLACK)
                }
                return view
            }
        }
    }

    private fun getMonthYearOptions(): List<String> {
        val options = mutableListOf<String>()
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)

        // Gerar opções para os últimos 10 anos
        for (year in currentYear downTo currentYear - 10) {
            for (month in 11 downTo 0) { // De dezembro a janeiro
                val tempCalendar = Calendar.getInstance()
                tempCalendar.set(Calendar.YEAR, year)
                tempCalendar.set(Calendar.MONTH, month)

                // Não incluir meses futuros do ano atual
                if (year == currentYear && month > currentCalendar.get(Calendar.MONTH)) {
                    continue
                }

                val monthName = tempCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                options.add("$monthName $year")
            }
        }

        return options
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val SWIPE_THRESHOLD = 100
                val SWIPE_VELOCITY_THRESHOLD = 100

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    val currentCalendar = Calendar.getInstance()
                    val currentYear = currentCalendar.get(Calendar.YEAR)
                    val currentMonth = currentCalendar.get(Calendar.MONTH)
                    val isCurrentMonth = calendar.get(Calendar.YEAR) == currentYear && 
                                       calendar.get(Calendar.MONTH) == currentMonth
                    val minYear = currentYear - 10 // Ano mínimo permitido

                    if (diffX > 0) { // Deslize para a direita - Mês anterior
                        // Verificar se está tentando navegar para um ano anterior ao mínimo permitido
                        if (calendar.get(Calendar.MONTH) == Calendar.JANUARY) {
                            val nextYear = calendar.get(Calendar.YEAR) - 1
                            if (nextYear < minYear) {
                                Utils.showCustomToast(requireContext(), getString(R.string.error_invalid_date))
                                return false
                            }
                            calendar.set(Calendar.YEAR, nextYear)
                            calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                        } else {
                            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1)
                        }
                        updateCalendarForDate(calendar)
                        return true
                    } else { // Deslize para a esquerda
                        // Verificar se o próximo mês seria o mês atual
                        val nextMonth = if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
                            Calendar.JANUARY
                        } else {
                            calendar.get(Calendar.MONTH) + 1
                        }
                        
                        val nextYear = if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
                            calendar.get(Calendar.YEAR) + 1
                        } else {
                            calendar.get(Calendar.YEAR)
                        }
                        
                        // Verificar se o próximo mês/ano seria o mês/ano atual
                        val wouldBeCurrentMonth = nextYear == currentYear && nextMonth == currentMonth
                        
                        if (wouldBeCurrentMonth) {
                            // Se o próximo mês seria o mês atual, navega para o mês atual
                            calendar.set(Calendar.DAY_OF_MONTH, 1) // Evita problemas com dias que não existem no novo mês
                            calendar.set(Calendar.YEAR, currentYear)
                            calendar.set(Calendar.MONTH, currentMonth)
                            updateCalendarForDate(calendar)
                        } else if (isCurrentMonth) {
                            // Se já estiver no mês atual, navega para o dashboard
                            findNavController().navigate(R.id.action_home_to_dashboard)
                        } else {
                            // Se estiver em um mês anterior e o próximo não é o atual, navega para o próximo mês
                            if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
                                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1)
                                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                            } else {
                                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1)
                            }
                            updateCalendarForDate(calendar)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Configurar o detector de gestos na view principal e no calendar_grid
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.calendarGrid.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Retorna false para permitir que o evento continue para os itens do grid
        }
    }

    private fun updateFabIcon() {
        val today = Calendar.getInstance()
        // Verificar se existe humor para o dia atual (hoje)
        val todayMood = homeViewModel.moodsForMonth.value?.find { mood ->
            val moodCalendar = Calendar.getInstance().apply { time = mood.date }
            val isToday = moodCalendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
                    moodCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    moodCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
            isToday
        }

        // Definir o ícone com base no humor do dia atual
        val iconResource = when (todayMood?.moodType) {
            0 -> R.drawable.very_happy_icon
            1 -> R.drawable.happy_icon
            2 -> R.drawable.neutral_icon
            3 -> R.drawable.sad_icon
            4 -> R.drawable.very_sad_icon
            else -> R.drawable.default_icon
        }

        activity?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab)?.let { fab ->
            fab.setImageResource(iconResource)
        }
    }

    private fun setupSettingsMenu() {
        val settingsButton = view?.findViewById<View>(R.id.btn_settings)
        settingsButton?.setOnClickListener {
            showSettingsBottomSheet()
        }
    }

    private fun showSettingsBottomSheet() {
        // Usar o tema personalizado para o BottomSheet
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), bottomSheetView)

        // Obter o idioma atual
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentLanguage = sharedPreferences.getString("current_language", "system")
        val isSystemLanguageApply = sharedPreferences.getBoolean("is_system_language_apply", true)
        
        // Definir o texto do idioma atual
        val textCurrentLanguage = bottomSheetView.findViewById<TextView>(R.id.text_current_language)
        val languageName = when (currentLanguage) {
            "system" -> getString(R.string.language_system)
            "pt" -> "Português"
            "en" -> "English"
            "es" -> "Español"
            "fr" -> "Français"
            "de" -> "Deutsch"
            "it" -> "Italiano"
            else -> getString(R.string.language_system)
        }
        textCurrentLanguage.text = languageName
        
        // Obter o tema atual
        val currentTheme = sharedPreferences.getInt("current_theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Definir o texto do tema atual
        val textCurrentTheme = bottomSheetView.findViewById<TextView>(R.id.text_current_theme)
        val themeName = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.system_theme)
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_theme)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_theme)
            else -> getString(R.string.system_theme)
        }
        textCurrentTheme.text = themeName
        
        // Obter a fonte atual
        val currentFont = sharedPreferences.getString("current_font", "default") ?: "default"
        
        // Definir o texto da fonte atual
        val textCurrentFont = bottomSheetView.findViewById<TextView>(R.id.text_current_font)
        val fontName = when (currentFont) {
            "default" -> getString(R.string.default_font)
            "itim" -> "Itim"
            "open_sans" -> getString(R.string.open_sans_font)
            "pangolin" -> "Pangolin"
            "underdog" -> "Underdog"
            "lato" -> getString(R.string.lato_font)
            "orbitron" -> "Orbitron"
            "mulish" -> getString(R.string.mulish_font)
            "limelight" -> "LimeLight"
            else -> getString(R.string.default_font)
        }
        textCurrentFont.text = fontName
        
        // Obter o status das notificações
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)
        val notificationHour = sharedPreferences.getInt("notification_hour", 20)
        val notificationMinute = sharedPreferences.getInt("notification_minute", 0)
        
        // Definir o texto do status das notificações
        val textNotificationStatus = bottomSheetView.findViewById<TextView>(R.id.text_notification_status)
        if (notificationsEnabled) {
            // Formatar o horário em formato de 12 horas com AM/PM
            val hour = if (notificationHour > 12) notificationHour - 12 else if (notificationHour == 0) 12 else notificationHour
            val amPm = if (notificationHour >= 12) "PM" else "AM"
            val minuteStr = if (notificationMinute < 10) "0$notificationMinute" else "$notificationMinute"
            textNotificationStatus.text = "$hour:$minuteStr $amPm"
        } else {
            textNotificationStatus.text = ""//getString(R.string.disabled)
        }
        
        // Configurar os listeners dos botões
        setupSettingsButtons(bottomSheetView, bottomSheetDialog)
        
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.setBackgroundResource(R.drawable.background_rounded_top)
        }
        
        bottomSheetDialog.show()
    }
    
    private fun setupSettingsButtons(bottomSheetView: View, bottomSheetDialog: BottomSheetDialog) {
        // Botão de idiomas
        val btnLanguages = bottomSheetView.findViewById<LinearLayout>(R.id.btn_languages)
        btnLanguages.setOnClickListener {
            bottomSheetDialog.dismiss()
            showLanguageBottomSheet()
        }
        
        // Botão de temas
        val btnThemes = bottomSheetView.findViewById<LinearLayout>(R.id.btn_themes)
        btnThemes.setOnClickListener {
            bottomSheetDialog.dismiss()
            showThemeBottomSheet()
        }
        
        // Botão de notificações
        val btnNotifications = bottomSheetView.findViewById<LinearLayout>(R.id.btn_notifications)
        btnNotifications.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                        showNotificationBottomSheet()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        showNotificationPermissionRationaleDialog()
                    }
                    else -> {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                showNotificationBottomSheet()
            }
        }
        
        // Botão de fontes
        val btnFonts = bottomSheetView.findViewById<LinearLayout>(R.id.btn_fonts)
        btnFonts.setOnClickListener {
            bottomSheetDialog.dismiss()
            showFontBottomSheet()
        }
        
        // Botão de tutorial
        val btnTutorial = bottomSheetView.findViewById<LinearLayout>(R.id.btn_tutorial)
        btnTutorial.setOnClickListener {
            bottomSheetDialog.dismiss()
            showOnboardingTutorial()
        }

        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btn_close)
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
    }

    private fun showLanguageBottomSheet() {
        // Criar o BottomSheetDialog com o estilo personalizado
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_language_selection, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), bottomSheetView)
        
        // Configurar o botão de voltar
        configureBackButton(bottomSheetView, bottomSheetDialog)

        
        // Lista de idiomas disponíveis
        val languages = listOf(
            LanguageModel("system", R.string.language_system, "system"),
            LanguageModel("en", R.string.language_english, "en"),
            LanguageModel("pt", R.string.language_portuguese, "pt"),
            LanguageModel("es", R.string.language_spanish, "es"),
            LanguageModel("zh", R.string.language_chinese, "zh"),
            LanguageModel("fr", R.string.language_french, "fr"),
            LanguageModel("ru", R.string.language_russian, "ru"),
            LanguageModel("de", R.string.language_german, "de"),
            LanguageModel("it", R.string.language_italian, "it")
        )

        // Carregar a preferência de idioma atual
        val currentLanguage = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getString("current_language", "system")
        val isSystemLanguageApply = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("is_system_language_apply", true)

        var selectedLanguage = if (isSystemLanguageApply) "system" else (currentLanguage ?: "en")

        // Configurar o RecyclerView
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.languages_recycler_view)
        val adapter = LanguageAdapter(requireContext(), languages) { language ->
            selectedLanguage = language.code
        }
        recyclerView.adapter = adapter
        
        // Forçar o layout do RecyclerView para garantir que os botões sejam exibidos corretamente
        recyclerView.post {
            recyclerView.layoutManager?.requestLayout()
            adapter.notifyDataSetChanged()
        }

        // Definir a seleção inicial
        adapter.setSelectedLanguage(selectedLanguage)

        val btnConfirm : Button =  bottomSheetView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm, textColor = resources.getColor(R.color.primary))
        btnConfirm.setOnClickListener {
            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putString("current_language", selectedLanguage)
                apply()
            }
            
            // Definir o locale com base na seleção
            if (selectedLanguage == "system") {
                // Usar o idioma do sistema
                val systemLocale = Resources.getSystem().configuration.locales.get(0)
                Locale.setDefault(systemLocale)
                val config = resources.configuration
                config.setLocale(systemLocale)
                resources.updateConfiguration(config, resources.displayMetrics)
                
                // Salvar a flag de aplicação do idioma do sistema
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_system_language_apply", true)
                    apply()
                }
            } else {
                // Usar o idioma selecionado
                val locale = Locale(selectedLanguage)
                Locale.setDefault(locale)
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
                
                // Salvar a flag de aplicação do idioma do sistema como falsa
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_system_language_apply", false)
                    apply()
                }
            }
            
            bottomSheetDialog.dismiss()
            
            // Reiniciar a atividade para aplicar a mudança de idioma
            activity?.recreate()
        }

        bottomSheetDialog.show()
    }

    private fun showThemeBottomSheet() {
        // Criar o BottomSheetDialog com o estilo personalizado
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_theme_selection, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), bottomSheetView)
        
        // Obter os layouts de tema
        val layoutSystemTheme = bottomSheetView.findViewById<LinearLayout>(R.id.btn_system_theme)
        val layoutLightTheme = bottomSheetView.findViewById<LinearLayout>(R.id.btn_light_theme)
        val layoutDarkTheme = bottomSheetView.findViewById<LinearLayout>(R.id.btn_dark_theme)
        
        // Obter os TextViews de tema
        val textSystemTheme = bottomSheetView.findViewById<TextView>(R.id.text_system_theme)
        val textLightTheme = bottomSheetView.findViewById<TextView>(R.id.text_light_theme)
        val textDarkTheme = bottomSheetView.findViewById<TextView>(R.id.text_dark_theme)
        
        // Lista de layouts e textos para facilitar a manipulação
        val themeLayouts = listOf(layoutSystemTheme, layoutLightTheme, layoutDarkTheme)
        val themeTexts = listOf(textSystemTheme, textLightTheme, textDarkTheme)
        
        // Verificar se é a primeira vez que o tema está sendo aplicado
        val isFirstThemeApply = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("is_first_theme_apply", true)
        
        // Função para destacar o layout selecionado
        fun highlightSelectedTheme(position: Int) {
            // Resetar todos os layouts para o estado normal
            themeLayouts.forEachIndexed { index, layout ->
                layout.setBackgroundResource(android.R.color.transparent)
                themeTexts[index].setTextColor(ContextCompat.getColor(requireContext(), R.color.secundary))

                // Remover drawableRight de todos
                themeTexts[index].setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            // Aplicar estilo de seleção no item escolhido
            themeLayouts[position].setBackgroundResource(R.drawable.background_rounded_left)
            themeLayouts[position].backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_green)
            )
            themeTexts[position].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            // Adicionar drawableRight apenas ao item selecionado
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)
            drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.secundary))
            themeTexts[position].setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        }

        
        // Variável para armazenar o tema selecionado
        var selectedTheme = if (isFirstThemeApply) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.getDefaultNightMode()
        }
        
        // Destacar o tema atual
        when (selectedTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> highlightSelectedTheme(2) // Dark theme
            AppCompatDelegate.MODE_NIGHT_NO -> highlightSelectedTheme(1) // Light theme
            else -> highlightSelectedTheme(0) // System theme
        }
        
        // Configurar os listeners de clique para os layouts de tema
        layoutSystemTheme.setOnClickListener {
            highlightSelectedTheme(0)
            selectedTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        layoutLightTheme.setOnClickListener {
            highlightSelectedTheme(1)
            selectedTheme = AppCompatDelegate.MODE_NIGHT_NO
        }
        
        layoutDarkTheme.setOnClickListener {
            highlightSelectedTheme(2)
            selectedTheme = AppCompatDelegate.MODE_NIGHT_YES
        }

        // Configurar o botão de voltar
        configureBackButton(bottomSheetView, bottomSheetDialog)

        // Botão de confirmar
        val btnConfirm: Button = bottomSheetView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm, textColor = resources.getColor(R.color.primary))
        btnConfirm.setOnClickListener {
            // Salvar o tema selecionado nas preferências compartilhadas
            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putInt("current_theme", selectedTheme)
                apply()
            }
            
            if (isFirstThemeApply) {
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_first_theme_apply", false)
                    apply()
                }
            }
            
            // Aplicar o tema
            AppCompatDelegate.setDefaultNightMode(selectedTheme)
            bottomSheetDialog.dismiss()
        }
        
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.setBackgroundResource(R.drawable.background_rounded_top)
        }
        
        bottomSheetDialog.show()
    }

    private fun showNotificationBottomSheet() {
        // Criar o BottomSheetDialog com o estilo personalizado
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_notification_settings, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), bottomSheetView)
        
        // Garantir que o BottomSheet tenha bordas arredondadas
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.setBackgroundResource(R.drawable.background_rounded_top)
        }

        val switchNotifications = bottomSheetView.findViewById<Switch>(R.id.switch_notifications)
        val timePicker = bottomSheetView.findViewById<TimePicker>(R.id.time_picker)
        val notificationHelper = NotificationHelper(requireContext())

        // Carregar configurações de notificação salvas
        val notificationsEnabled = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("notifications_enabled", false)
        val notificationHour = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getInt("notification_hour", 20) // Padrão para 20h
        val notificationMinute = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getInt("notification_minute", 0)

        Log.d(TAG, "Loading notification settings - Enabled: $notificationsEnabled, Hour: $notificationHour, Minute: $notificationMinute")

        switchNotifications.isChecked = notificationsEnabled
        timePicker.hour = notificationHour
        timePicker.minute = notificationMinute
        timePicker.setIs24HourView(false) // Configurar para formato de 12 horas com AM/PM
        timePicker.isEnabled = notificationsEnabled

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Notification switch changed to: $isChecked")
            timePicker.isEnabled = isChecked
            if (!isChecked) {
                notificationHelper.cancelDailyNotification()
            }
        }

        // Configurar o botão de voltar
        configureBackButton(bottomSheetView, bottomSheetDialog)

        // Botão de confirmar
        val btnConfirm = bottomSheetView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm, textColor = resources.getColor(R.color.primary))
        btnConfirm.setOnClickListener {
            val isEnabled = switchNotifications.isChecked
            val hour = timePicker.hour
            val minute = timePicker.minute

            Log.d(TAG, "Saving notification settings - Enabled: $isEnabled, Hour: $hour, Minute: $minute")

            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putBoolean("notifications_enabled", isEnabled)
                putInt("notification_hour", hour)
                putInt("notification_minute", minute)
                apply()
            }

            if (isEnabled) {
                notificationHelper.scheduleDailyNotification(hour, minute)
                Log.d(TAG, "Notification scheduled for $hour:$minute")
            } else {
                notificationHelper.cancelDailyNotification()
                Log.d(TAG, "Notifications cancelled")
            }

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showNotificationPermissionRationaleDialog() {

        CustomAlertDialog .create(requireContext())
            .setTitle(getString(R.string.notifications))
            .setMessage(getString(R.string.notification_permission_required))
            .setPositiveListener {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setDescricaoBtnPositive(getString(R.string.btn_ok))
            .setDescricaoBtnNegative(getString(R.string.btn_cancel))
            .setNegativeListener(null)
            .show()

    }

    private fun showNotificationPermissionDeniedDialog() {
        CustomAlertDialog .create(requireContext())
            .setTitle(getString(R.string.attention_dialog))
            .setMessage(getString(R.string.notification_permission_denied_permanently))
            .setPositiveListener {
                openNotificationSettings()
            }
            .setDescricaoBtnPositive(getString(R.string.btn_go_config))
            .setDescricaoBtnNegative(getString(R.string.btn_cancel))
            .setNegativeListener(null)
            .show()
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun showOnboardingTutorial() {
        val tutorialBottomSheet = TutorialBottomSheet()
        tutorialBottomSheet.show(parentFragmentManager, "TutorialBottomSheet")
    }

    private fun showFontBottomSheet() {
        // Criar o BottomSheetDialog com o estilo personalizado
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_font_selection, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), bottomSheetView)
        
        // Garantir que o BottomSheet tenha bordas arredondadas
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.setBackgroundResource(R.drawable.background_rounded_top)
        }

        // Get preview text views
        val previewText = bottomSheetView.findViewById<TextView>(R.id.preview_text)
        val weekText = bottomSheetView.findViewById<TextView>(R.id.week_text)
        val dayText = bottomSheetView.findViewById<TextView>(R.id.day_text)
        
        // Get current date info
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        
        // Set current weekday abbreviation
        val weekdayAbbr = when (currentDayOfWeek) {
            Calendar.SUNDAY -> getString(R.string.weekday_sunday)
            Calendar.MONDAY -> getString(R.string.weekday_monday)
            Calendar.TUESDAY -> getString(R.string.weekday_tuesday)
            Calendar.WEDNESDAY -> getString(R.string.weekday_wednesday)
            Calendar.THURSDAY -> getString(R.string.weekday_thursday)
            Calendar.FRIDAY -> getString(R.string.weekday_friday)
            Calendar.SATURDAY -> getString(R.string.weekday_saturday)
            else -> ""
        }
        weekText.text = weekdayAbbr
        dayText.text = currentDay.toString()

        // Get current font
        val currentFont = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .getString("current_font", "default") ?: "default"

        // Setup RecyclerView
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.fonts_recycler_view)
        var selectedFont = currentFont
        
        val fontAdapter = FontAdapter(
            requireContext(),
            FontUtils.getAllFonts(requireContext())
        ) { font ->
            selectedFont = font.id
            // Update preview text with selected font
            val typeface = ResourcesCompat.getFont(requireContext(), font.resourceId)
            previewText.typeface = typeface
            weekText.typeface = typeface
            dayText.typeface = typeface
            if (typeface != null) {
                updateQuickDistribution(bottomSheetView, typeface)
            }
        }
        
        recyclerView.adapter = fontAdapter
        fontAdapter.setSelectedFont(currentFont)

        // Forçar o layout do RecyclerView para garantir que os botões sejam exibidos corretamente
        recyclerView.post {
            recyclerView.layoutManager?.requestLayout()
            recyclerView.adapter?.notifyDataSetChanged()
        }

        // Update preview text with current font
        val currentTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont))
        previewText.typeface = currentTypeface
        weekText.typeface = currentTypeface
        dayText.typeface = currentTypeface
        if (currentTypeface != null) {
            updateQuickDistribution(bottomSheetView, currentTypeface)
        }

        // Configurar o botão de voltar
        configureBackButton(bottomSheetView, bottomSheetDialog)

        // Botão de confirmar
        val btnConfirm = bottomSheetView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm, textColor = resources.getColor(R.color.primary))
        btnConfirm.setOnClickListener {
            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putString("current_font", selectedFont)
                apply()
            }

            // Apply font change instantly
            activity?.let { activity ->
                FontUtils.applyFontToActivity(activity)
            }

            // Notify adapter to update fonts
            calendarAdapter.notifyDataSetChanged()

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDialogs(settingsDialogView: View, settingsDialog: androidx.appcompat.app.AlertDialog) {
        // Apply current font to settings dialog
        FontUtils.applyFontToView(requireContext(), settingsDialogView)

        val btnTutorial : Button = settingsDialogView.findViewById<Button>(R.id.btn_tutorial)
        Utils.setupDialogConfirmButton(requireContext(), btnTutorial)
        btnTutorial.setOnClickListener {
            settingsDialog.dismiss()
            showOnboardingTutorial()
        }

        val btnLanguages : Button = settingsDialogView.findViewById<Button>(R.id.btn_languages)
        Utils.setupDialogConfirmButton(requireContext(), btnLanguages)
        btnLanguages.setOnClickListener {
            settingsDialog.dismiss()
            showLanguageBottomSheet()
        }

        val btnThemes : Button = settingsDialogView.findViewById<Button>(R.id.btn_themes)
        Utils.setupDialogConfirmButton(requireContext(), btnThemes)
        btnThemes.setOnClickListener {
            settingsDialog.dismiss()
            showThemeBottomSheet()
        }

        val btnNotifications : Button = settingsDialogView.findViewById<Button>(R.id.btn_notifications)
        Utils.setupDialogConfirmButton(requireContext(), btnNotifications)
        btnNotifications.setOnClickListener {
            settingsDialog.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                        showNotificationBottomSheet()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        showNotificationPermissionRationaleDialog()
                    }
                    else -> {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                showNotificationBottomSheet()
            }
        }

        val btnFonts : Button = settingsDialogView.findViewById<Button>(R.id.btn_fonts)
        Utils.setupDialogConfirmButton(requireContext(), btnFonts)
        btnFonts.setOnClickListener {
            settingsDialog.dismiss()
            showFontBottomSheet()
        }

        val btnBack : Button = settingsDialogView.findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener {
            settingsDialog.dismiss()
        }
    }

    private fun configureBackButton(bottomSheetView: View, bottomSheetDialog: BottomSheetDialog) {
        val btnBack = bottomSheetView.findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            bottomSheetDialog.dismiss()
            showSettingsBottomSheet()
        }
    }

    private fun updateQuickDistribution(view: View, typeface: Typeface) {
        val container = view.findViewById<LinearLayout>(R.id.quick_distribution_container)
        container.removeAllViews()

        val total = 100f

        // Encontrar o humor com a maior porcentagem
        var maxPercentage = 0
        var maxMoodType = -1

        listOf(4, 3, 2, 1, 0).forEach { moodType ->
            val count = 20
            val percentage = (count / total * 100).roundToInt()
            if (percentage > maxPercentage) {
                maxPercentage = percentage
                maxMoodType = moodType
            }
        }

        // Calcular tamanho baseado na largura da tela
        val screenWidth = resources.displayMetrics.widthPixels
        val containerSize = (screenWidth * 0.1).toInt() // 13% da largura da tela
        val iconSize = (containerSize * 1).toInt() // 100% do tamanho do container

        // Tamanho fixo para o container de porcentagem
        val percentageWidth = resources.getDimensionPixelSize(R.dimen.percentage_width_min)
        val percentageHeight = resources.getDimensionPixelSize(R.dimen.percentage_height_min)

        listOf(4, 3, 2, 1, 0).forEach { moodType ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Container circular para o ícone
            val iconContainer = CardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    containerSize,
                    containerSize
                )
                radius = containerSize / 2f
                cardElevation = 0f
                setCardBackgroundColor(Utils.getMoodColor(moodType, requireContext()))
            }

            // Ícone do humor
            val icon = ImageView(context).apply {
                setImageResource(Utils.getMoodDrawable(moodType))
                layoutParams = LinearLayout.LayoutParams(
                    iconSize,
                    iconSize
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            // Texto da porcentagem com background arredondado
            val percentageText = TextView(requireContext()).apply {
                if(total == 0f) {
                    text = "0%"
                } else {
                    val count = 20
                    val percentage = (count / total * 100).roundToInt()
                    text = "$percentage%"
                }

                // Aplicar cor de texto baseada no background
                if (moodType == 2) {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.highlighted_percentage_background)
                    setTextColor(Color.WHITE)
                } else {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_percentage_background)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.secundary))
                }

                textSize = resources.getDimension(R.dimen.legend_bar_chart)
                gravity = Gravity.CENTER

                // Aplicar tamanho fixo
                layoutParams = LinearLayout.LayoutParams(
                    percentageWidth,
                    percentageHeight
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
            }

            percentageText.typeface = typeface

            iconContainer.addView(icon)
            itemLayout.addView(iconContainer)
            itemLayout.addView(percentageText)
            container.addView(itemLayout)
        }
    }

}