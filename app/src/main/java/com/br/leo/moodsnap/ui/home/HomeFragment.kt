package com.br.leo.moodsnap.ui.home

import android.Manifest
import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.FragmentHomeBinding
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import android.content.Context
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import android.widget.Switch
import android.widget.TimePicker
import com.br.leo.moodsnap.ui.notifications.NotificationHelper
import android.util.Log
import com.br.leo.moodsnap.ui.utils.FontUtils
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.provider.Settings
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.graphics.Paint
import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.ui.adapters.WeekdaysAdapter
import com.br.leo.moodsnap.ui.dialog.OnboardingDialog
import com.br.leo.moodsnap.ui.utils.ButtonUtils
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.FontUtils.updateFontDialogPicker
import com.br.leo.moodsnap.ui.viewmodel.HomeViewModel
import com.br.leo.moodsnap.ui.adapters.FontAdapter
import androidx.recyclerview.widget.RecyclerView

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
            showNotificationSettingsDialog()
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

    private fun isDateInFuture(dayOfMonth: Int): Boolean {
        val today = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply {
            set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), dayOfMonth)
        }
        return selectedDate.after(today)
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
                // Criar data para o dia selecionado
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                val selectedDate = calendar.time

                // Criar novo MoodModel
                val moodType = when (emotionResId) {
                    R.drawable.muito_feliz -> 0
                    R.drawable.feliz -> 1
                    R.drawable.neutro -> 2
                    R.drawable.triste -> 3
                    R.drawable.muito_triste -> 4
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
                        if (isCurrentMonth) {
                            // Se estiver no mês atual, navega para o dashboard
                            findNavController().navigate(R.id.action_home_to_dashboard)
                        } else {
                            // Se estiver em um mês anterior, navega para o próximo mês
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
            0 -> R.drawable.muito_feliz
            1 -> R.drawable.feliz
            2 -> R.drawable.neutro
            3 -> R.drawable.triste
            4 -> R.drawable.muito_triste
            else -> R.drawable.fechado_brilho
        }

        activity?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab)?.let { fab ->
            fab.setImageResource(iconResource)
        }
    }

    private fun setupSettingsMenu() {
        val settingsButton = view?.findViewById<View>(R.id.btn_settings)
        settingsButton?.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Apply current font to settings dialog
            FontUtils.applyFontToView(requireContext(), dialogView)

            showDialogs(dialogView, dialog)

            dialog.show()
        }
    }

    private fun showThemeSelectionDialog() {
        val themeDialogView = layoutInflater.inflate(R.layout.dialog_theme_selection, null)
        val themeDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(themeDialogView)
            .setCancelable(false)
            .create()

        themeDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Apply current font to theme dialog
        FontUtils.applyFontToView(requireContext(), themeDialogView)

        // Get theme buttons
        val btnSystemTheme = themeDialogView.findViewById<Button>(R.id.btn_system_theme)
        val btnLightTheme = themeDialogView.findViewById<Button>(R.id.btn_light_theme)
        val btnDarkTheme = themeDialogView.findViewById<Button>(R.id.btn_dark_theme)

        val buttons = listOf(btnSystemTheme, btnLightTheme, btnDarkTheme)

        // Verificar se é a primeira execução do app
        val isFirstThemeApply = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("is_first_theme_apply", true)

        // Set initial selection based on current theme or first run
        ButtonUtils.resetAllButtons(requireContext(), buttons)
        if (isFirstThemeApply) {
            ButtonUtils.highlightButton(requireContext(), btnSystemTheme)
        } else {
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            when (currentNightMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> ButtonUtils.highlightButton(requireContext(), btnDarkTheme)
                AppCompatDelegate.MODE_NIGHT_NO -> ButtonUtils.highlightButton(requireContext(), btnLightTheme)
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> ButtonUtils.highlightButton(requireContext(), btnSystemTheme)
            }
        }

        // Set up click listeners for theme buttons
        ButtonUtils.setupToggleButtonGroup(requireContext(), buttons)

        themeDialogView.findViewById<View>(R.id.btn_back).setOnClickListener {
            themeDialog.dismiss()
            // Reopen the settings dialog
            val settingsDialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            val settingsDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setView(settingsDialogView)
                .setCancelable(false)
                .create()

            settingsDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Apply current font to settings dialog
            FontUtils.applyFontToView(requireContext(), settingsDialogView)

            showDialogs(settingsDialogView, settingsDialog)

            settingsDialog.show()
        }

        val btnConfirm : Button = themeDialogView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm)
        btnConfirm.setOnClickListener {
            val nightMode = when {
                btnLightTheme.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                btnDarkTheme.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                else -> {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }

            // Save the selected theme to shared preferences
            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putInt("current_theme", nightMode)
                apply()
            }

            if(isFirstThemeApply){
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_first_theme_apply", false)
                    apply()
                }
            }

            // Apply the theme
            AppCompatDelegate.setDefaultNightMode(nightMode)
            themeDialog.dismiss()
        }

        themeDialog.show()
    }

    private fun showLanguageSelectionDialog() {
        val languageDialogView = layoutInflater.inflate(R.layout.dialog_language_selection, null)
        val languageDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(languageDialogView)
            .setCancelable(false)
            .create()

        languageDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Apply current font to language dialog
        FontUtils.applyFontToView(requireContext(), languageDialogView)

        // Get all language buttons
        val btnSystem = languageDialogView.findViewById<Button>(R.id.btn_system_language)
        val btnEnglish = languageDialogView.findViewById<Button>(R.id.btn_english)
        val btnPortuguese = languageDialogView.findViewById<Button>(R.id.btn_portuguese)
        val btnSpanish = languageDialogView.findViewById<Button>(R.id.btn_spanish)
        val btnFrench = languageDialogView.findViewById<Button>(R.id.btn_french)
        val btnItalian = languageDialogView.findViewById<Button>(R.id.btn_italian)
        val btnChinese = languageDialogView.findViewById<Button>(R.id.btn_chinese)
        val btnRussian = languageDialogView.findViewById<Button>(R.id.btn_russian)
        val btnGerman = languageDialogView.findViewById<Button>(R.id.btn_german)
        
        // Load the current language preference
        val currentLanguage = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getString("current_language", "system")
        
        val buttons = listOf(btnSystem, btnEnglish, btnPortuguese, btnSpanish, btnFrench, btnItalian, btnChinese, btnRussian, btnGerman)

        val isFirstLanguageApply = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("is_system_language_apply", true)

        // Create a map of language codes to buttons
        val languageButtonMap = mapOf(
            "system" to btnSystem,
            "en" to btnEnglish,
            "pt" to btnPortuguese,
            "es" to btnSpanish,
            "fr" to btnFrench,
            "it" to btnItalian,
            "zh" to btnChinese,
            "ru" to btnRussian,
            "de" to btnGerman
        )

        // Set initial selection based on current language or first run
        ButtonUtils.resetAllButtons(requireContext(), buttons)
        if (isFirstLanguageApply) {
            ButtonUtils.highlightButton(requireContext(), btnSystem)
        } else {
            // Set initial selection based on current language
            ButtonUtils.highlightButton(requireContext(), languageButtonMap[currentLanguage] ?: btnEnglish)
        }

        // Set up click listeners for all buttons
        ButtonUtils.setupToggleButtonGroup(requireContext(), buttons)

        languageDialogView.findViewById<View>(R.id.btn_back).setOnClickListener {
            languageDialog.dismiss()
            // Reopen the settings dialog
            val settingsDialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            val settingsDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setView(settingsDialogView)
                .setCancelable(false)
                .create()

            settingsDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Apply current font to settings dialog
            FontUtils.applyFontToView(requireContext(), settingsDialogView)

            showDialogs(settingsDialogView, settingsDialog)

            settingsDialog.show()
        }

        val btnConfirm : Button = languageDialogView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm)
        btnConfirm.setOnClickListener {
            val selectedLanguage = when {
                btnSystem.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> Resources.getSystem().configuration.locales.get(0).language.toString()
                btnPortuguese.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "pt"
                btnSpanish.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "es"
                btnFrench.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "fr"
                btnItalian.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "it"
                btnChinese.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "zh"
                btnRussian.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "ru"
                btnGerman.backgroundTintList?.defaultColor == ContextCompat.getColor(requireContext(), R.color.primary_green) -> "de"
                else -> "en"
            }

            with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                putString("current_language", selectedLanguage)
                apply()
            }
            
            // Set the locale to the selected language
            val locale = Locale(selectedLanguage)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            if(btnSystem.backgroundTintList?.defaultColor != ContextCompat.getColor(requireContext(), R.color.primary_green)){
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_system_language_apply", false)
                    apply()
                }
            }else{
                with(requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()) {
                    putBoolean("is_system_language_apply", true)
                    apply()
                }
            }
            
            languageDialog.dismiss()
            
            // Restart the activity to apply the language change
            activity?.recreate()
        }

        languageDialog.show()
    }

    private fun showNotificationSettingsDialog() {
        val notificationDialogView = layoutInflater.inflate(R.layout.dialog_notification_settings, null)
        val notificationDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(notificationDialogView)
            .setCancelable(false)
            .create()

        notificationDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Apply current font to notification dialog
        FontUtils.applyFontToView(requireContext(), notificationDialogView)

        val switchNotifications = notificationDialogView.findViewById<Switch>(R.id.switch_notifications)
        val timePicker = notificationDialogView.findViewById<TimePicker>(R.id.time_picker)
        val notificationHelper = NotificationHelper(requireContext())

        // Load saved notification settings
        val notificationsEnabled = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getBoolean("notifications_enabled", false)
        val notificationHour = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getInt("notification_hour", 20) // Default to 8 PM
        val notificationMinute = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getInt("notification_minute", 0)

        Log.d(TAG, "Loading notification settings - Enabled: $notificationsEnabled, Hour: $notificationHour, Minute: $notificationMinute")

        switchNotifications.isChecked = notificationsEnabled
        timePicker.hour = notificationHour
        timePicker.minute = notificationMinute
        timePicker.setIs24HourView(true)
        timePicker.isEnabled = notificationsEnabled

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Notification switch changed to: $isChecked")
            timePicker.isEnabled = isChecked
            if (!isChecked) {
                notificationHelper.cancelDailyNotification()
            }
        }

        notificationDialogView.findViewById<View>(R.id.btn_back).setOnClickListener {
            notificationDialog.dismiss()
            // Reopen the settings dialog
            val settingsDialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            val settingsDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setView(settingsDialogView)
                .setCancelable(false)
                .create()

            settingsDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Apply current font to settings dialog
            FontUtils.applyFontToView(requireContext(), settingsDialogView)

            showDialogs(settingsDialogView, settingsDialog)

            settingsDialog.show()
        }

        val btnConfirm : Button = notificationDialogView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm)
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

            notificationDialog.dismiss()
        }

        notificationDialog.show()
    }

    private fun showFontSelectionDialog() {
        val fontDialogView = layoutInflater.inflate(R.layout.dialog_font_selection, null)
        val fontDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(fontDialogView)
            .setCancelable(false)
            .create()

        fontDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Apply current font to font dialog
        FontUtils.applyFontToView(requireContext(), fontDialogView)

        // Get preview text views
        val previewText = fontDialogView.findViewById<TextView>(R.id.preview_text)
        val weekText = fontDialogView.findViewById<TextView>(R.id.week_text)
        val dayText = fontDialogView.findViewById<TextView>(R.id.day_text)
        
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
        val recyclerView = fontDialogView.findViewById<RecyclerView>(R.id.fonts_recycler_view)
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
        }
        
        recyclerView.adapter = fontAdapter
        fontAdapter.setSelectedFont(currentFont)

        // Update preview text with current font
        val currentTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont))
        previewText.typeface = currentTypeface
        weekText.typeface = currentTypeface
        dayText.typeface = currentTypeface

        fontDialogView.findViewById<View>(R.id.btn_back).setOnClickListener {
            fontDialog.dismiss()
            // Reopen the settings dialog
            val settingsDialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            val settingsDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setView(settingsDialogView)
                .setCancelable(false)
                .create()

            settingsDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Apply current font to settings dialog
            FontUtils.applyFontToView(requireContext(), settingsDialogView)

            showDialogs(settingsDialogView, settingsDialog)

            settingsDialog.show()
        }

        val btnConfirm : Button = fontDialogView.findViewById<Button>(R.id.btn_confirm)
        Utils.updateBackGroundColor(requireContext(), btnConfirm)
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

            fontDialog.dismiss()
        }

        fontDialog.show()
    }

    private fun showNotificationPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.notifications))
            .setMessage("Para receber lembretes diários, precisamos da sua permissão para enviar notificações.")
            .setPositiveButton("Permitir") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.notifications))
            .setMessage("As notificações estão desativadas. Para receber lembretes diários, você precisa habilitar as notificações nas configurações do sistema.")
            .setPositiveButton("Configurações") { _, _ ->
                openNotificationSettings()
            }
            .setNegativeButton("Cancelar", null)
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
        val onboardingDialog = OnboardingDialog(requireContext())
        onboardingDialog.setCancelable(false)
        onboardingDialog.show()
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
            showLanguageSelectionDialog()
        }

        val btnThemes : Button = settingsDialogView.findViewById<Button>(R.id.btn_themes)
        Utils.setupDialogConfirmButton(requireContext(), btnThemes)
        btnThemes.setOnClickListener {
            settingsDialog.dismiss()
            showThemeSelectionDialog()
        }

        val btnNotifications : Button = settingsDialogView.findViewById<Button>(R.id.btn_notifications)
        Utils.setupDialogConfirmButton(requireContext(), btnNotifications)
        btnNotifications.setOnClickListener {
            settingsDialog.dismiss()
            showNotificationSettingsDialog()
        }

        val btnFonts : Button = settingsDialogView.findViewById<Button>(R.id.btn_fonts)
        Utils.setupDialogConfirmButton(requireContext(), btnFonts)
        btnFonts.setOnClickListener {
            settingsDialog.dismiss()
            showFontSelectionDialog()
        }

        val btnBack : Button = settingsDialogView.findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener {
            settingsDialog.dismiss()
        }
    }
}