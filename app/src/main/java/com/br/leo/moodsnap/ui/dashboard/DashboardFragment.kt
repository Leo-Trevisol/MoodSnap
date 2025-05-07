package com.br.leo.moodsnap.ui.dashboard

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.FragmentDashboardBinding
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import android.content.Context
import com.br.leo.moodsnap.ui.utils.FontUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.RoundedBarChartRenderer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis
import java.util.*
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var mainViewModel: MainViewModel
    private var currentDayFilter = 0 // Novo: para controlar o filtro de dias atual

    // Ordem dos humores: do mais feliz para o mais triste
    private val moodOrder = listOf(4, 3, 2, 1, 0)

    // Novo: Interface para todos os tipos de filtro
    private interface FilterType {
        fun getStringResourceId(): Int
        fun getFilterName(context: Context): String
    }

    // Modificado: Enum para os filtros de dias agora implementa FilterType
    private enum class DayFilterType(val days: Int, private val resourceId: Int) : FilterType {
        LAST_7_DAYS(7, R.string.filter_last_7_days),
        LAST_MONTH(30, R.string.filter_last_month),
        LAST_3_MONTHS(90, R.string.filter_last_3_months),
        LAST_6_MONTHS(180, R.string.filter_last_6_months),
        LAST_9_MONTHS(270, R.string.filter_last_9_months),
        LAST_YEAR(365, R.string.filter_last_year),
        ALL(-1, R.string.filter_all);

        override fun getStringResourceId(): Int = resourceId
        override fun getFilterName(context: Context): String = context.getString(resourceId)
    }

    // Novo: Classe para filtros de meses específicos
    private data class MonthFilterType(
        val year: Int,
        val month: Int
    ) : FilterType {
        override fun getStringResourceId(): Int = -1 // Não usa recurso de string

        override fun getFilterName(context: Context): String {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            
            // Formatar nome do mês e ano (ex: "Abril 2025")
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return monthFormat.format(calendar.time).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
        }
        
        // Retorna a data de início do mês (primeiro dia)
        fun getStartDate(): Date {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }
        
        // Retorna a data de fim do mês (último dia)
        fun getEndDate(): Date {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.time
        }
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

        setupDayFilterSpinner()
        setupObservers()

        // Carregar os dados primeiro
        dashboardViewModel.loadMoods()

        // Observar quando os dados forem carregados para configurar os spinners
        dashboardViewModel.moods.observe(viewLifecycleOwner) { moods ->
            if (moods.isNotEmpty()) {
                setupDonutPeriodSpinner()
                setupDonutChart()
                setupBarPeriodSpinner()
                setupCustomBarChart()
                setupRadarPeriodSpinner()
                setupCustomRadarChart()
                setupGroupedBarPeriodSpinner()
                setupGroupedBarChart()
                setupMoodComparisonPeriodSpinner()
                setupMoodComparisonChart()
            }else{
                setupNoMoodRegistered()
            }
        }
    }

    private fun setupNoMoodRegistered() {
        binding.linearDonutChart.visibility = View.GONE
        binding.linearBarChart.visibility = View.GONE
        binding.linearRadarChart.visibility = View.GONE
        binding.linearGroupedBarChart.visibility = View.GONE
        binding.linearMoodComparisonChart.visibility = View.GONE

        binding.donutChartDescription.visibility = View.GONE
        binding.barChartDescription.visibility = View.GONE
        binding.radarChartDescription.visibility = View.GONE
        binding.groupedBarChartDescription.visibility = View.GONE
        binding.moodComparisonChartDescription.visibility = View.GONE

        binding.donutChartNoMoodRegisteredText.visibility = View.VISIBLE
        binding.barChartNoMoodRegisteredText.visibility = View.VISIBLE
        binding.radarChartNoMoodRegisteredText.visibility = View.VISIBLE
        binding.groupedBarChartNoMoodRegisteredText.visibility = View.VISIBLE
        binding.moodComparisonChartNoMoodRegisteredText.visibility = View.VISIBLE
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
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
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
    }

    private fun getAvailableFilters(oldestRecordDate: Date): List<FilterType> {
        val today = Calendar.getInstance().time
        val diffInMillis = today.time - oldestRecordDate.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

        // Obter filtros de dias disponíveis baseado na data mais antiga
        val dayFilters = DayFilterType.values().filter { filter ->
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

        // Obter filtros de meses com registros
        val monthFilters = getMonthsWithRecords(oldestRecordDate)

        // Combinar os dois tipos de filtros, colocando os filtros de dias primeiro
        return dayFilters + monthFilters
    }

    // Novo método para obter os meses que têm registros de humor
    private fun getMonthsWithRecords(oldestRecordDate: Date): List<MonthFilterType> {
        val moods = dashboardViewModel.moods.value ?: return emptyList()
        if (moods.isEmpty()) return emptyList()

        // Agrupar os registros por mês/ano
        val monthsWithRecords = mutableSetOf<Pair<Int, Int>>() // (ano, mês)
        val calendar = Calendar.getInstance()

        // Para cada humor, adicionar seu mês/ano ao conjunto
        for (mood in moods) {
            calendar.time = mood.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            monthsWithRecords.add(Pair(year, month))
        }

        // Converter os pares ano/mês para objetos MonthFilterType
        return monthsWithRecords.map { (year, month) ->
            MonthFilterType(year, month)
        }.sortedWith(compareByDescending<MonthFilterType> { it.year }.thenByDescending { it.month })
    }

    private fun filterDistributionByDays(distribution: Map<Int, Int>, filter: FilterType): Map<Int, Int> {
        // Se não houver distribuição, retornar vazio
        if (distribution.isEmpty()) return emptyMap()

        // Quando o filtro é "Tudo", não aplicamos filtro
        if (filter is DayFilterType && filter.days == -1) return distribution

        // Obter a data de início baseada no tipo de filtro
        val startDate = when (filter) {
            is DayFilterType -> {
                // Para filtros de dias, calcular a data de início baseada no número de dias
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -filter.days)
                }.time
            }
            is MonthFilterType -> {
                // Para filtros de mês específico, usar a data de início do mês
                filter.getStartDate()
            }
            else -> return distribution // Caso não seja um tipo de filtro conhecido
        }

        // Obter a data de fim baseada no tipo de filtro
        val endDate = when (filter) {
            is DayFilterType -> {
                // Para filtros de dias, a data de fim é a data atual
                Calendar.getInstance().time
            }
            is MonthFilterType -> {
                // Para filtros de mês específico, usar a data de fim do mês
                filter.getEndDate()
            }
            else -> return distribution // Caso não seja um tipo de filtro conhecido
        }

        // Filtrar a distribuição pelo período
        return dashboardViewModel.getMoodDistributionForPeriod(startDate, endDate)
    }

    // Método auxiliar para obter mensagem de "Sem dados" baseada no filtro
    private fun getNoDataMessageForPeriod(filter: FilterType?): String {
        return when (filter) {
            is DayFilterType -> {
                when (filter.days) {
                    7 -> getString(R.string.no_data_last_7_days)
                    30 -> getString(R.string.no_data_last_month)
                    90 -> getString(R.string.no_data_last_3_months)
                    180 -> getString(R.string.no_data_last_6_months)
                    270 -> getString(R.string.no_data_last_9_months)
                    365 -> getString(R.string.no_data_last_year)
                    else -> getString(R.string.no_data_all_time)
                }
            }
            is MonthFilterType -> {
                // Para meses específicos, mostrar mensagem personalizada
                getString(R.string.no_data_specific_month, filter.getFilterName(requireContext()))
            }
            else -> getString(R.string.no_data_all_time)
        }
    }

    private fun setupObservers() {
        // Observar humor médio
        dashboardViewModel.averageMood.observe(viewLifecycleOwner) { average ->
            if (average == null) {
                binding.averageMoodIcon.setImageResource(R.drawable.neutral_icon)
                binding.averageMoodText.text = getString(R.string.no_mood_registered)
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
            updateQuickDistribution(distribution)
        }

        // Observar sequência atual
        dashboardViewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            // Obter os últimos 5 dias
            val last5Days = dashboardViewModel.getLast5DaysMoods()

            // Limpar o container de dias
            binding.lastDaysContainer.removeAllViews()

            // Calcular tamanho baseado na largura da tela
            val screenWidth = resources.displayMetrics.widthPixels
            val containerSize = (screenWidth * 0.13).toInt() // 15% da largura da tela
            val iconSize = (containerSize * 1).toInt() // 99% do tamanho do container

            // Adicionar cada dia ao container (em ordem reversa para mostrar do mais antigo para o mais recente)
            last5Days.reversed().forEach { dayMood ->
                val dayContainer = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }

                // Container circular para o ícone do humor
                val iconContainer = CardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        containerSize,
                        containerSize
                    )
                    radius = containerSize / 2f
                    cardElevation = 0f

                    // Definir a cor de fundo baseada no humor (ou transparente se não houver)
                    setCardBackgroundColor(
                        dayMood.moodType?.let { moodType ->
                            dashboardViewModel.getMoodColor(moodType)
                        } ?: ContextCompat.getColor(requireContext(), R.color.primary_background)
                    )
                }

                // Frame para centralizar o ícone
                val frameLayout = FrameLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                }

                // Ícone do humor
                val iconView = ImageView(context).apply {
                    dayMood.moodType?.let { moodType ->
                        setImageResource(Utils.getMoodIcon(moodType))
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        iconSize,
                        iconSize
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                }

                // Adicionar o ícone ao frame layout para centralização
                frameLayout.addView(iconView)

                // Adicionar o frame layout ao container circular
                iconContainer.addView(frameLayout)

                // Criar TextView para o dia
                val dayText = TextView(requireContext()).apply {
                    text = "${dayMood.dayOfMonth}\n${DateUtils.getDayOfWeekShortName(requireContext(), dayMood.dayOfWeek)}"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.secundary))
                }

                // Adicionar views ao container do dia
                dayContainer.addView(iconContainer)
                dayContainer.addView(dayText)

                // Adicionar o container do dia ao container principal
                binding.lastDaysContainer.addView(dayContainer)
            }

            // Atualizar o texto do streak
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

    private fun updateQuickDistribution(distribution: Map<Int, Int>) {
        val container = binding.quickDistributionContainer
        container.removeAllViews()

        val total = distribution.values.sum().toFloat()

        // Calcular tamanho baseado na largura da tela
        val screenWidth = resources.displayMetrics.widthPixels
        val containerSize = (screenWidth * 0.13).toInt() // 18% da largura da tela
        val iconSize = (containerSize * 1).toInt() // 95% do tamanho do container

        moodOrder.forEach { moodType ->
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
                setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
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

            // Texto da porcentagem
            val percentageText = TextView(requireContext()).apply {
                if(total == 0f) {
                    text = "0%"
                } else {
                    val count = distribution[moodType] ?: 0
                    val percentage = (count / total * 100).roundToInt()
                    text = "$percentage%"
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.secundary))
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
            }

            iconContainer.addView(icon)
            itemLayout.addView(iconContainer)
            itemLayout.addView(percentageText)
            container.addView(itemLayout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getFirstPeriodWithMoods(availableFilters: List<FilterType>): Int {
        // Garantir que os dados estejam carregados
        val moods = dashboardViewModel.moods.value ?: return 0
        if (moods.isEmpty()) return 0

        for (i in availableFilters.indices) {
            val filter = availableFilters[i]
            if (filter is DayFilterType) {
                if (dashboardViewModel.hasMoodsInPeriod(filter.days)) {
                    return i
                }
            } else if (filter is MonthFilterType) {
                if (dashboardViewModel.hasMoodsInPeriod(filter.getStartDate(), filter.getEndDate())) {
                    return i
                }
            }
        }
        return 0 // Default to first period if no moods found
    }

    private fun setupDonutPeriodSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: run {
            binding.donutPeriodContainer.visibility = View.GONE
            return
        }

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<FilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.donutPeriodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        // Primeiro configurar o listener
        binding.donutPeriodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Obter a distribuição atual e aplicar o novo filtro
                    dashboardViewModel.moodDistribution.value?.let { distribution ->
                        val filteredDistribution = filterDistributionByDays(distribution, availableFilters[position])
                        updateDonutChart(filteredDistribution)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Depois selecionar o primeiro período com registros
        val firstPeriodWithMoods = getFirstPeriodWithMoods(availableFilters)
        binding.donutPeriodSpinner.setSelection(firstPeriodWithMoods)
    }

    private fun setupDonutChart() {
        val donutChart = binding.donutChart

        // Get current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Configurações básicas
        donutChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            setMinOffset(25f)
            setExtraOffsets(15f, 0f, 15f, 0f)

            legend.isEnabled = false

            // Configurar o buraco do donut
            holeRadius = resources.getDimension(R.dimen.hole_pie_chart)
            transparentCircleRadius = 50f
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)

            // Configurações de interação
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // Configurar texto quando não houver dados
            setNoDataText(getString(R.string.no_mood_distribution))
            setNoDataTextColor(resources.getColor(R.color.secundary))
            setNoDataTextTypeface(typeface)
            getPaint(PieChart.PAINT_INFO).textSize = resources.getDimension(R.dimen.no_data_text) * resources.displayMetrics.density

            // Adicionar listener de clique
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is PieEntry) {
                        val moodName = e.label
                        val moodType = dashboardViewModel.getMoodTypeByName(requireContext(), moodName)

                        // Obter a data inicial baseada no filtro selecionado
                        val selectedPosition = binding.donutPeriodSpinner.selectedItemPosition
                        val selectedFilter = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
                            .getOrNull(selectedPosition) ?: DayFilterType.LAST_7_DAYS

                        // Calcular a data inicial e final do período
                        val startDate: Date?
                        val endDate: Date?
                        
                        when (selectedFilter) {
                            is DayFilterType -> {
                                if (selectedFilter.days > 0) {
                                    // Para filtros de dias, calcular a data de início baseada no número de dias
                                    startDate = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, -selectedFilter.days)
                                    }.time
                                    endDate = Calendar.getInstance().time
                                } else {
                                    // Para "Tudo", não aplicamos filtro de data
                                    startDate = null
                                    endDate = null
                                }
                            }
                            is MonthFilterType -> {
                                // Para filtros de mês específico, usar a data de início e fim do mês
                                startDate = selectedFilter.getStartDate()
                                endDate = selectedFilter.getEndDate()
                            }
                            else -> {
                                startDate = null
                                endDate = null
                            }
                        }

                        // Obter dados por dia da semana para este humor específico com o filtro correto
                        val weekdayData = dashboardViewModel.getMoodsByWeekdayForMoodType(moodType, startDate, endDate)

                        // Criar uma string com a distribuição por dia da semana
                        val weekdays = listOf(
                            getString(R.string.weekday_full_sunday),
                            getString(R.string.weekday_full_monday),
                            getString(R.string.weekday_full_tuesday),
                            getString(R.string.weekday_full_wednesday),
                            getString(R.string.weekday_full_thursday),
                            getString(R.string.weekday_full_friday),
                            getString(R.string.weekday_full_saturday)
                        )

                        val totalCount = weekdayData.values.sum()

                        // Mostrar dialog com detalhes
                        showMoodWeekdayDetailsDialog(
                            moodName,
                            moodType,
                            weekdayData,
                            weekdays,
                            totalCount,
                            dashboardViewModel.getMoodColor(moodType)
                        )
                    }
                }

                override fun onNothingSelected() {
                    // Não é necessário fazer nada aqui
                }
            })
        }

        // Observar mudanças na distribuição de humor
        dashboardViewModel.moodDistribution.observe(viewLifecycleOwner) { distribution ->
            // Garantir que o filtro seja aplicado corretamente
            val availableFilters = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
            val selectedPosition = binding.donutPeriodSpinner.selectedItemPosition
            val selectedFilter = availableFilters.getOrNull(selectedPosition) ?: DayFilterType.LAST_7_DAYS
            val filteredDistribution = filterDistributionByDays(distribution, selectedFilter)
            updateDonutChart(filteredDistribution)
        }
    }

    private fun showMoodWeekdayDetailsDialog(
        moodName: String,
        moodType: Int,
        weekdayData: Map<Int, Int>,
        weekdays: List<String>,
        totalCount: Int,
        moodColor: Int
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_mood_weekday_details)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt() // 85% da largura da tela
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Configurar views do dialog
        val cardView = dialog.findViewById<CardView>(R.id.card_view)
        val moodCardView = dialog.findViewById<CardView>(R.id.mood_card_view)
        val titleText = dialog.findViewById<TextView>(R.id.title_text)
        val moodIcon = dialog.findViewById<ImageView>(R.id.mood_icon)
        val weekdayContainer = dialog.findViewById<LinearLayout>(R.id.weekday_container)

        // Configurar conteúdo
       // cardView.setCardBackgroundColor(moodColor)
        moodCardView.setCardBackgroundColor(moodColor)
        titleText.text = moodName
        titleText.setTextColor(Color.BLACK)
        moodIcon.setImageResource(Utils.getMoodDrawable(moodType))

        // Adicionar informações de cada dia da semana
        weekdays.forEachIndexed { index, weekday ->
            val count = weekdayData[index] ?: 0
            if (count > 0) {
                val weekdayLayout = layoutInflater.inflate(R.layout.item_weekday_count, null)

                weekdayLayout.findViewById<TextView>(R.id.weekday_text).apply {
                    text = weekday
                    setTextColor(resources.getColor(R.color.secundary))
                }
                weekdayLayout.findViewById<TextView>(R.id.count_text).apply {
                    text = getString(R.string.weekday_count_format, count, (count.toFloat() / totalCount * 100).roundToInt())
                    setTextColor(resources.getColor(R.color.secundary))
                }

                weekdayContainer.addView(weekdayLayout)
            }
        }

        dialog.show()
    }

    private fun updateDonutChart(distribution: Map<Int, Int>) {
        // Check if there are any records
        if (distribution.isEmpty() || distribution.values.sum() == 0) {
            binding.donutChart.clear()
            binding.donutChart.notifyDataSetChanged()
            binding.donutChart.invalidate()

            // Obter o período selecionado para a mensagem apropriada
            val selectedPosition = binding.donutPeriodSpinner.selectedItemPosition
            val filter = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date()).getOrNull(selectedPosition)
            binding.donutChart.setNoDataText(getNoDataMessageForPeriod(filter))

            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        moodOrder.forEach { moodType ->
            val count = distribution[moodType] ?: 0
            if (count > 0) {
                val percentage = (count.toFloat() / distribution.values.sum()) * 100
                entries.add(PieEntry(percentage, dashboardViewModel.getMoodName(requireContext(), moodType)))
                colors.add(dashboardViewModel.getMoodColor(moodType))
            }
        }

        // Configurar o dataset
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(true)
            valueTextSize = resources.getDimension(R.dimen.legend_pie_chart)
            valueTextColor = resources.getColor(R.color.secundary)
            valueTypeface = binding.donutChart.legend.typeface
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.3f
            valueLineColor = resources.getColor(R.color.secundary)
            valueLineWidth = 2f
            sliceSpace = 3f
        }

        // Configurar os dados
        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val total = distribution.values.sum().toFloat()
                    val count = (value * total / 100).roundToInt()
                    return "${value.roundToInt()}% ($count)"
                }
            })
        }

        // Aplicar dados ao gráfico
        binding.donutChart.apply {
            data = pieData
            animateY(700)
            invalidate()
        }

        // Atualizar a legenda personalizada
        updateDonutLegend(moodOrder)
    }

    private fun updateDonutLegend(moodOrder: List<Int>) {
        val legendContainer = binding.donutLegendItems
        legendContainer.removeAllViews()

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        moodOrder.forEachIndexed { index, moodType ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) {
                        marginStart = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                    }
                }
            }

            // Quadrado colorido
            val colorBox = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.legend_square_size),
                    resources.getDimensionPixelSize(R.dimen.legend_square_size)
                )
                setBackgroundColor(dashboardViewModel.getMoodColor(moodType))
            }

            // Texto da legenda
            val legendText = TextView(requireContext()).apply {
                text = dashboardViewModel.getMoodName(requireContext(), moodType)
                setTextColor(resources.getColor(R.color.secundary))
                textSize = resources.getDimension(R.dimen.legend_bar_chart)
                this.typeface = typeface
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
            }

            itemLayout.addView(colorBox)
            itemLayout.addView(legendText)
            legendContainer.addView(itemLayout)
        }
    }

    private fun setupBarPeriodSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: run {
            binding.barPeriodContainer.visibility = View.GONE
            return
        }

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<FilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.barPeriodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        // Primeiro configurar o listener
        binding.barPeriodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    dashboardViewModel.moodDistribution.value?.let { distribution ->
                        val filteredDistribution = filterDistributionByDays(distribution, availableFilters[position])
                        updateCustomBarChart(filteredDistribution)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Depois selecionar o primeiro período com registros
        val firstPeriodWithMoods = getFirstPeriodWithMoods(availableFilters)
        binding.barPeriodSpinner.setSelection(firstPeriodWithMoods)
    }

    private fun setupCustomBarChart() {
        val barChart = binding.barChart

        // Get current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val customTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Configurações básicas
        barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // Configurar eixo X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                typeface = customTypeface
                textColor = resources.getColor(R.color.secundary)
                setDrawLabels(false)
            }

            // Configurar eixo Y esquerdo
            axisLeft.apply {
                setDrawGridLines(true)
                typeface = customTypeface
                textColor = resources.getColor(R.color.secundary)
                axisMinimum = 0f
                granularity = 1f
                spaceTop = 35f
            }

            // Desabilitar eixo Y direito
            axisRight.isEnabled = false

            // Configurar texto quando não houver dados
            setNoDataText(getString(R.string.no_mood_distribution))
            setNoDataTextColor(resources.getColor(R.color.secundary))
            setNoDataTextTypeface(customTypeface)
            getPaint(BarChart.PAINT_INFO).textSize = resources.getDimension(R.dimen.no_data_text) * resources.displayMetrics.density

            // Adicionar listener de clique
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null) {
                        val moodType = moodOrder[e.x.toInt()]
                        val lastMood = dashboardViewModel.getLastMoodByType(moodType)

                        lastMood?.let {
                            showLastMoodDetailsDialog(
                                dashboardViewModel.getMoodName(requireContext(), moodType),
                                moodType,
                                it.date,
                                it.description,
                                dashboardViewModel.getMoodColor(moodType)
                            )
                        }
                    }
                }

                override fun onNothingSelected() {
                    // Não é necessário fazer nada aqui
                }
            })
        }

        // Definir renderer com cantos arredondados
        val renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)
        barChart.renderer = renderer

        // Observar mudanças na distribuição de humor
        dashboardViewModel.moodDistribution.observe(viewLifecycleOwner) { distribution ->
            val availableFilters = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
            val selectedPosition = binding.barPeriodSpinner.selectedItemPosition
            val selectedFilter = availableFilters.getOrNull(selectedPosition) ?: DayFilterType.LAST_7_DAYS
            val filteredDistribution = filterDistributionByDays(distribution, selectedFilter)
            updateCustomBarChart(filteredDistribution)
        }
    }

    private fun showLastMoodDetailsDialog(
        moodName: String,
        moodType: Int,
        date: Date,
        note: String?,
        moodColor: Int
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_last_mood_details)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt() // 85% da largura da tela
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Configurar views do dialog
        val cardView = dialog.findViewById<CardView>(R.id.card_view)
        val moodCardView = dialog.findViewById<CardView>(R.id.mood_card_view)
        val titleText = dialog.findViewById<TextView>(R.id.title_text)
        val moodIcon = dialog.findViewById<ImageView>(R.id.mood_icon)
        val dateText = dialog.findViewById<TextView>(R.id.date_text)
        val noteText = dialog.findViewById<TextView>(R.id.note_text)

        // Configurar conteúdo
        moodCardView.setCardBackgroundColor(moodColor)
        titleText.text = moodName
        titleText.setTextColor(Color.BLACK)
        moodIcon.setImageResource(Utils.getMoodDrawable(moodType))
        dateText.text = DateUtils.formatDateTime(requireContext(), date)

        if (!note.isNullOrBlank()) {
            noteText.text = note
            noteText.visibility = View.VISIBLE
        } else {
            noteText.visibility = View.GONE
        }

        dialog.show()
    }

    private fun updateCustomBarChart(distribution: Map<Int, Int>) {
        val barChart = binding.barChart

        // Check if there are any records
        if (distribution.isEmpty() || distribution.values.sum() == 0) {
            barChart.clear()
            barChart.notifyDataSetChanged()
            barChart.invalidate()

            val selectedPosition = binding.barPeriodSpinner.selectedItemPosition
            val filter = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date()).getOrNull(selectedPosition)
            barChart.setNoDataText(getNoDataMessageForPeriod(filter))

            return
        }

        // Calcular o total de registros para usar como base da porcentagem
        val totalRecords = distribution.values.sum()

        // Criar entradas para o gráfico
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        moodOrder.forEachIndexed { index, moodType ->
            val count = distribution[moodType] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
            labels.add(dashboardViewModel.getMoodName(requireContext(), moodType))
        }

        // Configurar o dataset
        val dataSet = BarDataSet(entries, "").apply {
            colors = moodOrder.map { moodType -> dashboardViewModel.getMoodColor(moodType) }
            valueTextSize = 11f
            valueTextColor = resources.getColor(R.color.secundary)
            valueTypeface = barChart.legend.typeface
            setDrawValues(true)
        }

        // Configurar dados do gráfico
        val barData = BarData(dataSet).apply {
            barWidth = 0.7f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value > 0f) {
                        val percentage = (value / totalRecords * 100).roundToInt()
                        return "${value.toInt()}\n($percentage%)"
                    }
                    return ""
                }
            })
        }

        // Aplicar dados ao gráfico
        barChart.apply {
            data = barData
            animateY(700)
            invalidate()
        }

        // Atualizar a legenda personalizada
        updateBarLegend(moodOrder)
    }

    private fun updateBarLegend(moodOrder: List<Int>) {
        val legendContainer = binding.barLegendItems
        legendContainer.removeAllViews()

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        moodOrder.forEachIndexed { index, moodType ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) {
                        marginStart = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                    }
                }
            }

            // Quadrado colorido
            val colorBox = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.legend_square_size),
                    resources.getDimensionPixelSize(R.dimen.legend_square_size)
                )
                setBackgroundColor(dashboardViewModel.getMoodColor(moodType))
            }

            // Texto da legenda
            val legendText = TextView(requireContext()).apply {
                text = dashboardViewModel.getMoodName(requireContext(), moodType)
                setTextColor(resources.getColor(R.color.secundary))
                textSize = resources.getDimension(R.dimen.legend_bar_chart)
                this.typeface = typeface
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
            }

            itemLayout.addView(colorBox)
            itemLayout.addView(legendText)
            legendContainer.addView(itemLayout)
        }
    }

    private fun setupRadarPeriodSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: run {
            binding.radarPeriodContainer.visibility = View.GONE
            return
        }

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<FilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.radarPeriodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        // Primeiro configurar o listener
        binding.radarPeriodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    dashboardViewModel.moodDistribution.value?.let { distribution ->
                        val filteredDistribution = filterDistributionByDays(distribution, availableFilters[position])
                        updateCustomRadarChart(filteredDistribution)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Depois selecionar o primeiro período com registros
        val firstPeriodWithMoods = getFirstPeriodWithMoods(availableFilters)
        binding.radarPeriodSpinner.setSelection(firstPeriodWithMoods)
    }

    private fun setupCustomRadarChart() {
        val radarChart = binding.radarChart

        // Get current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val customTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Configurações básicas
        radarChart.apply {
            description.isEnabled = false
            webLineWidth = 3f
            webColor = Color.LTGRAY
            webLineWidthInner = 3f
            webColorInner = Color.LTGRAY
            webAlpha = 100
            minOffset = 5f
            setTouchEnabled(true)
            isHighlightPerTapEnabled = true
            legend.isEnabled = false

            // Configurar typeface para os eixos
            xAxis.typeface = customTypeface
            yAxis.typeface = customTypeface

            // Configurar texto quando não houver dados
            setNoDataText(getString(R.string.no_mood_distribution))
            setNoDataTextColor(resources.getColor(R.color.secundary))
            setNoDataTextTypeface(customTypeface)
            getPaint(RadarChart.PAINT_INFO).textSize = resources.getDimension(R.dimen.no_data_text) * resources.displayMetrics.density

            // Adicionar listener de clique
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is RadarEntry) {
                        val weekdayIndex = h?.x?.toInt() ?: return
                        val moodType = moodOrder[h.dataSetIndex]
                        val count = e.y.toInt()

                        if (count > 0) {
                            showRadarDetailsDialog(
                                weekdayIndex,
                                moodType,
                                count,
                                dashboardViewModel.getMoodColor(moodType)
                            )
                        }
                    }
                }

                override fun onNothingSelected() {
                    // Não é necessário fazer nada aqui
                }
            })
        }

        // Observar mudanças na distribuição de humor
        dashboardViewModel.moodDistribution.observe(viewLifecycleOwner) { distribution ->
            val availableFilters = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
            val selectedPosition = binding.radarPeriodSpinner.selectedItemPosition
            val selectedFilter = availableFilters.getOrNull(selectedPosition) ?: DayFilterType.LAST_7_DAYS
            val filteredDistribution = filterDistributionByDays(distribution, selectedFilter)
            updateCustomRadarChart(filteredDistribution)
        }
    }

    private fun updateCustomRadarChart(distribution: Map<Int, Int>) {
        val radarChart = binding.radarChart

        // Check if there are any records
        if (distribution.isEmpty() || distribution.values.sum() == 0) {
            radarChart.clear()
            radarChart.notifyDataSetChanged()
            radarChart.invalidate()

            val selectedPosition = binding.radarPeriodSpinner.selectedItemPosition
            val filter = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date()).getOrNull(selectedPosition)
            radarChart.setNoDataText(getNoDataMessageForPeriod(filter))

            return
        }

        // Obter dados por dia da semana com filtro de período
        val selectedFilter = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date()).getOrNull(binding.radarPeriodSpinner.selectedItemPosition)
        
        val weekdayData = when (selectedFilter) {
            is DayFilterType -> {
                if (selectedFilter.days > 0) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -selectedFilter.days)
                    val startDate = calendar.time
                    dashboardViewModel.getMoodsByWeekdayForPeriod(startDate)
                } else {
                    dashboardViewModel.getMoodsByWeekday()
                }
            }
            is MonthFilterType -> {
                val startDate = selectedFilter.getStartDate()
                val endDate = selectedFilter.getEndDate()
                dashboardViewModel.getMoodsByWeekdayForPeriod(startDate, endDate)
            }
            else -> dashboardViewModel.getMoodsByWeekday()
        }

        val weekdays = listOf(
            getString(R.string.weekday_sunday),
            getString(R.string.weekday_monday),
            getString(R.string.weekday_tuesday),
            getString(R.string.weekday_wednesday),
            getString(R.string.weekday_thursday),
            getString(R.string.weekday_friday),
            getString(R.string.weekday_saturday)
        )

        // Criar entradas para cada tipo de humor na ordem padrão (4 a 0)
        val moodOrder = listOf(4, 3, 2, 1, 0)
        val entries = mutableListOf<List<RadarEntry>>()
        val colors = mutableListOf<Int>()
        val labels = mutableListOf<String>()

        // Para cada tipo de humor na ordem padrão
        for (moodType in moodOrder) {
            val moodEntries = weekdays.indices.map { dayIndex ->
                val count = weekdayData[dayIndex]?.get(moodType) ?: 0
                RadarEntry(count.toFloat())
            }
            entries.add(moodEntries)
            colors.add(dashboardViewModel.getMoodColor(moodType))
            labels.add(dashboardViewModel.getMoodName(requireContext(), moodType))
        }

        // Create datasets
        val dataSets = entries.mapIndexed { index, entries ->
            RadarDataSet(entries, labels[index]).apply {
                color = colors[index]
                fillColor = colors[index]
                setDrawFilled(true)
                fillAlpha = 180
                lineWidth = 2f
                isDrawHighlightCircleEnabled = true
                setDrawHighlightIndicators(false)
            }
        }

        // Configure data
        val radarData = RadarData(dataSets).apply {
            setValueTypeface(radarChart.legend.typeface)
            setValueTextSize(14f)
            setDrawValues(true)
            setValueTextColor(resources.getColor(R.color.secundary))
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) value.toInt().toString() else ""
                }
            })
        }

        // Configure X axis (weekdays)
        radarChart.xAxis.apply {
            textSize = 12f
            yOffset = 0f
            xOffset = 0f
            valueFormatter = IndexAxisValueFormatter(weekdays)
            textColor = resources.getColor(R.color.secundary)
        }

        // Configure Y axis
        radarChart.yAxis.apply {
            setLabelCount(5, false)
            textSize = 12f
            axisMinimum = 0f
            setDrawLabels(false)
            textColor = resources.getColor(R.color.secundary)
        }

        // Apply data to chart
        radarChart.apply {
            data = radarData
            animateXY(700, 700)
            invalidate()
        }

        // Atualizar a legenda personalizada
        updateRadarLegend(moodOrder)

    }

    private fun updateRadarLegend(moodOrder: List<Int>) {
        val legendContainer = binding.radarLegendItems
        legendContainer.removeAllViews()

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        moodOrder.forEachIndexed { index, moodType ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) {
                        marginStart = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                    }
                }
            }

            // Quadrado colorido
            val colorBox = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.legend_square_size),
                    resources.getDimensionPixelSize(R.dimen.legend_square_size)
                )
                setBackgroundColor(dashboardViewModel.getMoodColor(moodType))
            }

            // Texto da legenda
            val legendText = TextView(requireContext()).apply {
                text = dashboardViewModel.getMoodName(requireContext(), moodType)
                setTextColor(resources.getColor(R.color.secundary))
                textSize = resources.getDimension(R.dimen.legend_bar_chart)
                this.typeface = typeface
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.spacing_small)
                }
            }

            itemLayout.addView(colorBox)
            itemLayout.addView(legendText)
            legendContainer.addView(itemLayout)
        }
    }

    private fun showRadarDetailsDialog(
        weekdayIndex: Int,
        moodType: Int,
        count: Int,
        moodColor: Int
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_radar_details)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt() // 85% da largura da tela
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Configurar views do dialog
        val cardView = dialog.findViewById<CardView>(R.id.card_view)
        val moodContainer = dialog.findViewById<LinearLayout>(R.id.mood_container)

        // Obter o nome do dia da semana
        val weekdays = listOf(
            getString(R.string.weekday_full_sunday),
            getString(R.string.weekday_full_monday),
            getString(R.string.weekday_full_tuesday),
            getString(R.string.weekday_full_wednesday),
            getString(R.string.weekday_full_thursday),
            getString(R.string.weekday_full_friday),
            getString(R.string.weekday_full_saturday)
        )
        val weekdayName = weekdays[weekdayIndex]

        // Configurar título do dialog
        dialog.findViewById<TextView>(R.id.dialog_title).text = getString(
            R.string.dialog_title_radar_distribution,
            weekdayName
        )

        // Obter todos os humores registrados para este dia da semana
        val days = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date()).getOrNull(binding.radarPeriodSpinner.selectedItemPosition)

        val weekdayData = when (days) {
            is DayFilterType -> {
                if (days.days > 0) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -days.days)
                    val startDate = calendar.time
                    dashboardViewModel.getMoodsByWeekdayForPeriod(startDate)
                } else {
                    dashboardViewModel.getMoodsByWeekday()
                }
            }
            is MonthFilterType -> {
                val startDate = days.getStartDate()
                val endDate = days.getEndDate()
                dashboardViewModel.getMoodsByWeekdayForPeriod(startDate, endDate)
            }
            else -> dashboardViewModel.getMoodsByWeekday()
        }

        val moodsForWeekday = weekdayData[weekdayIndex] ?: return
        val totalMoods = moodsForWeekday.values.sum()

        // Adicionar um item para cada humor registrado neste dia
        moodOrder.forEach { moodType ->
            val count = moodsForWeekday[moodType] ?: 0
            if (count > 0) {
                val moodItem = layoutInflater.inflate(R.layout.item_radar_mood, null)
                val moodCardView = moodItem.findViewById<CardView>(R.id.mood_card_view)
                val moodIcon = moodItem.findViewById<ImageView>(R.id.mood_icon)
                val moodName = moodItem.findViewById<TextView>(R.id.mood_name)
                val moodCount = moodItem.findViewById<TextView>(R.id.mood_count)

                val percentage = (count.toFloat() / totalMoods * 100).roundToInt()
                moodCardView.setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                moodIcon.setImageResource(Utils.getMoodDrawable(moodType))
                moodName.text = dashboardViewModel.getMoodName(requireContext(), moodType)
                moodCount.text = getString(R.string.weekday_count_format, count, percentage)

                moodContainer.addView(moodItem)
            }
        }

        dialog.show()
    }

    // Métodos para o gráfico de barras agrupadas
    private fun setupGroupedBarPeriodSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: run {
            binding.groupedBarPeriodContainer.visibility = View.GONE
            return
        }

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<FilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.groupedBarPeriodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        // Configurar o listener
        binding.groupedBarPeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGroupedBarChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Selecionar o primeiro período com registros
        val firstPeriodWithMoods = getFirstPeriodWithMoods(availableFilters)
        binding.groupedBarPeriodSpinner.setSelection(firstPeriodWithMoods)
    }

    private fun setupGroupedBarChart() {
        // Configurar spinner de dias da semana 1
        setupWeekdaySpinner(binding.groupedBarDay1Spinner, 1) // Segunda-feira como padrão

        // Configurar spinner de dias da semana 2
        setupWeekdaySpinner(binding.groupedBarDay2Spinner, 2) // Terça-feira como padrão

        // Configurar spinner de tipos de humor
        setupMoodTypeSpinner(binding.groupedBarMoodSpinner)

        // Configurar o gráfico
        val barChart = binding.groupedBarChart

        // Get current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val customTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Configurações básicas
        barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // Configurar eixo X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                typeface = customTypeface
                textColor =resources.getColor(R.color.secundary)
                setDrawLabels(true)
            }

            // Configurar eixo Y esquerdo
            axisLeft.apply {
                setDrawGridLines(true)
                typeface = customTypeface
                textColor = resources.getColor(R.color.secundary)
                axisMinimum = 0f
                granularity = 1f
                spaceTop = 35f
            }

            // Desabilitar eixo Y direito
            axisRight.isEnabled = false

            // Configurar texto quando não houver dados
            setNoDataText(getString(R.string.no_mood_distribution))
            setNoDataTextColor(resources.getColor(R.color.secundary))
            setNoDataTextTypeface(customTypeface)
            getPaint(BarChart.PAINT_INFO).textSize = resources.getDimension(R.dimen.no_data_text) * resources.displayMetrics.density

            // Adicionar listener de clique
