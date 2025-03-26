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

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var gestureDetector: GestureDetector

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
        setupGestureDetector()
        setupDayFilterSpinner()
        setupObservers()
        dashboardViewModel.loadMoods()

        // Configurar o detector de gestos na view principal
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
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
    }

    private fun setupDayFilterSpinner() {
        val filters = DashboardViewModel.DayFilter.values()
        val adapter = object : ArrayAdapter<DashboardViewModel.DayFilter>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filters
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val filter = getItem(position)
                (view as TextView).text =
                    filter?.let { dashboardViewModel.getFilterDescription(it) }
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                val filter = getItem(position)
                (view as TextView).text =
                    filter?.let { dashboardViewModel.getFilterDescription(it) }
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dayFilterSpinner.adapter = adapter

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
                    getString(R.string.your_average_mood, dashboardViewModel.getMoodName(moodType))
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
            binding.bestDayText.text = dashboardViewModel.getDayStatisticsText(dayOfWeek, filter)
        }

        // Observar mudanças no filtro selecionado
        dashboardViewModel.selectedDayFilter.observe(viewLifecycleOwner) { filter ->
            val dayOfWeek = dashboardViewModel.bestDayOfWeek.value ?: -1
            binding.bestDayText.text = dashboardViewModel.getDayStatisticsText(dayOfWeek, filter)
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
            dashboardViewModel.getMoodName(mostFrequentMood?.key ?: 2),
            mostFrequentPercentage
        )

        // Configurar o clique no cabeçalho
        binding.distributionHeader.setOnClickListener {
            val isExpanded = container.visibility == View.VISIBLE
            container.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.expandArrow.rotation = if (isExpanded) 0f else 180f
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
                    textSize = 14f
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}