package com.br.leo.moodsnap.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.FragmentDashboardBinding
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.view.isVisible
import com.github.mikephil.charting.components.Legend
import android.content.Context
import com.br.leo.moodsnap.ui.utils.FontUtils
import android.graphics.Typeface
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.ui.utils.RoundedBarChartRenderer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.LegendEntry
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var gestureDetector: GestureDetector
    private var firstTime = true
    private var currentDayFilter = 0 // Novo: para controlar o filtro de dias atual

    // Novo: Enum para os filtros de dias
    private enum class DayFilterType(val days: Int, val description: String) {
        LAST_7_DAYS(7, "Últimos 7 dias"),
        LAST_MONTH(30, "Último mês"),
        LAST_3_MONTHS(90, "Últimos 3 meses"),
        LAST_6_MONTHS(180, "Últimos 6 meses"),
        LAST_9_MONTHS(270, "Últimos 9 meses"),
        LAST_YEAR(365, "Último ano"),
        ALL(-1, "Tudo")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply current font
        activity?.let { activity ->
            val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val currentFont = sharedPreferences.getString("current_font", "default")
            FontUtils.applyFontToActivity(activity)
        }

        setupGestureDetector()
        setupDayFilterSpinner()
        setupBarChartDayFilterSpinner()
        setupObservers()
        dashboardViewModel.loadMoods()

        // Configurar o detector de gestos na view principal
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Set initial visibility to ensure no chart is shown by default
        binding.moodDistributionContainer.visibility = View.GONE
        binding.pieChart.visibility = View.GONE
        binding.barChart.visibility = View.GONE
        binding.periodFilterContainer.visibility = View.GONE
        binding.expandArrow.rotation = 0f

        // Set spinner to default to 'Barras' but keep views hidden
        binding.distributionViewSpinner.setSelection(0)
    }

    private fun setupGestureDetector() {
        gestureDetector =
            GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
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
                        abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                    ) {

                        if (diffX > 0) { // Deslize para a direita
                            findNavController().navigate(R.id.action_dashboard_to_home)
                            return true
                        }
                    }
                    return false
                }
            })

        // Aplique o onTouchListener ao ScrollView ou ao contêiner
        val scrollView = view?.findViewById<ScrollView>(R.id.scroll_cards) // ou o seu contêiner
        scrollView?.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event) // Passe o evento para o GestureDetector
            false // Retorna false para permitir que o ScrollView também processe o evento
        }
    }

    private fun setupDayFilterSpinner() {
        val filters = DashboardViewModel.DayFilter.entries.toTypedArray()
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        
        val adapter = object : ArrayAdapter<DashboardViewModel.DayFilter>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val filter = getItem(position)
                (view as TextView).apply {
                    text = filter?.let { dashboardViewModel.getFilterDescription(requireContext(), it) }
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val filter = getItem(position)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = filter?.let { dashboardViewModel.getFilterDescription(requireContext(), it) }
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.dayFilterSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        binding.dayFilterSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedFilter = filters[position]
                    dashboardViewModel.setDayFilter(selectedFilter)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Setup distribution view spinner
        setupDistributionViewSpinner()
    }

    private fun getAvailableFilters(oldestRecordDate: Date): List<DayFilterType> {
        val today = Calendar.getInstance().time
        val diffInMillis = today.time - oldestRecordDate.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

        return DayFilterType.values().filter { filter ->
            when {
                diffInDays < 7 -> filter == DayFilterType.LAST_7_DAYS
                diffInDays < 30 -> filter in listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH)
                diffInDays < 90 -> filter in listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH, DayFilterType.LAST_3_MONTHS)
                diffInDays < 180 -> filter in listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH, DayFilterType.LAST_3_MONTHS, DayFilterType.LAST_6_MONTHS)
                diffInDays < 270 -> filter in listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH, DayFilterType.LAST_3_MONTHS, DayFilterType.LAST_6_MONTHS, DayFilterType.LAST_9_MONTHS)
                diffInDays < 365 -> filter in listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH, DayFilterType.LAST_3_MONTHS, DayFilterType.LAST_6_MONTHS, DayFilterType.LAST_9_MONTHS, DayFilterType.LAST_YEAR)
                else -> true // Se for mais que 365 dias, mostra todas as opções
            }
        }
    }

    private fun setupBarChartDayFilterSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: return

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)
        
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        
        val adapter = object : ArrayAdapter<DayFilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].description
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].description
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.periodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        binding.periodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    currentDayFilter = position
                    dashboardViewModel.moodDistribution.value?.let { distribution ->
                        val filteredDistribution = filterDistributionByDays(distribution, availableFilters[position].days)
                        setupPieChart(filteredDistribution)
                        setupBarChart(filteredDistribution)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun filterDistributionByDays(distribution: Map<Int, Int>, days: Int): Map<Int, Int> {
        if (days == -1) return distribution // Retorna todos os dados

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val filterDate = calendar.time

        return dashboardViewModel.getMoodDistributionForPeriod(filterDate)
    }

    private fun setupDistributionViewSpinner() {
        val viewTypes = listOf("Barras", "Donut", "Barra grupo")
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewTypes
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.distributionViewSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        binding.distributionViewSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if(!firstTime){
                        when (position) {
                            0 -> { // Barras
                                binding.pieChart.visibility = View.GONE
                                binding.barChart.visibility = View.GONE
                                binding.periodFilterContainer.visibility = View.GONE
                                binding.moodDistributionContainer.visibility = View.VISIBLE
                            }
                            1 -> { // Donut
                                binding.pieChart.visibility = View.VISIBLE
                                binding.barChart.visibility = View.GONE
                                binding.periodFilterContainer.visibility = View.VISIBLE
                                binding.moodDistributionContainer.visibility = View.GONE
                            }
                            2 -> { // Barra grupo
                                binding.pieChart.visibility = View.GONE
                                binding.barChart.visibility = View.VISIBLE
                                binding.periodFilterContainer.visibility = View.VISIBLE
                                binding.moodDistributionContainer.visibility = View.GONE
                            }
                        }
                    }else{
                        firstTime = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun setupObservers() {
        // Observar humor médio
        dashboardViewModel.averageMood.observe(viewLifecycleOwner) { average ->
            if (average == null) {
                binding.averageMoodIcon.setImageResource(R.drawable.neutro)
                binding.averageMoodText.text = getString(R.string.no_mood_registered)
                binding.cardAverageMood.setCardBackgroundColor(Color.WHITE)
            } else {
                val moodType = average.roundToInt()
                binding.averageMoodIcon.setImageResource(Utils.getMoodDrawable(moodType))
                binding.averageMoodText.text =
                    getString(R.string.your_average_mood, dashboardViewModel.getMoodName(requireContext(), moodType))
                binding.cardAverageMood.setCardBackgroundColor(
                    dashboardViewModel.getMoodColor(
                        moodType
                    )
                )
            }

            // Ajustar cor do texto baseado na cor de fundo
            val textColor = Color.BLACK
            binding.averageMoodTitle.setTextColor(textColor)
            binding.averageMoodText.setTextColor(textColor)
        }

        // Observar distribuição de humores
        dashboardViewModel.moodDistribution.observe(viewLifecycleOwner) { distribution ->
            updateMoodDistribution(distribution)
            setupPieChart(distribution)
        }

        // Observar sequência atual
        dashboardViewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            // Atualizar ícones da sequência
            val streakMoods = dashboardViewModel.getCurrentStreakMoods()

            // Limpar container de ícones
            binding.streakIconsContainer.removeAllViews()

            // Adicionar ícones em ordem reversa (do mais recente para o mais antigo)
            streakMoods.reversed().forEach { moodType ->
                // Container circular para o ícone
                val iconContainer = CardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        56, // Tamanho do container
                        56
                    ).apply {
                        marginStart = resources.getDimensionPixelSize(R.dimen.spacing_small)
                    }
                    radius = 24f // Metade do tamanho para fazer um círculo perfeito
                    cardElevation = 0f // Sem sombra
                    setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                }

                // Ícone do humor
                val icon = ImageView(context).apply {
                    setImageResource(Utils.getMoodDrawable(moodType))
                    layoutParams = LinearLayout.LayoutParams(
                        48, // Tamanho do ícone um pouco menor que o container
                        48
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                        // Centralizar o ícone no container
                        marginStart = 4
                        topMargin = 4
                    }
                }

                iconContainer.addView(icon)
                binding.streakIconsContainer.addView(iconContainer)
            }

            // Texto da sequência
            binding.currentStreakText.text = when {
                streak == 0 -> getString(R.string.no_mood_today)
                streak == 1 -> getString(R.string.recorded_today)
                else -> getString(R.string.recorded_days, streak)
            }
        }

        // Observar melhor dia da semana
        dashboardViewModel.bestDayOfWeek.observe(viewLifecycleOwner) { dayOfWeek ->
            val filter =
                dashboardViewModel.selectedDayFilter.value ?: DashboardViewModel.DayFilter.BEST_DAY
            binding.bestDayText.text = dashboardViewModel.getDayStatisticsText(requireContext(), dayOfWeek, filter)
        }

        // Observar mudanças no filtro selecionado
        dashboardViewModel.selectedDayFilter.observe(viewLifecycleOwner) { filter ->
            val dayOfWeek = dashboardViewModel.bestDayOfWeek.value ?: -1
            binding.bestDayText.text = dashboardViewModel.getDayStatisticsText(requireContext(), dayOfWeek, filter)
        }
    }

    private fun updateMoodDistribution(distribution: Map<Int, Int>) {
        val container = binding.moodDistributionContainer
        container.removeAllViews()

        val total = distribution.values.sum().toFloat()
        if (total == 0f) {
            binding.distributionSummary.text = getString(R.string.no_mood_distribution)
            return
        }

        // Encontrar o humor mais frequente
        val mostFrequentMood = distribution.entries.maxByOrNull { it.value }
        val mostFrequentPercentage = ((mostFrequentMood?.value ?: 0) / total * 100).roundToInt()
        // Atualizar o resumo no cabeçalho
        binding.distributionSummary.text = getString(
            R.string.you_were_mood,
            dashboardViewModel.getMoodName(requireContext(), mostFrequentMood?.key ?: 2),
            mostFrequentPercentage
        )

        // Configurar o clique no cabeçalho
        binding.distributionHeader.setOnClickListener {
            firstTime = false
            adjustGraphsVisibility()
        }

        // Criar cards para cada tipo de humor
        for (moodType in 0..4) {
            val count = distribution[moodType] ?: 0
            val percentage = (count / total * 100).roundToInt()

            val itemCard = CardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
                radius = resources.getDimensionPixelSize(R.dimen.spacing_small).toFloat()
                setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                cardElevation = resources.getDimensionPixelSize(R.dimen.spacing_small).toFloat()
            }

            val cardContent = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val paddingValue = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                setPadding(paddingValue, paddingValue, paddingValue, paddingValue)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // Ícone do humor
            val icon = ImageView(context).apply {
                setImageResource(Utils.getMoodDrawable(moodType))
                layoutParams = LinearLayout.LayoutParams(
                    60,
                    60
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                }
            }

            // Texto da porcentagem e estatísticas
            val statsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                }
            }

            // Texto da porcentagem
            val percentageText = TextView(context).apply {
                text = "$percentage%"
                setTextColor(Color.BLACK)
                textSize = 18f
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            }
            statsLayout.addView(percentageText)

            // Última ocorrência
            val lastDate = dashboardViewModel.getLastMoodDate(moodType)
            if (lastDate != null) {
                val lastOccurrenceText = TextView(context).apply {
                    text = getString(R.string.last_record, lastDate)
                    setTextColor(Color.BLACK)
                    textSize = 18f
                    alpha = 0.8f
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
                statsLayout.addView(lastOccurrenceText)
            }

            // Maior sequência
            val longestStreak = dashboardViewModel.getLongestStreak(moodType)
            if (longestStreak > 1) {
                val streakText = TextView(context).apply {
                    text = getString(R.string.longest_streak, longestStreak)
                    setTextColor(Color.BLACK)
                    textSize = 14f
                    alpha = 0.8f
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
                statsLayout.addView(streakText)
            }

            // Total de registros
            val totalRegisters = TextView(context).apply {
                text = getString(R.string.times_recorded, distribution[moodType] ?: 0)
                setTextColor(Color.BLACK)
                textSize = 18f
                textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                }
            }

            cardContent.addView(icon)
            cardContent.addView(statsLayout)
            cardContent.addView(totalRegisters)
            itemCard.addView(cardContent)
            container.addView(itemCard)
        }

        // Atualizar os gráficos com os dados filtrados pelo período atual
        val dayFilters = DayFilterType.values()
        val filteredDistribution = filterDistributionByDays(distribution, dayFilters[currentDayFilter].days)
        setupPieChart(filteredDistribution)
        setupBarChart(filteredDistribution)
    }

    private fun setupPieChart(distribution: Map<Int, Int>) {
        val pieChart: PieChart = binding.pieChart

        // Apply current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Ordem dos humores: muito feliz -> muito triste (mesma ordem do BarChart)
        val moodOrder = listOf(4, 3, 2, 1, 0)
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Criar entradas na ordem correta
        moodOrder.forEach { moodType ->
            val count = distribution[moodType] ?: 0
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), dashboardViewModel.getMoodName(requireContext(), moodType)))
                colors.add(dashboardViewModel.getMoodColor(moodType))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 13f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTypeface = typeface
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.6f
        dataSet.valueLinePart2Length = 0.3f
        dataSet.valueLineColor = Color.WHITE
        dataSet.valueLineWidth = 2f
        dataSet.sliceSpace = 3f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val count = (value * distribution.values.sum() / 100).roundToInt()
                return "${value.roundToInt()}% ($count)"
            }
        })
        pieChart.data = pieData

        // Customize chart appearance
        pieChart.description.isEnabled = false
        //pieChart.setExtraOffsets(50f, 50f, 50f, 50f) // Dar mais espaço para as linhas
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false) // Não mostrar labels dentro das fatias
        
        // Configurar legenda
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.WHITE
        pieChart.legend.textSize = 13f
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        pieChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        pieChart.legend.setDrawInside(false)
        pieChart.legend.xEntrySpace = 7f
        pieChart.legend.yEntrySpace = 5f
        pieChart.legend.yOffset = 10f
        pieChart.legend.typeface = typeface

        // Configurar o buraco do donut
          pieChart.holeRadius = resources.getDimension(R.dimen.hole_pie_chart)
          pieChart.transparentCircleRadius = 50f
          pieChart.setHoleColor(Color.TRANSPARENT)
          pieChart.setTransparentCircleColor(Color.TRANSPARENT)
