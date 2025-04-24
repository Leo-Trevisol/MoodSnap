package com.br.leo.moodsnap.ui.dashboard

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.br.leo.moodsnap.ui.utils.Utils
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoodRepository(application.applicationContext)

    // Cores para cada tipo de humor
    private val moodColors = mapOf(
        0 to ContextCompat.getColor(application.applicationContext, R.color.very_happy_color),
        1 to ContextCompat.getColor(application.applicationContext, R.color.happy_color),
        2 to ContextCompat.getColor(application.applicationContext, R.color.neutral_color),
        3 to ContextCompat.getColor(application.applicationContext, R.color.sad_color),
        4 to ContextCompat.getColor(application.applicationContext, R.color.very_sad_color)
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

    fun getMoodDistributionForPeriod(startDate: Date): Map<Int, Int> {
        val filteredMoods = _moods.value?.filter { mood ->
            mood.date.after(startDate) || mood.date == startDate
        } ?: emptyList()

        return filteredMoods.groupBy { it.moodType }
            .mapValues { it.value.size }
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
        val dayResult =
            calculateDayStatistics(moods, _selectedDayFilter.value ?: DayFilter.BEST_DAY)
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
            if (DateUtils.isSameDay(currentDate, moodDate)) {
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

    fun getMoodName(context: Context, moodType: Int): String {
        return Utils.getMoodName(context, moodType)
    }

    fun setDayFilter(filter: DayFilter) {
        _selectedDayFilter.value = filter
        // Recalcular as estatísticas com o novo filtro
        viewModelScope.launch(Dispatchers.IO) {
            val allMoods = repository.getAll()
            calculateStatistics(allMoods)
        }
    }

    fun getFilterDescription(context: Context, filter: DayFilter): String {
        return when (filter) {
            DayFilter.BEST_DAY -> context.getString(R.string.best_day)
            DayFilter.WORST_DAY -> context.getString(R.string.worst_day)
        }
    }

    fun getDayStatisticsText(context: Context, dayOfWeek: Int, filter: DayFilter): String {
        if (dayOfWeek == -1) return context.getString(R.string.register_more_moods)

        return when (filter) {
            DayFilter.BEST_DAY -> context.getString(
                R.string.best_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.WORST_DAY -> context.getString(
                R.string.worst_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )
        }
    }

    fun getLastMoodDate(moodType: Int): String? {
        val lastMood = moods.value?.filter { it.moodType == moodType }
            ?.maxByOrNull { it.date }
        return lastMood?.let { DateUtils.formatDateToString(it.date) }
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

            if (DateUtils.isSameDay(currentDate, moodDate)) {
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

    fun getOldestMoodDate(): Date? {
        return repository.getAll()
            .minByOrNull { it.date.time }
            ?.date
    }

    fun getMoodsByWeekday(): Map<Int, Map<Int, Int>> {
        val weekdayData = mutableMapOf<Int, MutableMap<Int, Int>>()
        
        // Initialize the map for each day of the week (0 = Sunday, 6 = Saturday)
        for (day in 0..6) {
            weekdayData[day] = mutableMapOf()
            // Initialize counts for each mood type (0-4)
            for (moodType in 0..4) {
                weekdayData[day]!![moodType] = 0
            }
        }
        
        // Process existing moods
        _moods.value?.forEach { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-based index
            weekdayData[dayOfWeek]!![mood.moodType] = (weekdayData[dayOfWeek]!![mood.moodType] ?: 0) + 1
        }
        
        return weekdayData
    }

    fun getMoodsByWeekdayForPeriod(startDate: Date): Map<Int, Map<Int, Int>> {
        val weekdayData = mutableMapOf<Int, MutableMap<Int, Int>>()
        
        // Initialize the map for each day of the week (0 = Sunday, 6 = Saturday)
        for (day in 0..6) {
            weekdayData[day] = mutableMapOf()
            // Initialize counts for each mood type (0-4)
            for (moodType in 0..4) {
                weekdayData[day]!![moodType] = 0
            }
        }
        
        // Process existing moods within the period
        _moods.value?.filter { mood -> 
            mood.date.after(startDate) || mood.date == startDate
        }?.forEach { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-based index
            weekdayData[dayOfWeek]!![mood.moodType] = (weekdayData[dayOfWeek]!![mood.moodType] ?: 0) + 1
        }
        
        return weekdayData
    }

    fun getLast5DaysMoods(): List<DayMood> {
        val moodsList = moods.value ?: return emptyList()
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        val result = mutableListOf<DayMood>()
        
        // Criar uma lista dos últimos 5 dias
        for (i in 0..4) {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val dayStart = DateUtils.getStartOfDay(calendar.time)
            
            // Encontrar o humor para este dia
            val mood = moodsList.find { DateUtils.isSameDay(it.date, dayStart) }
            
            result.add(
                DayMood(
                    date = dayStart,
                    moodType = mood?.moodType,
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        
        return result
    }
    
    data class DayMood(
        val date: Date,
        val moodType: Int?,
        val dayOfWeek: Int,
        val dayOfMonth: Int
    )
}