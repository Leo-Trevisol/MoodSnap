package com.br.leo.moodsnap.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _selectedEmotion = MutableLiveData<Int>()
    val selectedEmotion: LiveData<Int> get() = _selectedEmotion

    fun setSelectedEmotion(emotionResId: Int) {
        _selectedEmotion.value = emotionResId
    }
}
