package com.br.leo.moodsnap.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoodRepository(application)
    private val _moodsForMonth = MutableLiveData<List<MoodModel>>()
    val moodsForMonth: LiveData<List<MoodModel>> = _moodsForMonth

    fun loadMoodsForMonth(year: Int, month: Int) {
        val allMoods = repository.getAll()
        val calendar = Calendar.getInstance()
        
        val monthMoods = allMoods.filter { mood ->
            calendar.time = mood.date
            calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month
        }
        
        _moodsForMonth.value = monthMoods
    }
}