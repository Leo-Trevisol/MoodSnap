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
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
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
                    Toast.makeText(requireContext(), "Não é possível registrar humor em datas futuras", Toast.LENGTH_SHORT).show()
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

                val mood = MoodModel().apply {
                    date = selectedDate
                    this.moodType = moodType
                }

                // Salvar o humor
                homeViewModel.saveMood(mood)
            }
        }
    }

    private fun setupDatePickers() {
        binding.monthText.setOnClickListener { showMonthPicker() }
        binding.yearText.setOnClickListener { showYearPicker() }
    }

    private fun showMonthPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.month_picker)
        
        val months = (0..11).map { month ->
            calendar.set(Calendar.MONTH, month)
            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        }.toTypedArray()

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = calendar.get(Calendar.MONTH)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecione o Mês")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("OK") { dialog, _ ->
                calendar.set(Calendar.MONTH, monthPicker.value)
                updateDateTexts()
                updateCalendar()
            }
            .show()

        // Configurar as cores dos botões
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
    }

    private fun showYearPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_year_picker, null)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.year_picker)
        val currentYear = calendar.get(Calendar.YEAR)
        
        yearPicker.minValue = currentYear - 5
        yearPicker.maxValue = currentYear + 5
        yearPicker.value = currentYear

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecione o Ano")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("OK") { dialog, _ ->
                calendar.set(Calendar.YEAR, yearPicker.value)
                updateDateTexts()
                updateCalendar()
            }
            .show()

        // Configurar as cores dos botões
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.primary_red, null))
    }

    private fun updateDateTexts() {
        binding.monthText.text = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        binding.yearText.text = calendar.get(Calendar.YEAR).toString()
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth())
        binding.calendarGrid.adapter = calendarAdapter
        
        calendarAdapter.setOnDayClickListener { dayOfMonth ->
            if (isDateInFuture(dayOfMonth)) {
                Toast.makeText(requireContext(), "Não é possível selecionar datas futuras", Toast.LENGTH_SHORT).show()
                return@setOnDayClickListener;
            }
            selectedDay = dayOfMonth
            calendarAdapter.setSelectedDay(dayOfMonth)
        }
    }

    private fun updateCalendar() {
        selectedDay = -1
        homeViewModel.loadMoodsForMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        calendarAdapter.setDisplayMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
        calendarAdapter.updateData(getDaysInMonth(), emptyList())
    }

    private fun getDaysInMonth(): Int {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun observeViewModel() {
        homeViewModel.moodsForMonth.observe(viewLifecycleOwner) { moods ->
            calendarAdapter.updateData(getDaysInMonth(), moods)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}