//            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
//                override fun onValueSelected(e: Entry?, h: Highlight?) {
//                    if (e != null) {
//                        val moodType = moodOrder[e.x.toInt()]
//                        val lastMood = dashboardViewModel.getLastMoodByType(moodType)
//
//                        lastMood?.let {
//                            showLastMoodDetailsDialog(
//                                dashboardViewModel.getMoodName(requireContext(), moodType),
//                                moodType,
//                                it.date,
//                                it.description,
//                                dashboardViewModel.getMoodColor(moodType)
//                            )
//                        }
//                    }
//                }
//
//                override fun onNothingSelected() {
//                    // Não é necessário fazer nada aqui
//                }
//            })
        }

        // Definir renderer com cantos arredondados
        val renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)
        barChart.renderer = renderer

        // Atualizar o gráfico inicialmente
        updateGroupedBarChart()

        // Adicionar listeners para os spinners de dias e humor
        binding.groupedBarDay1Spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGroupedBarChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.groupedBarDay2Spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGroupedBarChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.groupedBarMoodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Atualizar o ícone e o nome do humor selecionado
                val moodType = position
                binding.cardGroupedBarMoodInfoContainer.setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                binding.groupedBarMoodIcon.setImageResource(Utils.getMoodIcon(moodType))
                binding.groupedBarMoodName.text = dashboardViewModel.getMoodName(requireContext(), moodType)
                
                updateGroupedBarChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupWeekdaySpinner(spinner: Spinner, defaultSelection: Int) {
        val weekdays = listOf(
            getString(R.string.weekday_full_sunday),
            getString(R.string.weekday_full_monday),
            getString(R.string.weekday_full_tuesday),
            getString(R.string.weekday_full_wednesday),
            getString(R.string.weekday_full_thursday),
            getString(R.string.weekday_full_friday),
            getString(R.string.weekday_full_saturday)
        )
        
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            weekdays
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            setSelection(defaultSelection)
        }
    }

    private fun setupMoodTypeSpinner(spinner: Spinner) {
        val moodTypes = listOf(
            getString(R.string.mood_very_happy),
            getString(R.string.mood_happy),
            getString(R.string.mood_neutral),
            getString(R.string.mood_sad),
            getString(R.string.mood_very_sad)
        )
        
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            moodTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }
    }

    private fun updateGroupedBarChart() {
        val barChart = binding.groupedBarChart

        // Obter os dias selecionados (converter para índice 0-6)
        val day1Position = binding.groupedBarDay1Spinner.selectedItemPosition
        val day2Position = binding.groupedBarDay2Spinner.selectedItemPosition

        // Obter o tipo de humor selecionado
        val moodPosition = binding.groupedBarMoodSpinner.selectedItemPosition
        val moodType = moodPosition // Os índices correspondem aos tipos de humor (0-4)

        // Obter o período selecionado
        val periodPosition = binding.groupedBarPeriodSpinner.selectedItemPosition
        val availableFilters = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
        val selectedFilter = availableFilters.getOrNull(periodPosition) ?: DayFilterType.LAST_7_DAYS

        // Obter as datas de início e fim baseadas no filtro
        val startDate: Date?
        val endDate: Date?
        when (selectedFilter) {
            is DayFilterType -> {
                if (selectedFilter.days == -1) {
                    // "Tudo" - sem filtro de data
                    startDate = null
                    endDate = null
                } else {
                    // Filtro de dias
                    startDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -selectedFilter.days)
                    }.time
                    endDate = Calendar.getInstance().time
                }
            }
            is MonthFilterType -> {
                // Filtro de mês específico
                startDate = selectedFilter.getStartDate()
                endDate = selectedFilter.getEndDate()
            }
            else -> {
                startDate = null
                endDate = null
            }
        }

        // Obter os dados para os dias selecionados
        val weekdays = listOf(day1Position, day2Position)
        val moodCounts = dashboardViewModel.getMoodCountByWeekdaysAndType(
            moodType = moodType,
            weekdays = weekdays,
            startDate = startDate,
            endDate = endDate
        )

        // Verificar se há dados para exibir
        if (moodCounts.values.sum() == 0) {
            barChart.clear()
            barChart.notifyDataSetChanged()
            barChart.invalidate()
            barChart.setNoDataText(getNoDataMessageForPeriod(selectedFilter))
            return
        }

        // Criar entradas para o gráfico
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        // Adicionar dados para cada dia
        weekdays.forEachIndexed { index, day ->
            val count = moodCounts[day] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))

            val weekdays1 = listOf(
                getString(R.string.weekday_full_sunday),
                getString(R.string.weekday_full_monday),
                getString(R.string.weekday_full_tuesday),
                getString(R.string.weekday_full_wednesday),
                getString(R.string.weekday_full_thursday),
                getString(R.string.weekday_full_friday),
                getString(R.string.weekday_full_saturday)
            )
            
            // Obter o nome do dia da semana
            val weekdayName = weekdays1[day]
            labels.add(weekdayName)
            
            // Usar a cor do humor para as barras
            colors.add(dashboardViewModel.getMoodColor(moodType))
        }

        // Configurar o dataset
        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = resources.getColor(R.color.secundary)
            valueTypeface = barChart.legend.typeface
            setDrawValues(true)
        }

        // Configurar dados do gráfico
        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            })
        }

        // Configurar eixo X com os nomes dos dias
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            yOffset = 2f
        }

        // Aplicar dados ao gráfico
        barChart.apply {
            data = barData
            animateY(700)
            invalidate()
        }
    }

    // Métodos para o gráfico de comparação de humores
    private fun setupMoodComparisonPeriodSpinner() {
        // Obter a data do registro mais antigo do ViewModel
        val oldestRecordDate = dashboardViewModel.getOldestMoodDate() ?: run {
            binding.moodComparisonPeriodContainer.visibility = View.GONE
            return
        }

        // Obter apenas os filtros disponíveis baseado na data mais antiga
        val availableFilters = getAvailableFilters(oldestRecordDate)

        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<FilterType>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableFilters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    gravity = Gravity.START
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    text = availableFilters[position].getFilterName(context)
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                    gravity = Gravity.START
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.moodComparisonPeriodSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        // Configurar o listener
        binding.moodComparisonPeriodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateMoodComparisonChart()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Selecionar o primeiro período com registros
        val firstPeriodWithMoods = getFirstPeriodWithMoods(availableFilters)
        binding.moodComparisonPeriodSpinner.setSelection(firstPeriodWithMoods)
    }

    private fun setupMoodComparisonChart() {
        // Configurar spinner de dia da semana
        setupWeekdaySpinner(binding.moodComparisonDaySpinner, 1) // Segunda-feira como padrão

        // Configurar spinners de tipos de humor
        setupMoodTypeSpinner(binding.moodComparisonMood1Spinner, 0) // Muito feliz como padrão para humor 1
        setupMoodTypeSpinner(binding.moodComparisonMood2Spinner, 4) // Triste como padrão para humor 2

        // Inicializar os ícones e nomes dos humores padrão
        val defaultMood1Type = 0 // Muito Feliz
        val defaultMood2Type = 4 // Muito Triste
        binding.moodComparisonMood1Icon.setImageResource(Utils.getMoodIcon(defaultMood1Type))
        binding.moodComparisonMood1Name.text = dashboardViewModel.getMoodName(requireContext(), defaultMood1Type)
        binding.moodComparisonMood2Icon.setImageResource(Utils.getMoodIcon(defaultMood2Type))
        binding.moodComparisonMood2Name.text = dashboardViewModel.getMoodName(requireContext(), defaultMood2Type)

        // Configurar o gráfico
        val barChart = binding.moodComparisonChart

        // Get current font
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val customTypeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        // Configurações básicas
        barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // Configurar eixo X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                typeface = customTypeface
                textColor = resources.getColor(R.color.secundary)
                setDrawLabels(true)
            }

            // Configurar eixo Y esquerdo
            axisLeft.apply {
                setDrawGridLines(true)
                typeface = customTypeface
                textColor = resources.getColor(R.color.secundary)
                axisMinimum = 0f
                granularity = 1f
                spaceTop = 35f
            }

            // Desabilitar eixo Y direito
            axisRight.isEnabled = false

            // Configurar texto quando não houver dados
            setNoDataText(getString(R.string.no_mood_distribution))
            setNoDataTextColor(resources.getColor(R.color.secundary))
            setNoDataTextTypeface(customTypeface)
            getPaint(BarChart.PAINT_INFO).textSize = resources.getDimension(R.dimen.no_data_text) * resources.displayMetrics.density

            // Adicionar listener de clique
