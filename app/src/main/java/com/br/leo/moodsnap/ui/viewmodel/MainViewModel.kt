package com.br.leo.moodsnap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.service.repository.MoodRepository
import java.util.Calendar

class MainViewModel : ViewModel() {

    private val _selectedEmotion = MutableLiveData<Int>()
    val selectedEmotion: LiveData<Int> get() = _selectedEmotion

    private val _moodDeleted = MutableLiveData<Boolean>()
    val moodDeleted: LiveData<Boolean> get() = _moodDeleted

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
}
