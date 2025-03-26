package com.br.leo.moodsnap.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        mainViewModel.initialize(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGestureDetector()
        setupDatePickers()
        setupWeekdaysGrid()
        setupCalendarView()
        observeViewModel()
        updateDateTexts()
        setupFabListener()
        observeMainViewModel()
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

        dialog.show()

    }

    private fun updateDateTexts() {
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
        val year = calendar.get(Calendar.YEAR).toString()
        binding.dateText.text = "$month $year"
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth())
        binding.calendarGrid.adapter = calendarAdapter

        // Configurar o mês inicial
        updateCalendarForDate(calendar)

        calendarAdapter.setOnDayClickListener { dayOfMonth ->
            if (isDateInFuture(dayOfMonth)) {
                Utils.showCustomToast(requireContext(), requireContext().getString(R.string.future_date_not_allowed))
                return@setOnDayClickListener
            }
            selectedDay = dayOfMonth
            calendarAdapter.setSelectedDay(dayOfMonth)

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

                    if (diffX < 0) { // Deslize para a esquerda
                        findNavController().navigate(R.id.action_home_to_dashboard)
                        return true
                    }
                }
                return false
            }
        })
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}