//        pieChart.setTransparentCircleAlpha(110)

        pieChart.invalidate() // Refresh chart
    }

    private fun setupBarChart(distribution: Map<Int, Int>) {
        val barChart: BarChart = binding.barChart
        
        // Aplicar fonte atual
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Calcular o total de registros para usar como base da porcentagem
        val totalRecords = distribution.values.sum()

        // Criar entradas para o gráfico
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // Ordem dos humores: muito feliz -> muito triste
        val moodOrder = listOf(4, 3, 2, 1, 0)
        moodOrder.forEachIndexed { index, moodType ->
            val count = distribution[moodType] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
            labels.add(dashboardViewModel.getMoodName(requireContext(), moodType))
        }

        // Configurar o dataset
        val dataSet = BarDataSet(entries, "")
        dataSet.colors = moodOrder.map { moodType ->
            dashboardViewModel.getMoodColor(moodType)
        }
        dataSet.valueTextSize = 11f // Reduzido o tamanho do texto
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTypeface = typeface
        dataSet.setDrawValues(true)
        dataSet.barShadowColor = R.color.black

        // Configurar dados do gráfico
        val barData = BarData(dataSet)
        barData.barWidth = 0.7f

        // Configurar formatador de valores personalizado
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Calcular a porcentagem baseada no total de registros
                if (value > 0f) {
                    val percentage = (value / totalRecords * 100).roundToInt()
                    return "${value.toInt()}\n($percentage%)"
                }
                return value.toInt().toString()
            }
        })

        // Definir renderer com cantos arredondados
        barChart.data = barData

        val renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)
        barChart.renderer = renderer

        renderer.initBuffers()

        // Personalizar aparência
        barChart.description.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.isHighlightPerTapEnabled = true
        barChart.setTouchEnabled(true)
        barChart.isClickable = true
        barChart.isHighlightPerTapEnabled = true

        // Configurar eixo X
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.typeface = typeface
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawLabels(false)
        
        // Configurar eixo Y esquerdo
        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.typeface = typeface
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.spaceTop = 35f // Adicionar espaço extra no topo
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        
        // Desabilitar eixo Y direito
        barChart.axisRight.isEnabled = false

        // Configurar legenda
        val legend = barChart.legend
        legend.isEnabled = true
        legend.textSize = resources.getDimension(R.dimen.legend_bar_chart)
        legend.textColor = Color.WHITE
        legend.typeface = typeface
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.yOffset = 2f
        legend.xOffset = 0f
        legend.yEntrySpace = 10f
        legend.xEntrySpace = 15f
        legend.form = Legend.LegendForm.SQUARE
        legend.formSize = 12f
        legend.formToTextSpace = 5f
        legend.maxSizePercent = 0.70f

        // Criar entradas personalizadas para a legenda
        val legendEntries = moodOrder.mapIndexed { index, moodType ->
            LegendEntry().apply {
                label = labels[index]
                formColor = dashboardViewModel.getMoodColor(moodType)
                form = Legend.LegendForm.SQUARE
            }
        }
        legend.setCustom(legendEntries)
        
        // Ajustar margens do gráfico
        barChart.setExtraTopOffset(15f) // Aumentado o offset do topo
        barChart.setExtraBottomOffset(15f)
        barChart.setExtraLeftOffset(10f)
        barChart.setExtraRightOffset(10f)
        barChart.setViewPortOffsets(50f, 15f, 30f, 50f) // Ajustado o offset do topo

        // Animação
        barChart.animateY(1000)
        barChart.highlightValues(null)
        barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun adjustGraphsVisibility() {
        val selectedPosition = binding.distributionViewSpinner.selectedItemPosition
        val isExpanded = when (selectedPosition) {
            0 -> binding.moodDistributionContainer.isVisible
            1 -> binding.pieChart.isVisible
            2 -> binding.barChart.isVisible
            else -> false
        }

        when (selectedPosition) {
            0 -> { // Barras
            binding.moodDistributionContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
                binding.pieChart.visibility = View.GONE
                binding.barChart.visibility = View.GONE
                binding.periodFilterContainer.visibility = View.GONE
            }
            1, 2 -> { // Donut ou Barra grupo
                binding.moodDistributionContainer.visibility = View.GONE
                binding.pieChart.visibility = if (selectedPosition == 1 && !isExpanded) View.VISIBLE else View.GONE
                binding.barChart.visibility = if (selectedPosition == 2 && !isExpanded) View.VISIBLE else View.GONE
                binding.periodFilterContainer.visibility = if (!isExpanded) View.VISIBLE else View.GONE
            }
        }

        binding.expandArrow.rotation = if (isExpanded) 0f else 180f
    }
}