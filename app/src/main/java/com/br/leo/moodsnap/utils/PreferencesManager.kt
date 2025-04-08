package com.br.leo.moodsnap.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
    }

    fun setFirstTimeDone() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }

    companion object {
        private const val PREF_NAME = "MoodSnapPrefs"
        private const val KEY_FIRST_TIME = "isFirstTime"
    }
} 