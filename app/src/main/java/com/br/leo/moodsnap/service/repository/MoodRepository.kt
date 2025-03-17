package com.br.leo.moodsnap.service.repository

import android.content.Context
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.database.MoodDatabase
import java.util.*

class MoodRepository (context: Context) {

    private val dataBase = MoodDatabase.getDatabase(context).moodDao()

    fun get(id: Int): MoodModel {
        return dataBase.load(id)
    }

    fun save(mood: MoodModel): Boolean {
        return dataBase.save(mood) > 0
    }

    fun getAll(): List<MoodModel> {
        return dataBase.getAll()
    }


    fun getMoodType(moodType : Int): List<MoodModel> {
        return dataBase.getMoodType(moodType)
    }

    fun update(mood: MoodModel): Boolean {
        return dataBase.update(mood) > 0
    }

    fun delete(mood: MoodModel) {
        dataBase.delete(mood)
    }

    fun getMoodByDate(date: Date): MoodModel? {
        val calendar = Calendar.getInstance().apply { time = date }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        return dataBase.getMoodByDateRange(startOfDay, endOfDay)
    }

}