//            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
//                override fun onValueSelected(e: Entry?, h: Highlight?) {
//                    if (e != null) {
//                        val moodType = moodOrder[e.x.toInt()]
//                        val lastMood = dashboardViewModel.getLastMoodByType(moodType)
//
//                        lastMood?.let {
//                            showLastMoodDetailsDialog(
//                                dashboardViewModel.getMoodName(requireContext(), moodType),
//                                moodType,
//                                it.date,
//                                it.description,
//                                dashboardViewModel.getMoodColor(moodType)
//                            )
//                        }
//                    }
//                }
//
//                override fun onNothingSelected() {
//                    // Não é necessário fazer nada aqui
//                }
//            })
        }

        // Definir renderer com cantos arredondados
        val renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)
        barChart.renderer = renderer

        // Atualizar o gráfico inicialmente
        updateMoodComparisonChart()

        // Adicionar listeners para os spinners de dia e humores
        binding.moodComparisonDaySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMoodComparisonChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.moodComparisonMood1Spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Atualizar o ícone e o nome do humor selecionado
                val moodType = position
                binding.cardMoodComparisonMoodsInfoContainer1.setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                binding.moodComparisonMood1Icon.setImageResource(Utils.getMoodIcon(moodType))
                binding.moodComparisonMood1Name.text = dashboardViewModel.getMoodName(requireContext(), moodType)
                
                updateMoodComparisonChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.moodComparisonMood2Spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Atualizar o ícone e o nome do humor selecionado
                val moodType = position
                binding.cardMoodComparisonMoodsInfoContainer2.setCardBackgroundColor(dashboardViewModel.getMoodColor(moodType))
                binding.moodComparisonMood2Icon.setImageResource(Utils.getMoodIcon(moodType))
                binding.moodComparisonMood2Name.text = dashboardViewModel.getMoodName(requireContext(), moodType)
                
                updateMoodComparisonChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupMoodTypeSpinner(spinner: Spinner, defaultSelection: Int) {
        val moodTypes = listOf(
            getString(R.string.mood_very_happy),
            getString(R.string.mood_happy),
            getString(R.string.mood_neutral),
            getString(R.string.mood_sad),
            getString(R.string.mood_very_sad)
        )
        
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentFont = sharedPreferences.getString("current_font", "default")
        val typeface = ResourcesCompat.getFont(requireContext(), FontUtils.getFontResourceId(currentFont ?: "default"))

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            moodTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    this.typeface = typeface
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))
                (view as TextView).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.secundary))
                    this.typeface = typeface
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.apply {
            this.adapter = adapter
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            setSelection(defaultSelection)
        }
    }


    private fun updateMoodComparisonChart() {
        val barChart = binding.moodComparisonChart

        // Obter o dia da semana selecionado
        val dayPosition = binding.moodComparisonDaySpinner.selectedItemPosition

        // Obter os tipos de humor selecionados
        val mood1Position = binding.moodComparisonMood1Spinner.selectedItemPosition
        val mood2Position = binding.moodComparisonMood2Spinner.selectedItemPosition
        val moodTypes = listOf(mood1Position, mood2Position)

        // Obter o período selecionado
        val periodPosition = binding.moodComparisonPeriodSpinner.selectedItemPosition
        val availableFilters = getAvailableFilters(dashboardViewModel.getOldestMoodDate() ?: Date())
        val selectedFilter = availableFilters.getOrNull(periodPosition) ?: DayFilterType.LAST_7_DAYS

        // Obter as datas de início e fim baseadas no filtro
        val startDate: Date?
        val endDate: Date?
        when (selectedFilter) {
            is DayFilterType -> {
                if (selectedFilter.days == -1) {
                    // "Tudo" - sem filtro de data
                    startDate = null
                    endDate = null
                } else {
                    // Filtro de dias
                    startDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -selectedFilter.days)
                    }.time
                    endDate = Calendar.getInstance().time
                }
            }
            is MonthFilterType -> {
                // Filtro de mês específico
                startDate = selectedFilter.getStartDate()
                endDate = selectedFilter.getEndDate()
            }
            else -> {
                startDate = null
                endDate = null
            }
        }

        // Obter os dados para os humores selecionados no dia da semana específico
        val moodCounts = dashboardViewModel.getMoodCountByTypesForWeekday(
            moodTypes = moodTypes,
            weekday = dayPosition,
            startDate = startDate,
            endDate = endDate
        )

        // Verificar se há dados para exibir
        if (moodCounts.values.sum() == 0) {
            barChart.clear()
            barChart.notifyDataSetChanged()
            barChart.invalidate()
            barChart.setNoDataText(getNoDataMessageForPeriod(selectedFilter))
            return
        }

        // Criar entradas para o gráfico
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        // Adicionar dados para cada tipo de humor
        moodTypes.forEachIndexed { index, moodType ->
            val count = moodCounts[moodType] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
            
            // Obter o nome do humor
            val moodName = dashboardViewModel.getMoodName(requireContext(), moodType)
            labels.add(moodName)
            
            // Usar a cor do humor para as barras
            colors.add(dashboardViewModel.getMoodColor(moodType))
        }

        // Configurar o dataset
        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = resources.getColor(R.color.secundary)
            valueTypeface = barChart.legend.typeface
            setDrawValues(true)
        }

        // Configurar dados do gráfico
        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            })
        }

        // Configurar eixo X com os nomes dos humores
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            yOffset = 2f
        }

        // Aplicar dados ao gráfico
        barChart.apply {
            data = barData
            animateY(700)
            invalidate()
        }
    }
}