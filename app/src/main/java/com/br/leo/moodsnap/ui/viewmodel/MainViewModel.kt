package com.br.leo.moodsnap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import java.util.*

class MainViewModel : ViewModel() {

    private val _selectedEmotion = MutableLiveData<Int>()
    val selectedEmotion: LiveData<Int> get() = _selectedEmotion

    private val _moodDeleted = MutableLiveData<Boolean>()
    val moodDeleted: LiveData<Boolean> get() = _moodDeleted

    private val _selectedDay = MutableLiveData<Int>()
    val selectedDay: LiveData<Int> = _selectedDay

    private val _selectedMonth = MutableLiveData<Int>()
    val selectedMonth: LiveData<Int> = _selectedMonth

    private val _selectedYear = MutableLiveData<Int>()
    val selectedYear: LiveData<Int> = _selectedYear

    private lateinit var repository: MoodRepository

    fun initialize(context: Context) {
        repository = MoodRepository(context)
    }

    fun setSelectedEmotion(emotionResId: Int) {
        _selectedEmotion.value = emotionResId
    }

    fun deleteMood(moodId: Long) {
        val mood = repository.get(moodId.toInt())
        repository.delete(mood)
        _moodDeleted.value = true
        _moodDeleted.value = false // Resetar para false para permitir novas notificações
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    fun getMoodByDate(date: Date): MoodModel? {
        return repository.getMoodByDate(date)
    }
}
