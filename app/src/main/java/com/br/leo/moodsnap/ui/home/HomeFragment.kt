package com.br.leo.moodsnap.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.FragmentHomeBinding
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.bumptech.glide.util.Util
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
            if (selectedDay != -1) {
                if (isDateInFuture(selectedDay)) {
                    Utils.showCustomToast(requireContext(), "Não é possível registrar humor em datas futuras")
                    return@setOnClickListener
                }
                val dialogEmotions = DialogEmotions(requireContext(), mainViewModel)
                dialogEmotions.show()
            }
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
            }
        }
    }

    private fun setupDatePickers() {
        binding.dateContainer.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.month_picker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.year_picker)
        
        // Configurar o picker de meses
        val months = (0..11).map { month ->
            val tempCalendar = Calendar.getInstance()
            tempCalendar.set(Calendar.MONTH, month)
            tempCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
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

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Selecione a Data")
            .setView(dialogView)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("OK") { _, _ ->
                calendar.set(Calendar.YEAR, yearPicker.value)
                calendar.set(Calendar.MONTH, monthPicker.value)
                updateDateTexts()
                updateCalendar()
            }
            .show()
    }

    private fun updateDateTexts() {
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = calendar.get(Calendar.YEAR).toString()
        binding.dateText.text = "$month $year"
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth())
        binding.calendarGrid.adapter = calendarAdapter
        
        // Configurar o mês inicial
        calendarAdapter.setDisplayMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        
        calendarAdapter.setOnDayClickListener { dayOfMonth ->
            if (isDateInFuture(dayOfMonth)) {
                Utils.showCustomToast(requireContext(), "Não é possível selecionar datas futuras")
                return@setOnDayClickListener
            }
            selectedDay = dayOfMonth
            calendarAdapter.setSelectedDay(dayOfMonth)
        }
    }

    private fun updateCalendar() {
        selectedDay = -1
        // Primeiro configura o mês no adaptador
        calendarAdapter.setDisplayMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        // Depois carrega os humores do mês
        homeViewModel.loadMoodsForMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
    }

    private fun getDaysInMonth(): Int {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun observeViewModel() {
        homeViewModel.moodsForMonth.observe(viewLifecycleOwner) { moods ->
            calendarAdapter.updateData(getDaysInMonth(), moods)
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

        binding.monthYearSpinner.adapter = adapter
        binding.monthYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOption = monthYearOptions[position]
                val parts = selectedOption.split(" ")
                val monthName = parts[0]
                val year = parts[1].toInt()
                
                // Encontrar o índice do mês baseado no nome
                val months = (0..11).map { month ->
                    val tempCalendar = Calendar.getInstance()
                    tempCalendar.set(Calendar.MONTH, month)
                    tempCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                }
                val monthIndex = months.indexOf(monthName)
                
                if (monthIndex != -1) {
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, monthIndex)
                    updateCalendar()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}