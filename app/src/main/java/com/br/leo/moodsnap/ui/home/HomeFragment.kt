package com.br.leo.moodsnap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
        binding.dateText.setOnClickListener { showDatePicker() }
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

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = calendar.get(Calendar.MONTH)

        // Configurar o picker de anos
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear
        yearPicker.value = calendar.get(Calendar.YEAR)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecione a Data")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("OK") { _, _ ->
                calendar.set(Calendar.YEAR, yearPicker.value)
                calendar.set(Calendar.MONTH, monthPicker.value)
                updateDateTexts()
                updateCalendar()
            }
            .show()

        // Configurar as cores dos botões
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
    }

    private fun updateDateTexts() {
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = calendar.get(Calendar.YEAR).toString()
        binding.dateText.text = "$month $year"
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth())
        binding.calendarGrid.adapter = calendarAdapter
        
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
        calendarAdapter.setDisplayMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        // Primeiro carrega os humores do mês
        homeViewModel.loadMoodsForMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        // Atualiza apenas os dias do mês, mantendo a lista atual de humores
        calendarAdapter.updateData(getDaysInMonth(), homeViewModel.moodsForMonth.value ?: emptyList())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}