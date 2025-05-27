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

    private val _totalMoodsRecorded = MutableLiveData<Int>()
    val totalMoodsRecorded: LiveData<Int> = _totalMoodsRecorded

    private val _bestDayOfWeek = MutableLiveData<Int>()
    val bestDayOfWeek: LiveData<Int> = _bestDayOfWeek

    enum class DayFilter {
        HAPPIEST_DAY,
        SADDEST_DAY,
        MOST_CONSISTENT_DAY,
        MOST_VARIABLE_DAY,
        MOST_ENTRIES_DAY,
        LEAST_ENTRIES_DAY
    }

    private val _selectedDayFilter = MutableLiveData<DayFilter>(DayFilter.HAPPIEST_DAY)
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

    fun getMoodDistributionForPeriod(startDate: Date, endDate: Date): Map<Int, Int> {
        val filteredMoods = _moods.value?.filter { mood ->
            (mood.date.after(startDate) || mood.date == startDate) && 
            (mood.date.before(endDate) || mood.date == endDate)
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
            _totalMoodsRecorded.postValue(0)
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

        // Calcular total de humores registrados
        _totalMoodsRecorded.postValue(moods.size)

        // Calcular estatísticas do dia da semana com base no filtro selecionado
        val dayResult =
            calculateDayStatistics(moods, _selectedDayFilter.value ?: DayFilter.HAPPIEST_DAY)
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

        // Mapear dias para listas de humores para uso nos cálculos de consistência e variabilidade
        val dayMoodTypes = dayMoods.mapValues { (_, moodList) ->
            moodList.map { it.moodType }
        }

        return when (filter) {

            DayFilter.HAPPIEST_DAY -> dayMoodTypes.entries
                .minByOrNull { (_, types) -> types.average() }?.key ?: -1

            DayFilter.SADDEST_DAY -> dayMoodTypes.entries
                .maxByOrNull { (_, types) -> types.average() }?.key ?: -1

            DayFilter.MOST_CONSISTENT_DAY -> dayMoodTypes.entries
                .minByOrNull { (_, types) -> types.distinct().size }?.key ?: -1

            DayFilter.MOST_VARIABLE_DAY -> dayMoodTypes.entries
                .maxByOrNull { (_, types) -> types.distinct().size }?.key ?: -1

            DayFilter.MOST_ENTRIES_DAY -> dayMoods.entries
                .maxByOrNull { (_, moodList) -> moodList.size }?.key ?: -1

            DayFilter.LEAST_ENTRIES_DAY -> dayMoods.entries
                .minByOrNull { (_, moodList) -> moodList.size }?.key ?: -1


        }
    }

    fun getMoodName(context: Context, moodType: Int): String {
        return Utils.getMoodName(context, moodType)
    }

    /**
     * Verifica se há dados de humor disponíveis.
     * @return true se houver pelo menos um registro de humor, false caso contrário
     */
    fun hasMoodData(): Boolean {
        return _moods.value?.isNotEmpty() ?: false
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
            DayFilter.HAPPIEST_DAY -> context.getString(R.string.happiest_day)
            DayFilter.SADDEST_DAY -> context.getString(R.string.saddest_day)
            DayFilter.MOST_CONSISTENT_DAY -> context.getString(R.string.most_consistent_day)
            DayFilter.MOST_VARIABLE_DAY -> context.getString(R.string.most_variable_day)
            DayFilter.MOST_ENTRIES_DAY -> context.getString(R.string.most_entries_day)
            DayFilter.LEAST_ENTRIES_DAY -> context.getString(R.string.least_entries_day)
        }
    }

    fun getDayStatisticsText(context: Context, dayOfWeek: Int, filter: DayFilter): String {
        if (dayOfWeek == -1) return context.getString(R.string.register_more_moods)

        return when (filter) {

            DayFilter.HAPPIEST_DAY -> context.getString(
                R.string.happiest_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.SADDEST_DAY -> context.getString(
                R.string.saddest_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.MOST_CONSISTENT_DAY -> context.getString(
                R.string.most_consistent_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.MOST_VARIABLE_DAY -> context.getString(
                R.string.most_variable_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.MOST_ENTRIES_DAY -> context.getString(
                R.string.most_entries_day_usually,
                DateUtils.getDayOfWeekName(context, dayOfWeek)
            )

            DayFilter.LEAST_ENTRIES_DAY -> context.getString(
                R.string.least_entries_day_usually,
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

    fun getMoodsByWeekdayForPeriod(startDate: Date, endDate: Date): Map<Int, Map<Int, Int>> {
        val weekdayData = mutableMapOf<Int, MutableMap<Int, Int>>()
        
        // Initialize the map for each day of the week (0 = Sunday, 6 = Saturday)
        for (day in 0..6) {
            weekdayData[day] = mutableMapOf()
            // Initialize counts for each mood type (0-4)
            for (moodType in 0..4) {
                weekdayData[day]!![moodType] = 0
            }
        }
        
        // Process existing moods within the period (entre startDate e endDate)
        _moods.value?.filter { mood -> 
            (mood.date.after(startDate) || mood.date == startDate) &&
            (mood.date.before(endDate) || mood.date == endDate)
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

    fun hasMoodsInPeriod(days: Int): Boolean {
        if (days == -1) return (_moods.value?.isNotEmpty() ?: false)
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        
        return _moods.value?.any { mood -> 
            mood.date.after(startDate) || mood.date == startDate
        } ?: false
    }

    fun hasMoodsInPeriod(startDate: Date, endDate: Date): Boolean {
        return _moods.value?.any { mood ->
            (mood.date.after(startDate) || mood.date == startDate) && 
            (mood.date.before(endDate) || mood.date == endDate)
        } ?: false
    }

    fun getMoodTypeByName(context: Context, moodName: String): Int {
        return when (moodName) {
            context.getString(R.string.mood_very_happy) -> 0
            context.getString(R.string.mood_happy) -> 1
            context.getString(R.string.mood_neutral) -> 2
            context.getString(R.string.mood_sad) -> 3
            context.getString(R.string.mood_very_sad) -> 4
            else -> 2 // Neutro como padrão
        }
    }

    fun getMoodsByWeekdayForMoodType(moodType: Int, startDate: Date? = null, endDate: Date? = null): Map<Int, Int> {
        val weekdayData = mutableMapOf<Int, Int>()
        
        // Inicializar o mapa com zeros para todos os dias da semana
        for (i in 0..6) {
            weekdayData[i] = 0
        }

        // Filtrar os registros pelo tipo de humor e período (se especificado)
        val filteredMoods = moods.value
            ?.filter { it.moodType == moodType }
            ?.filter { mood ->
                when {
                    startDate != null && endDate != null -> {
                        // Filtrar por intervalo completo (início e fim)
                        (mood.date.after(startDate) || mood.date == startDate) &&
                        (mood.date.before(endDate) || mood.date == endDate)
                    }
                    startDate != null -> {
                        // Filtrar apenas pela data de início
                        mood.date.after(startDate) || mood.date == startDate
                    }
                    else -> {
                        // Sem filtro de data
                        true
                    }
                }
            }

        // Contar por dia da semana
        filteredMoods?.forEach { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Converter para 0-6
            weekdayData[dayOfWeek] = (weekdayData[dayOfWeek] ?: 0) + 1
        }

        return weekdayData
    }

    fun getLastMoodByType(moodType: Int): MoodModel? {
        return moods.value
            ?.filter { it.moodType == moodType }
            ?.maxByOrNull { it.date }
    }

    /**
     * Obtém a contagem de um tipo específico de humor em dias da semana específicos
     * @param moodType O tipo de humor a ser contabilizado
     * @param weekdays Lista de dias da semana (0 = Domingo, 1 = Segunda, ..., 6 = Sábado)
     * @param startDate Data de início do período (opcional)
     * @param endDate Data de fim do período (opcional)
     * @return Mapa com dia da semana como chave e contagem como valor
     */
    fun getMoodCountByWeekdaysAndType(
        moodType: Int,
        weekdays: List<Int>,
        startDate: Date? = null,
        endDate: Date? = null
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        
        // Inicializar o mapa com zeros para os dias solicitados
        weekdays.forEach { day ->
            result[day] = 0
        }
        
        // Filtrar os registros pelo tipo de humor e período (se especificado)
        var filteredMoods = moods.value?.filter { it.moodType == moodType } ?: emptyList()
        
        // Aplicar filtro de data de início (se especificado)
        if (startDate != null) {
            filteredMoods = filteredMoods.filter { mood ->
                mood.date.after(startDate) || mood.date == startDate
            }
        }
        
        // Aplicar filtro de data de fim (se especificado)
        if (endDate != null) {
            filteredMoods = filteredMoods.filter { mood ->
                mood.date.before(endDate) || mood.date == endDate
            }
        }
        
        // Contar por dia da semana
        filteredMoods.forEach { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Converter para 0-6
            
            // Só contar se o dia estiver na lista de dias solicitados
            if (weekdays.contains(dayOfWeek)) {
                result[dayOfWeek] = (result[dayOfWeek] ?: 0) + 1
            }
        }
        
        return result
    }

    /**
     * Obtém a contagem de diferentes tipos de humor em um dia da semana específico
     * @param moodTypes Lista de tipos de humor a serem contabilizados
     * @param weekday Dia da semana (0 = Domingo, 1 = Segunda, ..., 6 = Sábado)
     * @param startDate Data de início do período (opcional)
     * @param endDate Data de fim do período (opcional)
     * @return Mapa com tipo de humor como chave e contagem como valor
     */
    fun getMoodCountByTypesForWeekday(
        moodTypes: List<Int>,
        weekday: Int,
        startDate: Date? = null,
        endDate: Date? = null
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        
        // Inicializar o mapa com zeros para os tipos de humor solicitados
        moodTypes.forEach { moodType ->
            result[moodType] = 0
        }
        
        // Filtrar os registros pelo dia da semana e período (se especificado)
        var filteredMoods = moods.value ?: emptyList()
        
        // Aplicar filtro de data de início (se especificado)
        if (startDate != null) {
            filteredMoods = filteredMoods.filter { mood ->
                mood.date.after(startDate) || mood.date == startDate
            }
        }
        
        // Aplicar filtro de data de fim (se especificado)
        if (endDate != null) {
            filteredMoods = filteredMoods.filter { mood ->
                mood.date.before(endDate) || mood.date == endDate
            }
        }
        
        // Filtrar por dia da semana e contar por tipo de humor
        filteredMoods.forEach { mood ->
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val moodDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Converter para 0-6
            
            // Só contar se for o dia da semana solicitado e o tipo de humor estiver na lista
            if (moodDayOfWeek == weekday && moodTypes.contains(mood.moodType)) {
                result[mood.moodType] = (result[mood.moodType] ?: 0) + 1
            }
        }
        
        return result
    }

    // Obter todas as datas para um tipo de humor específico no período
    fun getMoodDatesForType(moodType: Int, startDate: Date?, endDate: Date?): List<Date> {
        return _moods.value?.filter { mood ->
            mood.moodType == moodType && 
            (startDate == null || mood.date.after(startDate) || mood.date == startDate) &&
            (endDate == null || mood.date.before(endDate) || mood.date == endDate)
        }?.map { it.date }?.sortedByDescending { it.time } ?: emptyList()
    }

    // Obter todas as datas para um tipo de humor específico no período e dia da semana
    fun getMoodDatesForTypeAndWeekday(moodType: Int, weekday: Int, startDate: Date?, endDate: Date?): List<Date> {
        return _moods.value?.filter { mood ->
            // Verificar se o humor corresponde
            if (mood.moodType != moodType) return@filter false
            
            // Verificar se está dentro do período
            if (startDate != null && mood.date.before(startDate) && mood.date != startDate) return@filter false
            if (endDate != null && mood.date.after(endDate) && mood.date != endDate) return@filter false
            
            // Verificar se o dia da semana corresponde
            val calendar = Calendar.getInstance()
            calendar.time = mood.date
            val moodDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Converter para 0-6
            
            moodDayOfWeek == weekday
        }?.map { it.date }?.sortedByDescending { it.time } ?: emptyList()
    }
}