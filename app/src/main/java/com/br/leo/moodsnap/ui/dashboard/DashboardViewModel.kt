package com.br.leo.moodsnap.ui.dashboard

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoodRepository(application.applicationContext)

    // Cores para cada tipo de humor
    private val moodColors = mapOf(
        0 to Color.parseColor("#FFE500"), // Muito Feliz - Amarelo
        1 to Color.parseColor("#90EE90"), // Feliz - Verde claro
        2 to Color.parseColor("#E0E0E0"), // Neutro - Cinza
        3 to Color.parseColor("#87CEEB"), // Triste - Azul claro
        4 to Color.parseColor("#4682B4")  // Muito Triste - Azul escuro
    )

    private val _moods = MutableLiveData<List<MoodModel>>()
    val moods: LiveData<List<MoodModel>> = _moods

    private val _averageMood = MutableLiveData<Double?>()
    val averageMood: LiveData<Double?> = _averageMood

    private val _moodDistribution = MutableLiveData<Map<Int, Int>>()
    val moodDistribution: LiveData<Map<Int, Int>> = _moodDistribution

    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak: LiveData<Int> = _currentStreak

    private val _bestDayOfWeek = MutableLiveData<Int>()
    val bestDayOfWeek: LiveData<Int> = _bestDayOfWeek

    enum class DayFilter {
        BEST_DAY,
        WORST_DAY
    }

    private val _selectedDayFilter = MutableLiveData<DayFilter>(DayFilter.BEST_DAY)
    val selectedDayFilter: LiveData<DayFilter> = _selectedDayFilter

    init {
        loadMoods()
    }

    fun getMoodColor(moodType: Int): Int {
        return moodColors[moodType] ?: Color.WHITE
    }

    fun loadMoods() {
        viewModelScope.launch(Dispatchers.IO) {
            val allMoods = repository.getAll()
            _moods.postValue(allMoods)
            calculateStatistics(allMoods)
        }
    }

    private fun calculateStatistics(moods: List<MoodModel>) {
        if (moods.isEmpty()) {
            _averageMood.postValue(null)
            _moodDistribution.postValue(emptyMap())
            _currentStreak.postValue(0)
            _bestDayOfWeek.postValue(-1)
            return
        }

        // Calcular humor médio
        val average = moods.map { it.moodType }.average()
        _averageMood.postValue(average)

        // Calcular distribuição de humores
        val distribution = moods.groupBy { it.moodType }
            .mapValues { it.value.size }
        _moodDistribution.postValue(distribution)

        // Calcular sequência atual
        val streak = calculateCurrentStreak(moods)
        _currentStreak.postValue(streak)

        // Calcular estatísticas do dia da semana com base no filtro selecionado
        val dayResult = calculateDayStatistics(moods, _selectedDayFilter.value ?: DayFilter.BEST_DAY)
        _bestDayOfWeek.postValue(dayResult)
    }

    private fun calculateCurrentStreak(moods: List<MoodModel>): Int {
        val sortedMoods = moods.sortedByDescending { it.date }
        var streak = 0
        var currentDate = Calendar.getInstance()
        currentDate.time = Date() // Hoje

        for (mood in sortedMoods) {
            val moodDate = Calendar.getInstance()
            moodDate.time = mood.date

            // Verificar se a data do humor é o dia esperado na sequência
            if (isSameDay(currentDate, moodDate)) {
                streak++
                currentDate.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateDayStatistics(moods: List<MoodModel>, filter: DayFilter): Int {
        val dayMoods = moods.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        // Calcular pontuação para cada dia considerando média e quantidade
        val dayScores = dayMoods.mapValues { (_, moodsForDay) ->
            val average = moodsForDay.map { it.moodType }.average()
            val count = moodsForDay.size
            // Multiplicar a média pelo número de registros para dar mais peso aos dias com mais registros
            average * count
        }

        return when (filter) {
            DayFilter.BEST_DAY -> dayScores.entries
                .minByOrNull { it.value }?.key ?: -1

            DayFilter.WORST_DAY -> dayScores.entries
                .maxByOrNull { it.value }?.key ?: -1
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun getMoodName(moodType: Int): String {
        return when (moodType) {
            0 -> "Muito Feliz"
            1 -> "Feliz"
            2 -> "Neutro"
            3 -> "Triste"
            4 -> "Muito Triste"
            else -> "Desconhecido"
        }
    }

    fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Segunda-feira"
            Calendar.TUESDAY -> "Terça-feira"
            Calendar.WEDNESDAY -> "Quarta-feira"
            Calendar.THURSDAY -> "Quinta-feira"
            Calendar.FRIDAY -> "Sexta-feira"
            Calendar.SATURDAY -> "Sábado"
            else -> "Desconhecido"
        }
    }

    fun setDayFilter(filter: DayFilter) {
        _selectedDayFilter.value = filter
        // Recalcular as estatísticas com o novo filtro
        viewModelScope.launch(Dispatchers.IO) {
            val allMoods = repository.getAll()
            calculateStatistics(allMoods)
        }
    }

    fun getFilterDescription(filter: DayFilter): String {
        return when (filter) {
            DayFilter.BEST_DAY -> "Melhor"
            DayFilter.WORST_DAY -> "Pior"
        }
    }

    fun getDayStatisticsText(dayOfWeek: Int, filter: DayFilter): String {
        if (dayOfWeek == -1) return "Registre mais humores para ver as estatísticas"
        
        return when (filter) {
            DayFilter.BEST_DAY -> "Seu melhor dia costuma ser ${getDayOfWeekName(dayOfWeek)}"
            DayFilter.WORST_DAY -> "Seu pior dia costuma ser ${getDayOfWeekName(dayOfWeek)}"
        }
    }

    fun getLastMoodDate(moodType: Int): String? {
        val lastMood = moods.value?.filter { it.moodType == moodType }
            ?.maxByOrNull { it.date }
        return if (lastMood != null) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(lastMood.date)
        } else null
    }

    fun getLongestStreak(moodType: Int): Int {
        val moodsList = moods.value ?: return 0
        if (moodsList.isEmpty()) return 0

        var maxStreak = 0
        var currentStreak = 1
        
        // Ordenar por data e filtrar pelo tipo de humor
        val sortedMoods = moodsList.filter { it.moodType == moodType }
            .sortedBy { it.date }
            .map { mood ->
                Calendar.getInstance().apply { 
                    time = mood.date
                    // Zerar hora, minuto, segundo para comparar apenas datas
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }

        if (sortedMoods.isEmpty()) return 0
        
        for (i in 1 until sortedMoods.size) {
            val previousDate = sortedMoods[i - 1]
            val currentDate = sortedMoods[i]
            
            // Calcular diferença em dias
            val diffInMillis = currentDate.timeInMillis - previousDate.timeInMillis
            val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
            
            if (diffInDays == 1L) {
                currentStreak++
            } else {
                maxStreak = maxOf(maxStreak, currentStreak)
                currentStreak = 1
            }
        }

        // Não esquecer de verificar a última sequência
        return maxOf(maxStreak, currentStreak)
    }

    fun getCurrentStreakMoods(): List<Int> {
        val moodsList = moods.value ?: return emptyList()
        if (moodsList.isEmpty()) return emptyList()

        val sortedMoods = moodsList.sortedByDescending { it.date }
        val streakMoods = mutableListOf<Int>()
        var currentDate = Calendar.getInstance()
        currentDate.time = Date() // Hoje

        for (mood in sortedMoods) {
            val moodDate = Calendar.getInstance()
            moodDate.time = mood.date

            if (isSameDay(currentDate, moodDate)) {
                streakMoods.add(mood.moodType)
                currentDate.add(Calendar.DAY_OF_MONTH, -1)
                
                // Limitar a 5 humores
                if (streakMoods.size >= 5) break
            } else {
                break
            }
        }

        return streakMoods
    }
}