package com.br.leo.moodsnap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.br.leo.moodsnap.databinding.FragmentHomeBinding
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupCalendarView()
        observeViewModel()
    }

    private fun setupSpinners() {
        // Setup Month Spinner
        val months = (0..11).map { month ->
            calendar.set(Calendar.MONTH, month)
            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        }
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.monthSpinner.adapter = monthAdapter
        binding.monthSpinner.setSelection(calendar.get(Calendar.MONTH))

        // Setup Year Spinner
        val currentYear = calendar.get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).toList()
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.yearSpinner.adapter = yearAdapter
        binding.yearSpinner.setSelection(years.indexOf(currentYear))

        // Listeners
        binding.monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCalendar()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCalendar()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalendarView() {
        calendarAdapter = CalendarAdapter(getDaysInMonth())
        binding.calendarGrid.adapter = calendarAdapter
        
        calendarAdapter.setOnDayClickListener { dayOfMonth ->
            // Aqui você pode adicionar a lógica que deseja executar quando um dia é clicado
            // Por exemplo, abrir o DialogEmotions para o dia selecionado
        }
    }

    private fun updateCalendar() {
        val selectedMonth = binding.monthSpinner.selectedItemPosition
        val selectedYear = binding.yearSpinner.selectedItem.toString().toInt()
        
        calendar.set(Calendar.YEAR, selectedYear)
        calendar.set(Calendar.MONTH, selectedMonth)
        
        homeViewModel.loadMoodsForMonth(selectedYear, selectedMonth)
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